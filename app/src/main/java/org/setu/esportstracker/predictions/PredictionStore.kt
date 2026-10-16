package org.setu.esportstracker.predictions

// lists the actions a prediction store must support.
interface PredictionStore {
    // gets all saved predictions.
    fun findAll(): List<Prediction>

    // gets one prediction using its match id.
    fun findOne(matchId: Long): Prediction?

    // saves a new prediction if it is valid.
    fun save(prediction: Prediction): Boolean

    // adds an official winner to a saved prediction.
    fun applyOfficialResult(matchId: Long, winnerTeamId: Long): Boolean
}