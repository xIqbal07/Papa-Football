package com.papa.fr.football.presentation.signin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papa.fr.football.databinding.ItemLeagueAddTeamsBinding
import com.papa.fr.football.presentation.signin.LeagueUiModel

class LeagueSelectionAdapter(
    private val onLeagueSelected: (LeagueUiModel) -> Unit,
) : ListAdapter<LeagueUiModel, LeagueSelectionAdapter.LeagueViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLeagueAddTeamsBinding.inflate(inflater, parent, false)
        return LeagueViewHolder(binding, onLeagueSelected)
    }

    override fun onBindViewHolder(holder: LeagueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LeagueViewHolder(
        private val binding: ItemLeagueAddTeamsBinding,
        private val onLeagueSelected: (LeagueUiModel) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LeagueUiModel) {
            binding.itlLeague.setTitle(item.name)
            item.iconRes?.let(binding.itlLeague::setLogo)
            binding.root.setOnClickListener { onLeagueSelected(item) }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LeagueUiModel>() {
            override fun areItemsTheSame(oldItem: LeagueUiModel, newItem: LeagueUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LeagueUiModel, newItem: LeagueUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
