package com.papa.fr.football.common.itemteam

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.papa.fr.football.R
import com.papa.fr.football.databinding.ItemTeamBinding

class ItemListTeam @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding =
        ItemTeamBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.ItemListTeam) {
                binding.tvTeamName.text = getString(R.styleable.ItemListTeam_title)
                binding.ivLogo.setImageDrawable(getDrawable(R.styleable.ItemListTeam_logo))
                binding.ivIndicatorActive.isVisible =
                    getBoolean(R.styleable.ItemListTeam_active, false)
            }
        }
    }
}
