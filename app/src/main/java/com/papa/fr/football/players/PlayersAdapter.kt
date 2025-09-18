package com.papa.fr.football.players

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papa.fr.football.data.Player
import com.papa.fr.football.databinding.ItemPlayerBinding

class PlayersAdapter : ListAdapter<Player, PlayersAdapter.PlayerViewHolder>(PlayerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class PlayerViewHolder(private val binding: ItemPlayerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: Player, adapterPosition: Int) {
            binding.tvPlayerName.text = binding.root.context.getString(
                com.papa.fr.football.R.string.player_name_format,
                player.number,
                player.name
            )
            binding.tvPlayerAge.text = binding.root.context.getString(
                com.papa.fr.football.R.string.player_age_format,
                player.age
            )
            binding.tvGoalsValue.text = player.goals.toString()
            binding.tvAssistsValue.text = player.assists.toString()
            binding.tvYellowCardsValue.text = player.yellowCards.toString()
            binding.tvPlayerPosition.text = binding.root.context.getString(
                com.papa.fr.football.R.string.player_order_format,
                adapterPosition + 1
            )
        }
    }

    private object PlayerDiffCallback : DiffUtil.ItemCallback<Player>() {
        override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean = oldItem == newItem
    }
}
