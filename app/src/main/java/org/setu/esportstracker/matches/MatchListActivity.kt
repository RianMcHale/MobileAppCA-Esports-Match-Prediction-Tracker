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

class MatchListActivity : AppCompatActivity() {

    // Identifies the newest request when users switch tabs quickly.
    private var latestRequestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_list)

        val root = findViewById<View>(R.id.matchListRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.matchToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.matches_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeActionContentDescription(R.string.navigate_home)
        }

        findViewById<RecyclerView>(R.id.matchesRecyclerView)
            .layoutManager = LinearLayoutManager(this)

        val tabs = findViewById<TabLayout>(R.id.matchTabs)
        tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    loadMatches(categoryFor(tab.position))
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    // Nothing is needed when leaving a tab.
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    // Tapping the current tab checks for fresh matches.
                    loadMatches(categoryFor(tab.position))
                }
            }
        )

        // The first tab is Upcoming.
        loadMatches(MatchCategory.UPCOMING)
    }

    private fun categoryFor(position: Int): MatchCategory =
        when (position) {
            1 -> MatchCategory.LIVE
            2 -> MatchCategory.COMPLETED
            else -> MatchCategory.UPCOMING
        }

    private fun loadMatches(category: MatchCategory) {
        val requestId = ++latestRequestId
        val matchList = findViewById<RecyclerView>(R.id.matchesRecyclerView)
        val emptyMessage = findViewById<TextView>(R.id.emptyMessage)

        matchList.adapter = null
        matchList.visibility = View.GONE
        emptyMessage.visibility = View.VISIBLE

        if (BuildConfig.PANDASCORE_TOKEN.isBlank()) {
            emptyMessage.setText(R.string.pandascore_token_missing)
            return
        }

        emptyMessage.setText(R.string.loading_matches)

        Thread {
            try {
                val matches = PandaScoreMatchStore.refresh(category)

                runOnUiThread {
                    if (isFinishing || isDestroyed ||
                        requestId != latestRequestId
                    ) {
                        return@runOnUiThread
                    }

                    matchList.adapter = MatchAdapter(
                        matches,
                        category
                    ) { selectedMatch ->
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

                    matchList.visibility =
                        if (matches.isEmpty()) View.GONE else View.VISIBLE
                    emptyMessage.visibility =
                        if (matches.isEmpty()) View.VISIBLE else View.GONE

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
                Log.e(
                    "MatchListActivity",
                    "Could not load PandaScore matches",
                    error
                )

                runOnUiThread {
                    if (isFinishing || isDestroyed ||
                        requestId != latestRequestId
                    ) {
                        return@runOnUiThread
                    }

                    matchList.visibility = View.GONE
                    emptyMessage.visibility = View.VISIBLE
                    emptyMessage.setText(R.string.matches_load_failed)
                }
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}