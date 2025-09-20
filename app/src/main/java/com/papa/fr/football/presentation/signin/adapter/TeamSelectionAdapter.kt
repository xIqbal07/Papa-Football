package com.papa.fr.football.presentation.signin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papa.fr.football.databinding.ItemFavoriteTeamBinding
import com.papa.fr.football.presentation.signin.TeamSelectionUiModel

class TeamSelectionAdapter(
    private val onSelectionChanged: (teamId: Int, isSelected: Boolean) -> Unit,
) : ListAdapter<TeamSelectionUiModel, TeamSelectionAdapter.TeamSelectionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamSelectionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFavoriteTeamBinding.inflate(inflater, parent, false)
        return TeamSelectionViewHolder(binding, onSelectionChanged)
    }

    override fun onBindViewHolder(holder: TeamSelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TeamSelectionViewHolder(
        private val binding: ItemFavoriteTeamBinding,
        private val onSelectionChanged: (teamId: Int, isSelected: Boolean) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TeamSelectionUiModel) {
            binding.tvTeamName.text = item.name
            if (item.logoRes != null) {
                binding.ivLogo.setImageResource(item.logoRes)
            } else {
                binding.ivLogo.setImageDrawable(null)
            }
            binding.cbTeam.setOnCheckedChangeListener(null)
            binding.cbTeam.isChecked = item.isSelected
            binding.cbTeam.setOnCheckedChangeListener { _, isChecked ->
                onSelectionChanged(item.id, isChecked)
            }
            binding.cbTeam.isVisible = true
            binding.root.setOnClickListener {
                val newState = !binding.cbTeam.isChecked
                binding.cbTeam.isChecked = newState
            }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TeamSelectionUiModel>() {
            override fun areItemsTheSame(
                oldItem: TeamSelectionUiModel,
                newItem: TeamSelectionUiModel,
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: TeamSelectionUiModel,
                newItem: TeamSelectionUiModel,
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
