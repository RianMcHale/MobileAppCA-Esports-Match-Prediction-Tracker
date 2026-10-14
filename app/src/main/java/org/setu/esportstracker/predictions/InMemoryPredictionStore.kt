package org.setu.esportstracker.predictions

object InMemoryPredictionStore : PredictionStore {

    // The match ID is the key, so each match has at most one prediction.
    private val predictions = mutableMapOf<Long, Prediction>()

    override fun findAll(): List<Prediction> =
        predictions.values.toList()

    override fun findOne(matchId: Long): Prediction? =
        predictions[matchId]

    override fun save(prediction: Prediction): Boolean {
        val match = prediction.match

        if (prediction.matchId != match.id) return false
        if (prediction.predictedTeamId != match.teamOneId &&
            prediction.predictedTeamId != match.teamTwoId
        ) return false
        if (predictions.containsKey(prediction.matchId)) return false

        predictions[prediction.matchId] = prediction
        return true
    }

    override fun applyOfficialResult(matchId: Long, winnerTeamId: Long): Boolean {
        val prediction = predictions[matchId] ?: return false
        val match = prediction.match

        // The winner must be one of the two teams in this match.
        if (winnerTeamId != match.teamOneId &&
            winnerTeamId != match.teamTwoId
        ) return false

        predictions[matchId] = prediction.copy(
            match = match.copy(winnerTeamId = winnerTeamId)
        )
        return true
    }
}