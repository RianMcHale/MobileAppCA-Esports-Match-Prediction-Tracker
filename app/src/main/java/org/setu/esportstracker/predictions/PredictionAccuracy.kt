package org.setu.esportstracker.predictions

data class AccuracySummary(
    val correct: Int,
    val evaluated: Int
) {
    val percentage: Double
        get() = if (evaluated == 0) {
            0.0
        } else {
            correct * 100.0 / evaluated
        }
}

object PredictionAccuracy {

    fun calculate(predictions: List<Prediction>): AccuracySummary {
        // A prediction is evaluated only when its match has a winner.
        val completed = predictions.filter {
            it.match.winnerTeamId != null
        }

        val correct = completed.count {
            it.predictedTeamId == it.match.winnerTeamId
        }

        return AccuracySummary(
            correct = correct,
            evaluated = completed.size
        )
    }
}