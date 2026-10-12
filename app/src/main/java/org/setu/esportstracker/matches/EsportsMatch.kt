package org.setu.esportstracker.matches
import java.io.Serializable

data class EsportsMatch( // unique/stable Ids, display names, etc.
    val id: Long, // unique
    val teamOneId: Long, // stable
    val teamOneName: String, // display
    val teamTwoId: Long, // stable
    val teamTwoName: String, // display
    val game: String, // game
    val competition: String, // tournament
    val startTimeUtcMillis: Long, // schedule time in milliseconds
    val winnerTeamId: Long? = null // null means winner is not known yet
) : Serializable // lets the match be passed to the prediction screen