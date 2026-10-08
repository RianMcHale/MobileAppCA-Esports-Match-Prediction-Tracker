package org.setu.esportstracker.predictions

data class Prediction(
    val matchId: Long,
    val predictedTeamId: Long,
    val createdAtUtcMillis: Long
)