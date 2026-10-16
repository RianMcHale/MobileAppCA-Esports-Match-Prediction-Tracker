package org.setu.esportstracker.predictions

// holds the correct and completed prediction counts.
data class AccuracySummary(
    val correct: Int, // picks that matched the winner.
    val evaluated: Int // picks with a known winner.
) {
    // calculates the percentage only when division is safe.
    val percentage: Double
        get() = if (evaluated == 0) {
            0.0
        } else {
            correct * 100.0 / evaluated
        }
}

// works out the accuracy shown on the history screen.
object PredictionAccuracy {

    // leaves unfinished matches out of the total.
    fun calculate(predictions: List<Prediction>): AccuracySummary {
        // a match with no winner cannot be marked correct or wrong yet.
        val completed = predictions.filter {
            it.match.winnerTeamId != null
        }

        // counts picks where the chosen team id is the winner id.
        val correct = completed.count {
            it.predictedTeamId == it.match.winnerTeamId
        }

        return AccuracySummary(
            correct = correct,
            evaluated = completed.size
        )
    }
}