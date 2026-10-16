package org.setu.esportstracker.predictions

import org.setu.esportstracker.matches.EsportsMatch

// keeps the user's choice and the match details needed by history.
data class Prediction(
    val matchId: Long, // identifies the predicted match.
    val predictedTeamId: Long, // identifies the team the user picked.
    val createdAtUtcMillis: Long, // records when the pick was saved.
    val match: EsportsMatch // keeps the teams available after the list changes.
)