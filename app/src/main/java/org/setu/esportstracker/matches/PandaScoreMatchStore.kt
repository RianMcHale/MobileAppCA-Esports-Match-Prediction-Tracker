package org.setu.esportstracker.matches

import org.json.JSONArray
import org.json.JSONObject
import org.setu.esportstracker.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

object PandaScoreMatchStore : MatchStore {
    private const val URL_BASE = "https://api.pandascore.co/csgo/matches"

    @Volatile
    private var cachedMatches: List<EsportsMatch> = emptyList()

    override fun findAll(): List<EsportsMatch> = cachedMatches

    override fun findOne(id: Long): EsportsMatch? =
        cachedMatches.find { it.id == id }

    // Network work: call from a background thread.
    fun refresh(category: MatchCategory): List<EsportsMatch> {
        check(BuildConfig.PANDASCORE_TOKEN.isNotBlank()) {
            "PandaScore token is missing"
        }

        val pageSize = if (category == MatchCategory.COMPLETED) 50 else 100
        val results = mutableListOf<EsportsMatch>()
        var page = 1

        while (true) {
            val response = requestPage(category, page, pageSize)

            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                mapMatch(item, category)?.let(results::add)
            }

            // Show only the newest page of completed matches.
            // Otherwise, continue until all pages have been loaded.
            if (category == MatchCategory.COMPLETED ||
                response.length() < pageSize
            ) {
                break
            }

            page++
        }

        val sorted = if (category == MatchCategory.COMPLETED) {
            results.sortedByDescending { it.startTimeUtcMillis }
        } else {
            results.sortedBy { it.startTimeUtcMillis }
        }

        cachedMatches = sorted
        return sorted
    }

    private fun requestPage(
        category: MatchCategory,
        page: Int,
        pageSize: Int
    ): JSONArray {
        val sort = if (category == MatchCategory.COMPLETED) {
            "&sort=-begin_at"
        } else {
            ""
        }

        val url = URL(
            "$URL_BASE/${category.apiPath}" +
                    "?per_page=$pageSize&page=$page$sort"
        )
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "Authorization",
                "Bearer ${BuildConfig.PANDASCORE_TOKEN}"
            )

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException(
                    "PandaScore returned HTTP ${connection.responseCode}"
                )
            }

            val body = connection.inputStream.bufferedReader().use {
                it.readText()
            }
            return JSONArray(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun mapMatch(
        item: JSONObject,
        category: MatchCategory
    ): EsportsMatch? {
        // Do not show canceled matches in the Completed tab.
        if (category == MatchCategory.COMPLETED &&
            item.optString("status") != "finished"
        ) {
            return null
        }

        val matchId = item.optLong("id", -1L)
        val opponents = item.optJSONArray("opponents") ?: return null

        if (matchId <= 0L || opponents.length() != 2) return null

        val firstTeam = opponents.optJSONObject(0)
            ?.optJSONObject("opponent") ?: return null
        val secondTeam = opponents.optJSONObject(1)
            ?.optJSONObject("opponent") ?: return null

        val firstId = firstTeam.optLong("id", -1L)
        val secondId = secondTeam.optLong("id", -1L)
        val firstName = firstTeam.optString("name")
        val secondName = secondTeam.optString("name")

        if (firstId <= 0L || secondId <= 0L ||
            firstName.isBlank() || secondName.isBlank()
        ) {
            return null
        }

        val scheduledAt = item.optString("scheduled_at")
            .takeUnless { it.isBlank() || it == "null" }
            ?: item.optString("begin_at")
        val startMillis = parseTime(scheduledAt) ?: return null

        val competition = item.optJSONObject("tournament")
            ?.optString("name")
            .orEmpty()
            .ifBlank { "Unknown tournament" }

        val apiWinnerId = item.optLong("winner_id", -1L)
        val winnerId = apiWinnerId.takeIf {
            category == MatchCategory.COMPLETED &&
                    item.optString("winner_type") == "Team" &&
                    (it == firstId || it == secondId)
        }

        return EsportsMatch(
            id = matchId,
            teamOneId = firstId,
            teamOneName = firstName,
            teamTwoId = secondId,
            teamTwoName = secondName,
            game = "CS2",
            competition = competition,
            startTimeUtcMillis = startMillis,
            winnerTeamId = winnerId
        )
    }

    private fun parseTime(value: String): Long? {
        val normalized = value.replace(
            Regex("\\.\\d+(?=Z|[+-]\\d{2}:\\d{2}$)"),
            ""
        )

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.US
        ).apply {
            isLenient = false
        }

        val position = ParsePosition(0)
        val date = formatter.parse(normalized, position)
        return date?.takeIf { position.index == normalized.length }?.time
    }
}