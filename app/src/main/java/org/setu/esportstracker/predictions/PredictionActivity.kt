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

class PredictionActivity : AppCompatActivity() { // lets user choose the predicted winner for the match

    companion object {
        const val EXTRA_MATCH_ID = "org.setu.esportstracker.extra.MATCH_ID" // key used to pass the match id
        const val EXTRA_MATCH = "org.setu.esportstracker.extra.MATCH" // key used to pass the selected match
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prediction)

        // adds padding so the form is not hidden by system bars
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.predictionRoot)
        ) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // sets up the title and up arrow in the toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.predictionToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.prediction_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.back_to_matches)
        }

        // reads the match id and details sent from MatchListActivity
        val matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
        val match = IntentCompat.getSerializableExtra(
            intent,
            EXTRA_MATCH,
            EsportsMatch::class.java
        )?.takeIf { it.id == matchId } // only uses the match if the ids agree

        // closes the form if the match was not passed correctly
        if (match == null) {
            Toast.makeText(this, R.string.match_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // shows the selected match and competition at the top of the form
        findViewById<TextView>(R.id.predictionMatchTitle).text =
            getString(R.string.match_teams, match.teamOneName, match.teamTwoName)

        findViewById<TextView>(R.id.predictionCompetition).text =
            getString(R.string.match_game_competition, match.game, match.competition)

        // finds the controls used to choose and save a team
        val teamOneRadio = findViewById<RadioButton>(R.id.teamOneRadio)
        val teamTwoRadio = findViewById<RadioButton>(R.id.teamTwoRadio)
        val teamChoices = findViewById<RadioGroup>(R.id.teamChoiceGroup)
        val selectionError = findViewById<TextView>(R.id.selectionError)
        val status = findViewById<TextView>(R.id.predictionStatus)
        val saveButton = findViewById<Button>(R.id.savePredictionButton)

        // puts the team names beside the radio buttons
        teamOneRadio.text = match.teamOneName
        teamTwoRadio.text = match.teamTwoName

        // displays an existing choice and stops a second submission
        fun showSavedPrediction(prediction: Prediction) {
            val choseTeamOne = prediction.predictedTeamId == match.teamOneId
            val teamName = if (choseTeamOne) match.teamOneName else match.teamTwoName

            teamChoices.check(
                if (choseTeamOne) R.id.teamOneRadio else R.id.teamTwoRadio
            )

            teamOneRadio.isEnabled = false
            teamTwoRadio.isEnabled = false
            saveButton.isEnabled = false

            status.text = getString(R.string.prediction_saved_for, teamName)
            status.visibility = View.VISIBLE
        }

        // hides the validation error when choosing a team
        teamChoices.setOnCheckedChangeListener { _, _ ->
            selectionError.visibility = View.GONE
        }

        // shows the saved choice if this match already has a prediction
        InMemoryPredictionStore.findOne(match.id)?.let {
            showSavedPrediction(it)
        }

        // checks the choice and tries to save when the button is pressed
        saveButton.setOnClickListener {
            val chosenTeamId = when (teamChoices.checkedRadioButtonId) {
                R.id.teamOneRadio -> match.teamOneId
                R.id.teamTwoRadio -> match.teamTwoId
                else -> null
            }

            // requires a team to be selected before saving
            if (chosenTeamId == null) {
                selectionError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // creates a prediction using the team id and current time
            val prediction = Prediction(
                matchId = match.id,
                predictedTeamId = chosenTeamId,
                createdAtUtcMillis = System.currentTimeMillis()
            )

            // shows the saved choice, including one that was already stored
            if (InMemoryPredictionStore.save(prediction)) {
                showSavedPrediction(prediction)
            } else {
                InMemoryPredictionStore.findOne(match.id)?.let {
                    showSavedPrediction(it)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}