package com.papa.fr.football.matches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papa.fr.football.databinding.ItemMatchFutureBinding
import com.papa.fr.football.databinding.ItemMatchLiveBinding
import com.papa.fr.football.databinding.ItemMatchPastBinding

class MatchesAdapter : ListAdapter<MatchUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is MatchUiModel.Future -> ViewType.FUTURE.ordinal
            is MatchUiModel.Live -> ViewType.LIVE.ordinal
            is MatchUiModel.Past -> ViewType.PAST.ordinal
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (ViewType.entries[viewType]) {
            ViewType.FUTURE -> FutureMatchViewHolder(
                ItemMatchFutureBinding.inflate(inflater, parent, false)
            )

            ViewType.LIVE -> LiveMatchViewHolder(
                ItemMatchLiveBinding.inflate(inflater, parent, false)
            )

            ViewType.PAST -> PastMatchViewHolder(
                ItemMatchPastBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FutureMatchViewHolder -> holder.bind(getItem(position) as MatchUiModel.Future)
            is LiveMatchViewHolder -> holder.bind(getItem(position) as MatchUiModel.Live)
            is PastMatchViewHolder -> holder.bind(getItem(position) as MatchUiModel.Past)
        }
    }

    fun submitMatches(matches: List<MatchUiModel>, onCommitted: (() -> Unit)? = null) {
        if (onCommitted == null) {
            submitList(matches)
        } else {
            submitList(matches, Runnable { onCommitted() })
        }
    }

    private enum class ViewType { FUTURE, LIVE, PAST }

    private class FutureMatchViewHolder(
        private val binding: ItemMatchFutureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MatchUiModel.Future) = with(binding) {
            iltHome.setTitle(item.homeTeamName)
            iltHome.setIndicatorActive(item.isToday)
            iltHome.setLogoBase64(item.homeLogoBase64)
            iltAway.setTitle(item.awayTeamName)
            iltAway.setIndicatorActive(false)
            iltAway.setLogoBase64(item.awayLogoBase64)
            tvHomeStartTime.text = item.startDateLabel
            tvAwayStartTime.text = item.startTimeLabel
        }
    }

    private class LiveMatchViewHolder(
        private val binding: ItemMatchLiveBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MatchUiModel.Live) = with(binding) {
//            tvLiveMinute.text = item.contextText
//            tvLiveHome.text = item.homeTeam
//            tvLiveAway.text = item.awayTeam
//            tvLiveScore.text = item.score
//            tvLiveTime.text = item.elapsed
//            tvLiveStatus.text = item.status
        }
    }

    private class PastMatchViewHolder(
        private val binding: ItemMatchPastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MatchUiModel.Past) = with(binding) {
//            tvPastDate.text = item.date
//            tvPastHome.text = item.homeTeam
//            tvPastAway.text = item.awayTeam
//            tvPastScore.text = item.score
//            tvPastResult.text = item.resultLabel
//            tvPastStatus.text = item.status
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MatchUiModel>() {
            override fun areItemsTheSame(oldItem: MatchUiModel, newItem: MatchUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MatchUiModel, newItem: MatchUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

sealed class MatchUiModel(open val id: String) {
    data class Future(
        override val id: String,
        val homeTeamId: Int,
        val homeTeamName: String,
        val awayTeamId: Int,
        val awayTeamName: String,
        val startTimestamp: Long,
        val startDateLabel: String,
        val startTimeLabel: String,
        val isToday: Boolean,
        val homeLogoBase64: String,
        val awayLogoBase64: String,
        val odds: Odds? = null,
    ) : MatchUiModel(id)

    data class Live(
        override val id: String,
        val homeTeam: String,
        val awayTeam: String,
        val score: String,
        val contextText: String,
        val elapsed: String,
        val status: String,
    ) : MatchUiModel(id)

    data class Past(
        override val id: String,
        val homeTeam: String,
        val awayTeam: String,
        val score: String,
        val date: String,
        val resultLabel: String,
        val status: String,
    ) : MatchUiModel(id)

    data class Odds(
        val home: String,
        val draw: String,
        val away: String,
    )
}
