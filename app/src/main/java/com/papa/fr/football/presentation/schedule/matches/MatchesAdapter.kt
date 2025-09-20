package com.papa.fr.football.presentation.schedule.matches

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
            iltHome.setFavoriteTeamIndicator(item.isHomeTeamFavorite)
            iltHome.setLogoBase64(item.homeLogoBase64)
            iltAway.setTitle(item.awayTeamName)
            iltAway.setFavoriteTeamIndicator(item.isAwayTeamFavorite)
            iltAway.setLogoBase64(item.awayLogoBase64)
            tvHomeStartTime.text = item.startDateLabel
            tvAwayStartTime.text = item.startTimeLabel
        }
    }

    private class LiveMatchViewHolder(
        private val binding: ItemMatchLiveBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MatchUiModel.Live) = with(binding) {
            iltHome.setTitle(item.homeTeamName)
            iltHome.setFavoriteTeamIndicator(item.isHomeTeamFavorite)
            iltHome.setLogoBase64(item.homeLogoBase64)
            iltAway.setTitle(item.awayTeamName)
            iltAway.setFavoriteTeamIndicator(item.isAwayTeamFavorite)
            iltAway.setLogoBase64(item.awayLogoBase64)
            tvHomeStatus.text = item.statusLabel
            tvScore.text = item.scoreLabel
        }
    }

    private class PastMatchViewHolder(
        private val binding: ItemMatchPastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MatchUiModel.Past) = with(binding) {
            iltHome.setTitle(item.homeTeamName)
            iltHome.setFavoriteTeamIndicator(item.isHomeTeamFavorite)
            iltHome.setLogoBase64(item.homeLogoBase64)
            iltAway.setTitle(item.awayTeamName)
            iltAway.setFavoriteTeamIndicator(item.isAwayTeamFavorite)
            iltAway.setLogoBase64(item.awayLogoBase64)
            tvStartTime.text = item.startDateLabel
            tvScore.text = item.scoreLabel
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
