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
import com.google.android.material.tabs.TabLayout
import org.setu.esportstracker.BuildConfig
import org.setu.esportstracker.R
import org.setu.esportstracker.predictions.PredictionActivity

// shows upcoming, live, or completed cs2 matches.
class MatchListActivity : AppCompatActivity() {

    // increases for every request so an old tab cannot replace a newer tab.
    private var latestRequestId = 0

    // creates the screen and connects its tabs.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_list)

        // keeps the layout clear of the status and navigation bars.
        val root = findViewById<View>(R.id.matchListRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // shows the title and back arrow.
        val toolbar = findViewById<MaterialToolbar>(R.id.matchToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.matches_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.navigate_home)
        }

        // arranges match cards in one vertical list.
        findViewById<RecyclerView>(R.id.matchesRecyclerView)
            .layoutManager = LinearLayoutManager(this)

        // loads the matching category when a tab is selected.
        val tabs = findViewById<TabLayout>(R.id.matchTabs)
        tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                // reads the new tab position and loads its matches.
                override fun onTabSelected(tab: TabLayout.Tab) {
                    loadMatches(categoryFor(tab.position))
                }

                // no action is needed for the tab being left.
                override fun onTabUnselected(tab: TabLayout.Tab) {
                }

                // tapping the same tab again checks for newer data.
                override fun onTabReselected(tab: TabLayout.Tab) {
                    loadMatches(categoryFor(tab.position))
                }
            }
        )

        // upcoming is the first category shown when the screen opens.
        loadMatches(MatchCategory.UPCOMING)
    }

    // changes a tab position into its match category.
    private fun categoryFor(position: Int): MatchCategory =
        when (position) {
            1 -> MatchCategory.LIVE
            2 -> MatchCategory.COMPLETED
            else -> MatchCategory.UPCOMING
        }

    // loads the selected category without blocking the screen.
    private fun loadMatches(category: MatchCategory) {
        // this number lets the response check if it still belongs to the current tab.
        val requestId = ++latestRequestId
        val matchList = findViewById<RecyclerView>(R.id.matchesRecyclerView)
        val emptyMessage = findViewById<TextView>(R.id.emptyMessage)

        // clears the last tab's cards while the new tab loads.
        matchList.adapter = null
        matchList.visibility = View.GONE
        emptyMessage.visibility = View.VISIBLE

        // the api request cannot work without the token.
        if (BuildConfig.PANDASCORE_TOKEN.isBlank()) {
            emptyMessage.setText(R.string.pandascore_token_missing)
            return
        }

        emptyMessage.setText(R.string.loading_matches)

        // network work runs on another thread so taps and drawing can continue.
        Thread {
            try {
                // asks pandascore for the category selected by this request.
                val matches = PandaScoreMatchStore.refresh(category)

                // screen views must be changed on android's main thread.
                runOnUiThread {
                    // a closed screen or newer tab should ignore this old response.
                    if (isFinishing || isDestroyed ||
                        requestId != latestRequestId
                    ) {
                        return@runOnUiThread
                    }

                    // connects each returned match to a card.
                    matchList.adapter = MatchAdapter(
                        matches,
                        category
                    ) { selectedMatch ->
                        // upcoming cards open the prediction form with this match.
                        val intent = Intent(
                            this,
                            PredictionActivity::class.java
                        )
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

                    // shows either the cards or the empty message.
                    matchList.visibility =
                        if (matches.isEmpty()) View.GONE else View.VISIBLE
                    emptyMessage.visibility =
                        if (matches.isEmpty()) View.VISIBLE else View.GONE

                    // each tab has a message that fits its own result.
                    val emptyText = when (category) {
                        MatchCategory.UPCOMING ->
                            R.string.no_upcoming_matches
                        MatchCategory.LIVE ->
                            R.string.no_live_matches
                        MatchCategory.COMPLETED ->
                            R.string.no_completed_matches
                    }
                    emptyMessage.setText(emptyText)
                }
            } catch (error: Exception) {
                // records the real error in logcat for debugging.
                Log.e(
                    "MatchListActivity",
                    "Could not load PandaScore matches",
                    error
                )

                runOnUiThread {
                    // do not show an error from an old tab over the new one.
                    if (isFinishing || isDestroyed ||
                        requestId != latestRequestId
                    ) {
                        return@runOnUiThread
                    }

                    // lets the user know this request failed.
                    matchList.visibility = View.GONE
                    emptyMessage.visibility = View.VISIBLE
                    emptyMessage.setText(R.string.matches_load_failed)
                }
            }
        }.start()
    }

    // closes this screen when the toolbar back arrow is pressed.
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}