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

object PandaScoreMatchStore : MatchStore { // gets upcoming CS2 matches from PandaScore
    private const val PAGE_SIZE = 100
    private const val URL_BASE = "https://api.pandascore.co/csgo/matches/upcoming"

    // stores the last matches loaded so they can be found by id
    @Volatile
    private var cachedMatches: List<EsportsMatch> = emptyList()

    override fun findAll(): List<EsportsMatch> = cachedMatches

    override fun findOne(id: Long): EsportsMatch? =
        cachedMatches.find { it.id == id }

    // asks PandaScore for each page of upcoming matches
    fun refreshUpcoming(): List<EsportsMatch> {
        check(BuildConfig.PANDASCORE_TOKEN.isNotBlank()) {
            "PandaScore token is missing"
        }

        val results = mutableListOf<EsportsMatch>()
        var page = 1

        while (true) {
            val response = requestPage(page)

            // adds matches that have enough information for the app
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                mapMatch(item)?.let(results::add)
            }

            // stops when there are no more full pages
            if (response.length() < PAGE_SIZE) break
            page++
        }

        // sorts matches by start time and saves the list
        return results.sortedBy { it.startTimeUtcMillis }.also {
            cachedMatches = it
        }
    }

    private fun requestPage(page: Int): JSONArray {
        val url = URL("$URL_BASE?per_page=$PAGE_SIZE&page=$page")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")

            // sends the token with the request
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
            // closes the connection even if the request fails
            connection.disconnect()
        }
    }

    private fun mapMatch(item: JSONObject): EsportsMatch? {
        val matchId = item.optLong("id", -1L)
        val opponents = item.optJSONArray("opponents") ?: return null

        // skips matches without two known teams
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
        ) return null

        // uses the scheduled time, or the begin time if needed
        val scheduledAt = item.optString("scheduled_at")
            .takeUnless { it.isBlank() || it == "null" }
            ?: item.optString("begin_at")
        val startMillis = parseTime(scheduledAt) ?: return null

        val competition = item.optJSONObject("tournament")
            ?.optString("name")
            .orEmpty()
            .ifBlank { "Unknown tournament" }

        return EsportsMatch(
            id = matchId,
            teamOneId = firstId,
            teamOneName = firstName,
            teamTwoId = secondId,
            teamTwoName = secondName,
            game = "CS2",
            competition = competition,
            startTimeUtcMillis = startMillis
        )
    }

    private fun parseTime(value: String): Long? {
        // removes decimal seconds before reading the date
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

        // only accepts the date if the whole value was read
        return date?.takeIf { position.index == normalized.length }?.time
    }
}