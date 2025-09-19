package com.papa.fr.football.common.dropdown

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.papa.fr.football.R
import com.papa.fr.football.databinding.ItemLeagueDropdownBinding
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toDrawable

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
    private var placeholderText: String? = null

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.LeagueDropdownView) {
                binding.tvTitle.text = getString(R.styleable.LeagueDropdownView_label)
                binding.til.isStartIconVisible =
                    getBoolean(R.styleable.LeagueDropdownView_is_show_start_icon, false)
            }
        }

        binding.actv.setAdapter(adapter)
        binding.actv.setDropDownBackgroundDrawable(
            ContextCompat.getColor(context, R.color.matches_card_background).toDrawable()
        )
        binding.actv.setOnItemClickListener { _, _, position, _ ->
            val value = adapter.getItem(position) ?: return@setOnItemClickListener
            setSelected(value)
            onChanged?.invoke(value)
        }

        // Optional: allow clicking the layout to open menu
        binding.til.setEndIconOnClickListener { binding.actv.showDropDown() }
        binding.actv.setOnClickListener { binding.actv.showDropDown() }

        // Ensure initial enabled state cascades to children
        setEnabled(isEnabled)
    }


    fun setHint(hint: CharSequence) {
        binding.til.hint = hint
    }

    fun setData(items: List<LeagueItem>) {
        val previouslySelectedId = getSelected()?.id

        leagues = items
        adapter.replaceAll(items)

        when {
            previouslySelectedId != null -> {
                leagues.firstOrNull { it.id == previouslySelectedId }?.let { setSelected(it) }
            }

            placeholderText != null -> {
                binding.actv.setText(placeholderText, false)
            }

            items.isNotEmpty() -> {
                setSelected(items.first())
            }

            else -> {
                binding.actv.setText("", false)
            }
        }
    }

    fun setSelected(target: LeagueItem?) {
        if (target == null) {
            binding.actv.setText("", false)
            return
        }
        placeholderText = null
        val index = leagues.indexOfFirst { it.id == target.id }
        if (index >= 0) {
            // setText(CharSequence, boolean) avoids filtering
            binding.actv.setText(leagues[index].name, false)
        }
        binding.til.startIconDrawable = target.iconRes?.let { ContextCompat.getDrawable(context, it) }
    }

    fun setPlaceholder(text: CharSequence?) {
        placeholderText = text?.toString()
        if (text == null) {
            if (getSelected() == null) {
                binding.actv.setText("", false)
            }
            return
        }

        if (getSelected() == null) {
            binding.actv.setText(placeholderText, false)
        }
    }

    fun getSelected(): LeagueItem? {
        val name = binding.actv.text?.toString() ?: return null
        if (placeholderText != null && name == placeholderText) {
            return null
        }
        return leagues.firstOrNull { it.name == name }
    }

    fun setOnChangedListener(listener: (LeagueItem) -> Unit) {
        onChanged = listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.til.isEnabled = enabled
        binding.actv.isEnabled = enabled
    }


    /** Save/restore selection across rotations **/
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return Bundle().apply {
            putParcelable("super", superState)
            getSelected()?.id?.let { putInt("selectedId", it) }
            putString("placeholder", placeholderText)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as? Bundle
        super.onRestoreInstanceState(bundle?.getParcelable("super"))
        placeholderText = bundle?.getString("placeholder")
        val hasSelectedId = bundle?.containsKey("selectedId") == true
        val id = bundle?.getInt("selectedId")
        when {
            hasSelectedId && id != null -> {
                leagues.firstOrNull { it.id == id }?.let { setSelected(it) }
            }

            placeholderText != null -> {
                binding.actv.setText(placeholderText, false)
            }
        }
    }
}
