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
import org.setu.esportstracker.R

class MatchListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_list)

        val root = findViewById<View>(R.id.matchListRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val matches: List<EsportsMatch> = InMemoryMatchStore().findAll()
        val matchList = findViewById<RecyclerView>(R.id.matchesRecyclerView)
        val emptyMessage = findViewById<TextView>(R.id.emptyMessage)

        matchList.layoutManager = LinearLayoutManager(this)
        matchList.adapter = MatchAdapter(matches)

        matchList.visibility = if (matches.isEmpty()) View.GONE else View.VISIBLE
        emptyMessage.visibility = if (matches.isEmpty()) View.VISIBLE else View.GONE
    }
}