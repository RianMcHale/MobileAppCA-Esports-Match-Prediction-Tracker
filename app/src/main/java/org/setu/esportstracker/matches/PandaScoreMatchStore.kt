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

// downloads cs2 matches and keeps the latest loaded list in memory.
object PandaScoreMatchStore : MatchStore {
    private const val URL_BASE = "https://api.pandascore.co/csgo/matches"

    // makes the newest list visible to other threads after it is replaced.
    @Volatile
    private var cachedMatches: List<EsportsMatch> = emptyList()

    // returns the latest list fetched from pandascore.
    override fun findAll(): List<EsportsMatch> = cachedMatches

    // looks for one match in the latest fetched list.
    override fun findOne(id: Long): EsportsMatch? =
        cachedMatches.find { it.id == id }

    // downloads the selected type of match on a background thread.
    fun refresh(category: MatchCategory): List<EsportsMatch> {
        // stops before making a request if the app has no api token.
        check(BuildConfig.PANDASCORE_TOKEN.isNotBlank()) {
            "PandaScore token is missing"
        }

        // completed uses one page of 50; other categories use pages of 100.
        val pageSize = if (category == MatchCategory.COMPLETED) 50 else 100
        val results = mutableListOf<EsportsMatch>()
        var page = 1

        while (true) {
            // loads one page for the selected endpoint.
            val response = requestPage(category, page, pageSize)

            // converts usable json entries into app match objects.
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                mapMatch(item, category)?.let(results::add)
            }

            // avoids downloading every completed match from the whole history.
            if (category == MatchCategory.COMPLETED ||
                response.length() < pageSize
            ) {
                break
            }

            // a full page might mean more results exist.
            page++
        }

        // completed shows newest first; upcoming shows nearest first.
        val sorted = if (category == MatchCategory.COMPLETED) {
            results.sortedByDescending { it.startTimeUtcMillis }
        } else {
            results.sortedBy { it.startTimeUtcMillis }
        }

        // stores this list for findall and findone.
        cachedMatches = sorted
        return sorted
    }

    // requests one page and returns its json array.
    private fun requestPage(
        category: MatchCategory,
        page: Int,
        pageSize: Int
    ): JSONArray {
        // asks pandascore to put recent completed matches first.
        val sort = if (category == MatchCategory.COMPLETED) {
            "&sort=-begin_at"
        } else {
            ""
        }

        // adds the category, page number, page size, and optional sorting.
        val url = URL(
            "$URL_BASE/${category.apiPath}" +
                    "?per_page=$pageSize&page=$page$sort"
        )
        val connection = url.openConnection() as HttpURLConnection

        try {
            // sets the request type and prevents it hanging forever.
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")

            // sends the token in the authorisation header.
            connection.setRequestProperty(
                "Authorization",
                "Bearer ${BuildConfig.PANDASCORE_TOKEN}"
            )

            // anything other than http 200 is treated as a failed load.
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException(
                    "PandaScore returned HTTP ${connection.responseCode}"
                )
            }

            // reads the full response before turning it into a json array.
            val body = connection.inputStream.bufferedReader().use {
                it.readText()
            }
            return JSONArray(body)
        } finally {
            // closes the connection even if reading or parsing fails.
            connection.disconnect()
        }
    }

    // turns one pandascore json match into the simpler app model.
    private fun mapMatch(
        item: JSONObject,
        category: MatchCategory
    ): EsportsMatch? {
        // past results can include canceled matches, so only keep finished ones.
        if (category == MatchCategory.COMPLETED &&
            item.optString("status") != "finished"
        ) {
            return null
        }

        // skips data without a usable match id or opponents list.
        val matchId = item.optLong("id", -1L)
        val opponents = item.optJSONArray("opponents") ?: return null
        if (matchId <= 0L || opponents.length() != 2) return null

        // each opponents entry has another object containing the team.
        val firstTeam = opponents.optJSONObject(0)
            ?.optJSONObject("opponent") ?: return null
        val secondTeam = opponents.optJSONObject(1)
            ?.optJSONObject("opponent") ?: return null

        // reads both teams' ids and display names.
        val firstId = firstTeam.optLong("id", -1L)
        val secondId = secondTeam.optLong("id", -1L)
        val firstName = firstTeam.optString("name")
        val secondName = secondTeam.optString("name")

        // cards and predictions need two identified teams.
        if (firstId <= 0L || secondId <= 0L ||
            firstName.isBlank() || secondName.isBlank()
        ) {
            return null
        }

        // scheduled time is preferred; begin time is the fallback.
        val scheduledAt = item.optString("scheduled_at")
            .takeUnless { it.isBlank() || it == "null" }
            ?: item.optString("begin_at")

        // matches without a readable time cannot be sorted or shown properly.
        val startMillis = parseTime(scheduledAt) ?: return null

        // uses a fallback if the tournament name is missing.
        val competition = item.optJSONObject("tournament")
            ?.optString("name")
            .orEmpty()
            .ifBlank { "Unknown tournament" }

        // pandascore may give no winner for a match.
        val apiWinnerId = item.optLong("winner_id", -1L)

        // only accepts a team winner that matches one of this match's teams.
        val winnerId = apiWinnerId.takeIf {
            category == MatchCategory.COMPLETED &&
                    item.optString("winner_type") == "Team" &&
                    (it == firstId || it == secondId)
        }

        // stores only the fields the app needs to display and predict matches.
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

    // changes an api time string into utc milliseconds.
    private fun parseTime(value: String): Long? {
        // removes decimal seconds so the formatter can read the rest.
        val normalized = value.replace(
            Regex("\\.\\d+(?=Z|[+-]\\d{2}:\\d{2}$)"),
            ""
        )

        // expects a full date, time, and timezone.
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.US
        ).apply {
            isLenient = false
        }

        // tracks how much of the string the formatter actually read.
        val position = ParsePosition(0)
        val date = formatter.parse(normalized, position)

        // rejects a partial date instead of silently accepting it.
        return date?.takeIf { position.index == normalized.length }?.time
    }
}