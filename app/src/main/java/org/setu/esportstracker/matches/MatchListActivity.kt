package org.setu.esportstracker.matches

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import org.setu.esportstracker.BuildConfig
import org.setu.esportstracker.R
import org.setu.esportstracker.predictions.PredictionActivity

class MatchListActivity : AppCompatActivity() {

    // sets up the screen when android creates it
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_list)

        // keeps the screen content clear of system bars
        val root = findViewById<View>(R.id.matchListRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // uses the toolbar as the app bar and shows an up arrow
        val toolbar = findViewById<MaterialToolbar>(R.id.matchToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.upcoming_matches_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.navigate_home)
        }

        // finds the match list and empty-message views
        val matchList = findViewById<RecyclerView>(R.id.matchesRecyclerView)
        val emptyMessage = findViewById<TextView>(R.id.emptyMessage)
        matchList.layoutManager = LinearLayoutManager(this)
        matchList.visibility = View.GONE
        emptyMessage.visibility = View.VISIBLE

        // tells the user if the token has not been added
        if (BuildConfig.PANDASCORE_TOKEN.isBlank()) {
            emptyMessage.text = getString(R.string.pandascore_token_missing)
            return
        }

        // shows a message while matches are loading
        emptyMessage.text = getString(R.string.loading_matches)

        // loads matches without blocking the screen
        Thread {
            try {
                val matches = PandaScoreMatchStore.refreshUpcoming()

                runOnUiThread {
                    // stops updating the screen if the user has left it
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    // places the loaded match cards in a vertical list
                    matchList.adapter = MatchAdapter(matches) { selectedMatch ->
                        // opens the prediction screen with the selected match
                        val intent = Intent(this, PredictionActivity::class.java)
                        intent.putExtra(
                            PredictionActivity.EXTRA_MATCH_ID,
                            selectedMatch.id
                        )
                        intent.putExtra(
                            PredictionActivity.EXTRA_MATCH,
                            selectedMatch
                        )
                        startActivity(intent)
                    }

                    // shows either the matches or the empty message
                    matchList.visibility =
                        if (matches.isEmpty()) View.GONE else View.VISIBLE
                    emptyMessage.visibility =
                        if (matches.isEmpty()) View.VISIBLE else View.GONE
                    emptyMessage.text = getString(R.string.no_matches)
                }
            } catch (error: Exception) {
                Log.e("MatchListActivity", "Could not load PandaScore matches", error)

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    // shows an error message if the matches could not load
                    matchList.visibility = View.GONE
                    emptyMessage.visibility = View.VISIBLE
                    emptyMessage.text = getString(R.string.matches_load_failed)
                }
            }
        }.start()
    }

    // returns to the previous screen when the up arrow is pressed
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}