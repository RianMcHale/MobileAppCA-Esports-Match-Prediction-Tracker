package org.setu.esportstracker.predictions

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import org.setu.esportstracker.BuildConfig
import org.setu.esportstracker.R
import org.setu.esportstracker.matches.PandaScoreResultService

// shows saved picks, official winners, and prediction accuracy.
class PredictionHistoryActivity : AppCompatActivity() {

    // creates the history screen and toolbar.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prediction_history)

        // keeps the content clear of the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.historyRoot)
        ) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // adds the title and back arrow.
        val toolbar = findViewById<MaterialToolbar>(R.id.historyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.history_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.navigate_home)
        }
    }

    // redraws history and checks for new results when this screen opens.
    override fun onResume() {
        super.onResume()
        showHistory()
        refreshOfficialResults()
    }

    // fills the screen using predictions currently held in memory.
    private fun showHistory() {
        // newest picks appear first.
        val predictions = InMemoryPredictionStore.findAll()
            .sortedByDescending { it.createdAtUtcMillis }

        // pending matches are left out of the accuracy total.
        val summary = PredictionAccuracy.calculate(predictions)
        val summaryText = findViewById<TextView>(R.id.historySummary)
        val emptyMessage = findViewById<TextView>(R.id.historyEmptyMessage)
        val historyList = findViewById<LinearLayout>(R.id.historyList)

        // avoids showing a percentage when no match has a winner.
        summaryText.text = if (summary.evaluated == 0) {
            getString(R.string.no_completed_predictions)
        } else {
            getString(
                R.string.history_accuracy,
                summary.correct,
                summary.evaluated,
                summary.percentage
            )
        }

        // shows the empty message only when no predictions exist.
        emptyMessage.visibility =
            if (predictions.isEmpty()) View.VISIBLE else View.GONE

        // removes old cards before drawing the latest details.
        historyList.removeAllViews()

        for (prediction in predictions) {
            val match = prediction.match
            val card = layoutInflater.inflate(
                R.layout.item_prediction,
                historyList,
                false
            )

            // converts the chosen team id back to its display name.
            val pickedName =
                if (prediction.predictedTeamId == match.teamOneId) {
                    match.teamOneName
                } else {
                    match.teamTwoName
                }

            card.findViewById<TextView>(R.id.historyMatch).text =
                getString(
                    R.string.match_teams,
                    match.teamOneName,
                    match.teamTwoName
                )

            card.findViewById<TextView>(R.id.historyPick).text =
                getString(R.string.history_pick, pickedName)

            val winnerId = match.winnerTeamId

            // an unknown winner means the prediction is still pending.
            val resultText = if (winnerId == null) {
                getString(R.string.result_pending)
            } else {
                // looks up the official winner's name.
                val winnerName =
                    if (winnerId == match.teamOneId) {
                        match.teamOneName
                    } else {
                        match.teamTwoName
                    }

                // compares the user's team id with the winning team id.
                val verdict =
                    if (prediction.predictedTeamId == winnerId) {
                        getString(R.string.result_correct)
                    } else {
                        getString(R.string.result_incorrect)
                    }

                getString(R.string.history_result, winnerName, verdict)
            }

            card.findViewById<TextView>(R.id.historyResult).text = resultText
            historyList.addView(card)
        }
    }

    // prevents a second result check starting before the first one finishes.
    private var checkingResults = false

    // asks pandascore whether saved matches now have official winners.
    private fun refreshOfficialResults() {
        if (checkingResults) return

        val predictions = InMemoryPredictionStore.findAll()
        if (predictions.isEmpty()) return

        val syncStatus = findViewById<TextView>(R.id.historySyncStatus)
        syncStatus.visibility = View.VISIBLE

        // tells the user why syncing cannot start without a token.
        if (BuildConfig.PANDASCORE_TOKEN.isBlank()) {
            syncStatus.setText(R.string.pandascore_token_missing)
            return
        }

        checkingResults = true
        syncStatus.setText(R.string.checking_results)

        // network calls cannot run on the screen's main thread.
        Thread {
            try {
                // sends saved match ids and recieves finished winners by id.
                val winners = PandaScoreResultService.fetchFinishedWinners(
                    predictions.map { it.matchId }
                )

                // updates the store and screen on android's main thread.
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    // the store checks that each winner belongs to that match.
                    for ((matchId, winnerTeamId) in winners) {
                        InMemoryPredictionStore.applyOfficialResult(
                            matchId,
                            winnerTeamId
                        )
                    }

                    checkingResults = false
                    syncStatus.setText(R.string.results_checked)
                    showHistory()
                }
            } catch (error: Exception) {
                // keeps api failure details in logcat.
                Log.e(
                    "PredictionHistory",
                    "Could not check official results",
                    error
                )

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    // leaves existing picks visible if the check fails.
                    checkingResults = false
                    syncStatus.setText(R.string.results_check_failed)
                }
            }
        }.start()
    }

    // closes history when the toolbar back arrow is pressed.
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}