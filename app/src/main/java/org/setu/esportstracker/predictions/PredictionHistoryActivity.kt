package org.setu.esportstracker.predictions

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.setu.esportstracker.R
import android.util.Log
import org.setu.esportstracker.BuildConfig
import org.setu.esportstracker.matches.PandaScoreResultService


class PredictionHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prediction_history)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.historyRoot)
        ) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.historyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.history_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.navigate_home)
        }
    }

    override fun onResume() {
        super.onResume()
        showHistory()
        refreshOfficialResults()
    }

    private fun showHistory() {
        val predictions = InMemoryPredictionStore.findAll()
            .sortedByDescending { it.createdAtUtcMillis }

        val summary = PredictionAccuracy.calculate(predictions)
        val summaryText = findViewById<TextView>(R.id.historySummary)
        val emptyMessage = findViewById<TextView>(R.id.historyEmptyMessage)
        val historyList = findViewById<LinearLayout>(R.id.historyList)

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

        emptyMessage.visibility =
            if (predictions.isEmpty()) View.VISIBLE else View.GONE

        historyList.removeAllViews()

        for (prediction in predictions) {
            val match = prediction.match
            val card = layoutInflater.inflate(
                R.layout.item_prediction,
                historyList,
                false
            )

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
            val resultText = if (winnerId == null) {
                getString(R.string.result_pending)
            } else {
                val winnerName =
                    if (winnerId == match.teamOneId) {
                        match.teamOneName
                    } else {
                        match.teamTwoName
                    }

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

    private var checkingResults = false

    private fun refreshOfficialResults() {
        if (checkingResults) return

        val predictions = InMemoryPredictionStore.findAll()
        if (predictions.isEmpty()) return

        val syncStatus = findViewById<TextView>(R.id.historySyncStatus)
        syncStatus.visibility = View.VISIBLE

        if (BuildConfig.PANDASCORE_TOKEN.isBlank()) {
            syncStatus.setText(R.string.pandascore_token_missing)
            return
        }

        checkingResults = true
        syncStatus.setText(R.string.checking_results)

        Thread {
            try {
                val winners = PandaScoreResultService.fetchFinishedWinners(
                    predictions.map { it.matchId }
                )

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

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
                Log.e(
                    "PredictionHistory",
                    "Could not check official results",
                    error
                )

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    checkingResults = false
                    syncStatus.setText(R.string.results_check_failed)
                }
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}