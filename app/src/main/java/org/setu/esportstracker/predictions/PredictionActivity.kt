package org.setu.esportstracker.predictions

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import org.setu.esportstracker.R
import org.setu.esportstracker.matches.EsportsMatch

// lets the user pick a winner for an upcoming match.
class PredictionActivity : AppCompatActivity() {

    // keeps the names used when another activity sends match data here.
    companion object {
        const val EXTRA_MATCH_ID = "org.setu.esportstracker.extra.MATCH_ID"
        const val EXTRA_MATCH = "org.setu.esportstracker.extra.MATCH"
    }

    // creates the form and reads the selected match.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prediction)

        // stops system bars from covering the form.
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.predictionRoot)
        ) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // sets the form title and back arrow.
        val toolbar = findViewById<MaterialToolbar>(R.id.predictionToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.prediction_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.back_to_matches)
        }

        // reads the id sent by the match list.
        val matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)

        // reads the match object and checks it agrees with the id.
        val match = IntentCompat.getSerializableExtra(
            intent,
            EXTRA_MATCH,
            EsportsMatch::class.java
        )?.takeIf { it.id == matchId }

        // closes the form if the sent match is missing or mismatched.
        if (match == null) {
            Toast.makeText(this, R.string.match_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // shows the teams and competition before the user picks.
        findViewById<TextView>(R.id.predictionMatchTitle).text =
            getString(R.string.match_teams, match.teamOneName, match.teamTwoName)

        findViewById<TextView>(R.id.predictionCompetition).text =
            getString(R.string.match_game_competition, match.game, match.competition)

        // finds the inputs, error message, save button, and saved status.
        val teamOneRadio = findViewById<RadioButton>(R.id.teamOneRadio)
        val teamTwoRadio = findViewById<RadioButton>(R.id.teamTwoRadio)
        val teamChoices = findViewById<RadioGroup>(R.id.teamChoiceGroup)
        val selectionError = findViewById<TextView>(R.id.selectionError)
        val status = findViewById<TextView>(R.id.predictionStatus)
        val saveButton = findViewById<Button>(R.id.savePredictionButton)

        // names each radio button after its team.
        teamOneRadio.text = match.teamOneName
        teamTwoRadio.text = match.teamTwoName

        // shows an existing pick and prevents it being saved again.
        fun showSavedPrediction(prediction: Prediction) {
            val choseTeamOne = prediction.predictedTeamId == match.teamOneId
            val teamName = if (choseTeamOne) match.teamOneName else match.teamTwoName

            // checks the radio button that matches the saved team id.
            teamChoices.check(
                if (choseTeamOne) R.id.teamOneRadio else R.id.teamTwoRadio
            )

            // locks the choices and save button after a pick exists.
            teamOneRadio.isEnabled = false
            teamTwoRadio.isEnabled = false
            saveButton.isEnabled = false

            // tells the user which team was saved.
            status.text = getString(R.string.prediction_saved_for, teamName)
            status.visibility = View.VISIBLE
        }

        // clears the missing-choice error when the user selects a team.
        teamChoices.setOnCheckedChangeListener { _, _ ->
            selectionError.visibility = View.GONE
        }

        // restores a pick if the user already predicted this match.
        InMemoryPredictionStore.findOne(match.id)?.let {
            showSavedPrediction(it)
        }

        // checks the selected radio button when save is pressed.
        saveButton.setOnClickListener {
            val chosenTeamId = when (teamChoices.checkedRadioButtonId) {
                R.id.teamOneRadio -> match.teamOneId
                R.id.teamTwoRadio -> match.teamTwoId
                else -> null
            }

            // refuses an empty choice.
            if (chosenTeamId == null) {
                selectionError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // stores the chosen team, current time, and match details together.
            val prediction = Prediction(
                matchId = match.id,
                predictedTeamId = chosenTeamId,
                createdAtUtcMillis = System.currentTimeMillis(),
                match = match
            )

            // shows the saved pick, or the existing pick if save was rejected.
            if (InMemoryPredictionStore.save(prediction)) {
                showSavedPrediction(prediction)
            } else {
                InMemoryPredictionStore.findOne(match.id)?.let {
                    showSavedPrediction(it)
                }
            }
        }
    }

    // closes the form when the toolbar back arrow is pressed.
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}