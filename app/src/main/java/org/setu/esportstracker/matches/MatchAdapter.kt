package org.setu.esportstracker.matches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.setu.esportstracker.R
import java.text.DateFormat
import java.util.Date

// puts match details into the cards shown in the list.
class MatchAdapter(
    private val matches: List<EsportsMatch>, // matches to display.
    private val category: MatchCategory, // the selected tab.
    private val onMatchClick: (EsportsMatch) -> Unit // action for an upcoming match.
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    // keeps references to the text views inside one match card.
    class MatchViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val teamsText: TextView = itemView.findViewById(R.id.teamsText)
        val competitionText: TextView =
            itemView.findViewById(R.id.competitionText)
        val scheduledText: TextView =
            itemView.findViewById(R.id.scheduledText)
        val statusText: TextView =
            itemView.findViewById(R.id.matchStatusText)
    }

    // creates a card when the list needs another one on screen.
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    // fills one card with the match at this list position.
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        val context = holder.itemView.context

        // shows both team names.
        holder.teamsText.text = context.getString(
            R.string.match_teams,
            match.teamOneName,
            match.teamTwoName
        )

        // shows the game and competition.
        holder.competitionText.text = context.getString(
            R.string.match_game_competition,
            match.game,
            match.competition
        )

        // turns the stored utc time into the device's local date and time.
        val localTime = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(match.startTimeUtcMillis))

        holder.scheduledText.text = context.getString(
            R.string.match_scheduled,
            localTime
        )

        // upcoming cards do not need an extra status line.
        holder.statusText.visibility =
            if (category == MatchCategory.UPCOMING) View.GONE else View.VISIBLE

        // live cards show their status; completed cards try to show the winner.
        holder.statusText.text = when (category) {
            MatchCategory.UPCOMING -> ""
            MatchCategory.LIVE ->
                context.getString(R.string.match_live)

            MatchCategory.COMPLETED -> {
                // the winner id must match one of the teams on this card.
                val winnerName = when (match.winnerTeamId) {
                    match.teamOneId -> match.teamOneName
                    match.teamTwoId -> match.teamTwoName
                    else -> null
                }

                // some finished matches may not have a usable winner yet.
                if (winnerName == null) {
                    context.getString(R.string.result_unavailable)
                } else {
                    context.getString(R.string.completed_winner, winnerName)
                }
            }
        }

        // predictions are only allowed from the upcoming tab.
        val canPredict = category == MatchCategory.UPCOMING

        if (canPredict) {
            // describes the button-like card for accessibility.
            holder.itemView.contentDescription = context.getString(
                R.string.open_prediction_for_match,
                match.teamOneName,
                match.teamTwoName
            )

            // sends the selected match back to the activity.
            holder.itemView.setOnClickListener {
                onMatchClick(match)
            }
        } else {
            // removes the click action from live and completed cards.
            holder.itemView.contentDescription = null
            holder.itemView.setOnClickListener(null)
        }

        // also removes tap and keyboard focus from cards that cannot be predicted.
        holder.itemView.isClickable = canPredict
        holder.itemView.isFocusable = canPredict
    }

    // tells the list how many match cards it has.
    override fun getItemCount(): Int = matches.size
}