package org.setu.esportstracker.matches

import org.json.JSONArray
import org.setu.esportstracker.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// checks official finished matches for the winners of saved predictions.
object PandaScoreResultService {

    // returns a map from match id to winning team id.
    fun fetchFinishedWinners(matchIds: List<Long>): Map<Long, Long> {
        // the request cannot be made without the app's api token.
        check(BuildConfig.PANDASCORE_TOKEN.isNotBlank()) {
            "PandaScore token is missing"
        }

        val winners = mutableMapOf<Long, Long>()

        // removes bad ids and duplicates before they reach the api.
        val validIds = matchIds.filter { it > 0L }.distinct()

        // limits each request to 100 ids, matching the requested page size.
        for (ids in validIds.chunked(100)) {
            // pandascore accepts a comma-separated list in its id filter.
            val idList = ids.joinToString(",")

            // %5b and %5d are the encoded square brackets in filter[id].
            val url = URL(
                "https://api.pandascore.co/csgo/matches/past" +
                        "?filter%5Bid%5D=$idList&per_page=100"
            )
            val connection = url.openConnection() as HttpURLConnection

            try {
                // uses get and stops if connecting or reading takes too long.
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")

                // sends the same token used for the match list.
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer ${BuildConfig.PANDASCORE_TOKEN}"
                )

                // stops instead of treating an api error as an empty result.
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException(
                        "PandaScore returned HTTP ${connection.responseCode}"
                    )
                }

                // reads the response and turns it into a list of json matches.
                val body = connection.inputStream.bufferedReader().use {
                    it.readText()
                }
                val matches = JSONArray(body)

                for (index in 0 until matches.length()) {
                    // skips entries that are not valid json objects.
                    val match = matches.optJSONObject(index) ?: continue
                    val matchId = match.optLong("id", -1L)
                    val winnerId = match.optLong("winner_id", -1L)

                    // only records a finished team win for a requested match.
                    if (matchId in ids &&
                        match.optString("status") == "finished" &&
                        match.optString("winner_type") == "Team" &&
                        winnerId > 0L
                    ) {
                        winners[matchId] = winnerId
                    }
                }
            } finally {
                // always frees this network connection.
                connection.disconnect()
            }
        }

        return winners
    }
}