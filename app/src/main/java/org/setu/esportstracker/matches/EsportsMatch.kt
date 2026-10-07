package org.setu.esportstracker.matches

data class EsportsMatch(
    val id: Long,
    val teamOneId: Long,
    val teamOneName: String,
    val teamTwoId: Long,
    val teamTwoName: String,
    val game: String,
    val competition: String,
    val startTimeUtcMillis: Long,

    // null means winner is not known yet
    val winnerTeamId: Long? = null
)