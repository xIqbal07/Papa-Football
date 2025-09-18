package com.papa.fr.football.matches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papa.fr.football.data.Team
import com.papa.fr.football.databinding.ItemTeamBinding

class TeamsAdapter(
    private val onTeamClicked: (Team) -> Unit
) : ListAdapter<Team, TeamsAdapter.TeamViewHolder>(TeamDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemTeamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TeamViewHolder(private val binding: ItemTeamBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(team: Team) {
            binding.tvTeamName.text = team.name
            binding.root.setOnClickListener { onTeamClicked(team) }
        }
    }

    private object TeamDiffCallback : DiffUtil.ItemCallback<Team>() {
        override fun areItemsTheSame(oldItem: Team, newItem: Team): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Team, newItem: Team): Boolean = oldItem == newItem
    }
}
