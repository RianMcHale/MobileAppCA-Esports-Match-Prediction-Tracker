package org.setu.esportstracker.predictions

import org.setu.esportstracker.matches.EsportsMatch

data class Prediction(
    val matchId: Long,
    val predictedTeamId: Long,
    val createdAtUtcMillis: Long,
    val match: EsportsMatch
)