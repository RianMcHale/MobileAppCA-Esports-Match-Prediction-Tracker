package org.setu.esportstracker.predictions

object InMemoryPredictionStore : PredictionStore {

    // easch match id points at most one prediction
    private val predictions = mutableMapOf<Long, Prediction>()

    // return saved predictions
    override fun findAll(): List<Prediction> =
        predictions.values.toList()

    override fun findOne(matchId: Long): Prediction? =
        predictions[matchId]

    override fun save(prediction: Prediction): Boolean {
        if (predictions.containsKey(prediction.matchId)) {
            return false
        }

        predictions[prediction.matchId] = prediction
        return true
    }
}