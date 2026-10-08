package org.setu.esportstracker.matches

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import org.setu.esportstracker.R
import android.content.Intent
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

        // uses toolbar as the app bar and displays an up arrow
        val toolbar = findViewById<MaterialToolbar>(R.id.matchToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.sample_matches_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.navigate_home)
        }

        // loads sample matches and finds list and empty-message views
        val matches: List<EsportsMatch> = InMemoryMatchStore().findAll()
        val matchList = findViewById<RecyclerView>(R.id.matchesRecyclerView)
        val emptyMessage = findViewById<TextView>(R.id.emptyMessage)

        // places match cards in verticle list
        matchList.layoutManager = LinearLayoutManager(this)

        // opens prediction screen with tapped match IDs
        matchList.adapter = MatchAdapter(matches) { selectedMatch ->
            val intent = Intent(this, PredictionActivity::class.java)
            intent.putExtra(PredictionActivity.EXTRA_MATCH_ID, selectedMatch.id)
            startActivity(intent)
        }

        // shows either the list or the empty message
        matchList.visibility = if (matches.isEmpty()) View.GONE else View.VISIBLE
        emptyMessage.visibility = if (matches.isEmpty()) View.VISIBLE else View.GONE
    }

    // return to previous screen when toolbars up arrow is pressed
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}