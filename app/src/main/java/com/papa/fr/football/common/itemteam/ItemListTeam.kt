package com.papa.fr.football.common.itemteam

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
                val isEdit = getBoolean(R.styleable.ItemListTeam_isEdit, false)
                binding.tvTeamName.text = getString(R.styleable.ItemListTeam_title)
                binding.ivLogo.setImageDrawable(getDrawable(R.styleable.ItemListTeam_logo))
                binding.ivIndicatorActive.isVisible =
                    getBoolean(R.styleable.ItemListTeam_active, false)
                binding.groupEdit.isVisible = isEdit
                if (isEdit) {
                    binding.ivEndIcon.setImageDrawable(getDrawable(R.styleable.ItemListTeam_endIconLogo))
                    binding.ivIndicatorActive.isVisible = false
                }
            }
        }
    }

    fun setTitle(title: String) {
        binding.tvTeamName.text = title
    }

    fun setLogo(@DrawableRes logo: Int) {
        binding.ivLogo.setImageDrawable(ContextCompat.getDrawable(context, logo))
    }

    fun setLogoBase64(base64: String?) {
        if (base64.isNullOrBlank()) {
            binding.ivLogo.setImageDrawable(null)
            return
        }

        val decodedBytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
        if (decodedBytes == null) {
            binding.ivLogo.setImageDrawable(null)
            return
        }

        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        if (bitmap != null) {
            binding.ivLogo.setImageBitmap(bitmap)
        } else {
            binding.ivLogo.setImageDrawable(null)
        }
    }

    fun setIndicatorActive(isActive: Boolean) {
        binding.ivIndicatorActive.isVisible = isActive
    }
}
