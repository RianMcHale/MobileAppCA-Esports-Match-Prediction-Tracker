package org.setu.esportstracker.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.setu.esportstracker.R
import java.text.DateFormat
import java.util.Date

// connects match data to recycler view
class MatchAdapter(
    private val matches: List<EsportsMatch>,

    // activity provides this function in order to handle when a match is tapped / clicked on
    private val onMatchClick: (EsportsMatch) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    // reference to the views in the match card
    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamsText: TextView = itemView.findViewById(R.id.teamsText)
        val competitionText: TextView = itemView.findViewById(R.id.competitionText)
        val scheduledText: TextView = itemView.findViewById(R.id.scheduledText)
    }

    // creates a new match card when recylerview needs one
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)

        return MatchViewHolder(view)
    }

    // puts match info into the card
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        val context = holder.itemView.context

        holder.teamsText.text = context.getString(
            R.string.match_teams,
            match.teamOneName,
            match.teamTwoName
        ) // team names

        holder.competitionText.text = context.getString(
            R.string.match_game_competition,
            match.game,
            match.competition
        ) // game and comp

        val localTime = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(match.startTimeUtcMillis))

        holder.scheduledText.text = context.getString(
            R.string.match_scheduled,
            localTime
        )  // converts stored time into readable time for the device

        holder.itemView.contentDescription = context.getString(
            R.string.open_prediction_for_match,
            match.teamOneName,
            match.teamTwoName
        ) // gives decription of the tappable card

        holder.itemView.setOnClickListener {
            onMatchClick(match)
        } // tells activity which match wsa tapped
    }

    override fun getItemCount(): Int = matches.size
}