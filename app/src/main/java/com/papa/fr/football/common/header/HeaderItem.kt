package com.papa.fr.football.common.header

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.papa.fr.football.databinding.ItemHeaderBinding

class HeaderItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding =
        ItemHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    fun setLastUpdated(lastUpdated: String) {
        binding.btnSchedule.text = lastUpdated
    }

    fun setOnClickListener(
        onResetListener: (() -> Unit),
        onProfileListener: (() -> Unit),
    ) {
        binding.apply {
            ivProfile.setOnClickListener { onProfileListener.invoke() }
            btnSchedule.setOnClickListener { onResetListener.invoke() }
        }
    }
}