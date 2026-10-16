package org.setu.esportstracker.matches

import java.io.Serializable

// holds the details needed to show a match and make a prediction.
data class EsportsMatch(
    val id: Long, // identifies this match.
    val teamOneId: Long, // identifies the first team.
    val teamOneName: String, // shows the first team's name.
    val teamTwoId: Long, // identifies the second team.
    val teamTwoName: String, // shows the second team's name.
    val game: String, // names the game, such as cs2.
    val competition: String, // names the tournament.
    val startTimeUtcMillis: Long, // stores the start time in utc milliseconds.
    val winnerTeamId: Long? = null // stays null until a winner is known.
) : Serializable // allows the match to be sent to another activity.