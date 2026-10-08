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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import org.setu.esportstracker.R
import org.setu.esportstracker.matches.InMemoryMatchStore

class PredictionActivity : AppCompatActivity() { // lets user choose the predicted winner for the match

    companion object {
        const val EXTRA_MATCH_ID = "org.setu.esportstracker.extra.MATCH_ID" // use this key to pass and read the match ID
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

        // set up the title and up arrow in tool bar
        val toolbar = findViewById<MaterialToolbar>(R.id.predictionToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.prediction_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.back_to_matches)
        }

        // read match id from MatchListActivity
        val matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
        // find match using the id above
        val match = InMemoryMatchStore().findOne(matchId)

        // closes form if id does not refer to a match
        if (match == null) {
            Toast.makeText(this, R.string.match_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // shows selected match and comp at top of the form
        findViewById<TextView>(R.id.predictionMatchTitle).text =
            getString(R.string.match_teams, match.teamOneName, match.teamTwoName)

        findViewById<TextView>(R.id.predictionCompetition).text =
            getString(R.string.match_game_competition, match.game, match.competition)

        // find controls used to choose and save a team
        val teamOneRadio = findViewById<RadioButton>(R.id.teamOneRadio)
        val teamTwoRadio = findViewById<RadioButton>(R.id.teamTwoRadio)
        val teamChoices = findViewById<RadioGroup>(R.id.teamChoiceGroup)
        val selectionError = findViewById<TextView>(R.id.selectionError)
        val status = findViewById<TextView>(R.id.predictionStatus)
        val saveButton = findViewById<Button>(R.id.savePredictionButton)

        // puts the real team names beside the radio buttons
        teamOneRadio.text = match.teamOneName
        teamTwoRadio.text = match.teamTwoName

        // display an existing/saved choice and stops a second submission
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

        // if match has a prediction already, show that
        InMemoryPredictionStore.findOne(match.id)?.let {
            showSavedPrediction(it)
        }

        // checks choice and tries to save when button is pressed
        saveButton.setOnClickListener {
            val chosenTeamId = when (teamChoices.checkedRadioButtonId) {
                R.id.teamOneRadio -> match.teamOneId
                R.id.teamTwoRadio -> match.teamTwoId
                else -> null
            }

            // team must be selected to save prediction
            if (chosenTeamId == null) {
                selectionError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // creates prediction using thes table IDs and current time
            val prediction = Prediction(
                matchId = match.id,
                predictedTeamId = chosenTeamId,
                createdAtUtcMillis = System.currentTimeMillis()
            )

            // shows saved predict ion, if one exists already then it shows that one
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