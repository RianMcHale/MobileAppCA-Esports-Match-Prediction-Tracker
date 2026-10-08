package org.setu.esportstracker.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.setu.esportstracker.R
import java.text.DateFormat
import java.util.Date

class MatchAdapter(
    private val matches: List<EsportsMatch>
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamsText: TextView = itemView.findViewById(R.id.teamsText)
        val competitionText: TextView = itemView.findViewById(R.id.competitionText)
        val scheduledText: TextView = itemView.findViewById(R.id.scheduledText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)

        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        val context = holder.itemView.context

        holder.teamsText.text = context.getString(
            R.string.match_teams,
            match.teamOneName,
            match.teamTwoName
        )

        holder.competitionText.text = context.getString(
            R.string.match_game_competition,
            match.game,
            match.competition
        )

        val localTime = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(match.startTimeUtcMillis))

        holder.scheduledText.text = context.getString(
            R.string.match_scheduled,
            localTime
        )
    }

    override fun getItemCount(): Int = matches.size
}