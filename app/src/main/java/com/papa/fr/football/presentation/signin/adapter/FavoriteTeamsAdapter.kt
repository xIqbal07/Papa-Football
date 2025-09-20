package com.papa.fr.football.presentation.signin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.papa.fr.football.common.extensions.setImageBase64
import com.papa.fr.football.databinding.ItemTeamBinding
import com.papa.fr.football.presentation.signin.FavoriteTeamUiModel

class FavoriteTeamsAdapter :
    ListAdapter<FavoriteTeamUiModel, FavoriteTeamsAdapter.FavoriteTeamViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteTeamViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTeamBinding.inflate(inflater, parent, false)
        return FavoriteTeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteTeamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FavoriteTeamViewHolder(
        private val binding: ItemTeamBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoriteTeamUiModel) {
            binding.tvTeamName.text = item.name
            binding.ivLogo.setImageBase64(item.logoBase64)
            binding.ivIndicatorActive.isVisible = true
            binding.groupEdit.isVisible = false
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FavoriteTeamUiModel>() {
            override fun areItemsTheSame(
                oldItem: FavoriteTeamUiModel,
                newItem: FavoriteTeamUiModel,
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: FavoriteTeamUiModel,
                newItem: FavoriteTeamUiModel,
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
