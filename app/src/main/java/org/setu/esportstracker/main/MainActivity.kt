package org.setu.esportstracker.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.setu.esportstracker.R
import org.setu.esportstracker.matches.MatchListActivity

class MainActivity : AppCompatActivity() { // first screen when the app opens up

    // android calls thist to create the screen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // adds padding so the content isnt blocked by status/nav bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // opens the match list when user presses the button
        findViewById<Button>(R.id.viewMatchesButton).setOnClickListener {
            startActivity(Intent(this, MatchListActivity::class.java))
        }
    }
}