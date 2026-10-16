package org.setu.esportstracker.predictions

// holds predictions while this app process is running.
object InMemoryPredictionStore : PredictionStore {

    // one match id points to at most one prediction.
    private val predictions = mutableMapOf<Long, Prediction>()

    // returns all saved predictions as a list.
    override fun findAll(): List<Prediction> =
        predictions.values.toList()

    // returns the prediction for a match, if one exists.
    override fun findOne(matchId: Long): Prediction? =
        predictions[matchId]

    // saves a choice only when its match and team ids are valid.
    override fun save(prediction: Prediction): Boolean {
        val match = prediction.match

        // the prediction must point to the match it carries.
        if (prediction.matchId != match.id) return false

        // the pick must be one of the two teams in that match.
        if (prediction.predictedTeamId != match.teamOneId &&
            prediction.predictedTeamId != match.teamTwoId
        ) return false

        // a second pick for the same match is not allowed.
        if (predictions.containsKey(prediction.matchId)) return false

        predictions[prediction.matchId] = prediction
        return true
    }

    // places a valid official winner into the saved match copy.
    override fun applyOfficialResult(matchId: Long, winnerTeamId: Long): Boolean {
        // cannot update a prediction that was never saved.
        val prediction = predictions[matchId] ?: return false
        val match = prediction.match

        // rejects an id that is not one of this match's teams.
        if (winnerTeamId != match.teamOneId &&
            winnerTeamId != match.teamTwoId
        ) return false

        // copies the prediction because its data class values cannot be changed directly.
        predictions[matchId] = prediction.copy(
            match = match.copy(winnerTeamId = winnerTeamId)
        )
        return true
    }
}