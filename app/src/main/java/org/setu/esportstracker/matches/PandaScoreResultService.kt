package org.setu.esportstracker.matches

import org.json.JSONArray
import org.setu.esportstracker.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object PandaScoreResultService {

    // Returns match ID -> winning team ID for finished matches.
    // Call this from a background thread.
    fun fetchFinishedWinners(matchIds: List<Long>): Map<Long, Long> {
        check(BuildConfig.PANDASCORE_TOKEN.isNotBlank()) {
            "PandaScore token is missing"
        }

        val winners = mutableMapOf<Long, Long>()
        val validIds = matchIds.filter { it > 0L }.distinct()

        // PandaScore allows up to 100 results per page.
        for (ids in validIds.chunked(100)) {
            val idList = ids.joinToString(",")
            val url = URL(
                "https://api.pandascore.co/csgo/matches/past" +
                        "?filter%5Bid%5D=$idList&per_page=100"
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
                val matches = JSONArray(body)

                for (index in 0 until matches.length()) {
                    val match = matches.optJSONObject(index) ?: continue
                    val matchId = match.optLong("id", -1L)
                    val winnerId = match.optLong("winner_id", -1L)

                    if (matchId in ids &&
                        match.optString("status") == "finished" &&
                        match.optString("winner_type") == "Team" &&
                        winnerId > 0L
                    ) {
                        winners[matchId] = winnerId
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

        return winners
    }
}