package com.papa.fr.football.common.dropdown

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.papa.fr.football.databinding.ItemLeagueOptionBinding

class LeagueItemAdapter(
    context: Context,
    private val inflater: LayoutInflater,
    leagues: MutableList<LeagueItem> = mutableListOf()
) : ArrayAdapter<LeagueItem>(context, 0, leagues) {

    fun replaceAll(newData: List<LeagueItem>) {
        clear()
        addAll(newData)
        notifyDataSetChanged()
    }

    private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemLeagueOptionBinding
        val view: View

        if (convertView == null) {
            binding = ItemLeagueOptionBinding.inflate(inflater, parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as ItemLeagueOptionBinding
        }

        getItem(position)?.let { league ->
            league.iconRes?.let { binding.icon.setImageResource(it) }
            binding.icon.isVisible = league.iconRes != null
            binding.title.text = league.name
        }

        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)
}