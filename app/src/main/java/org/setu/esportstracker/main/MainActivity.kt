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
import org.setu.esportstracker.predictions.PredictionHistoryActivity

// shows the home screen and its navigation buttons.
class MainActivity : AppCompatActivity() {

    // creates the home screen when android opens this activity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // lets the layout draw behind the system bars.
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // adds space so the status and navigation bars do not cover the content.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // opens the match list when its button is pressed.
        findViewById<Button>(R.id.viewMatchesButton).setOnClickListener {
            startActivity(Intent(this, MatchListActivity::class.java))
        }

        // opens saved predictions when its button is pressed.
        findViewById<Button>(R.id.viewHistoryButton).setOnClickListener {
            startActivity(Intent(this, PredictionHistoryActivity::class.java))
        }
    }
}