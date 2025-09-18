package com.papa.fr.football.common.dropdown

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.papa.fr.football.R
import com.papa.fr.football.databinding.ItemLeagueDropdownBinding
import androidx.core.content.withStyledAttributes

class LeagueDropdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
        ItemLeagueDropdownBinding.inflate(LayoutInflater.from(context), this, true)
    private val adapter = LeagueItemAdapter(context, LayoutInflater.from(context))

    private var onChanged: ((LeagueItem) -> Unit)? = null
    private var leagues: List<LeagueItem> = emptyList()

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.LeagueDropdownView) {
                binding.tvTitle.text = getString(R.styleable.LeagueDropdownView_label)
                binding.til.isStartIconVisible = getBoolean(R.styleable.LeagueDropdownView_is_show_start_icon, false)
            }
        }

        binding.actv.setAdapter(adapter)
        binding.actv.setOnItemClickListener { _, _, position, _ ->
            val value = adapter.getItem(position) ?: return@setOnItemClickListener
            onChanged?.invoke(value)
        }

        // Optional: allow clicking the layout to open menu
        binding.til.setEndIconOnClickListener { binding.actv.showDropDown() }
        binding.actv.setOnClickListener { binding.actv.showDropDown() }
    }


    fun setHint(hint: CharSequence) {
        binding.til.hint = hint
    }

    fun setData(items: List<LeagueItem>) {
        leagues = items
        adapter.replaceAll(items)
        // If current selection no longer exists, clear
        val selected = getSelected()
        if (selected == null && items.isNotEmpty()) {
            setSelected(items.first())
        }
    }

    fun setSelected(target: LeagueItem?) {
        if (target == null) {
            binding.actv.setText("", false)
            return
        }
        val index = leagues.indexOfFirst { it.id == target.id }
        if (index >= 0) {
            // setText(CharSequence, boolean) avoids filtering
            binding.actv.setText(leagues[index].name, false)
        }
    }

    fun getSelected(): LeagueItem? {
        val name = binding.actv.text?.toString() ?: return null
        return leagues.firstOrNull { it.name == name }
    }

    fun setOnChangedListener(listener: (LeagueItem) -> Unit) {
        onChanged = listener
    }

    /** Save/restore selection across rotations **/
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return Bundle().apply {
            putParcelable("super", superState)
            putString("selectedId", getSelected()?.id)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as? Bundle
        super.onRestoreInstanceState(bundle?.getParcelable("super"))
        val id = bundle?.getString("selectedId") ?: return
        leagues.firstOrNull { it.id == id }?.let { setSelected(it) }
    }
}