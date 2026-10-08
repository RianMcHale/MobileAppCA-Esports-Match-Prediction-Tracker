package org.setu.esportstracker.predictions

interface PredictionStore {
    fun findAll(): List<Prediction>
    fun findOne(matchId: Long): Prediction?
    fun save(prediction: Prediction): Boolean
}