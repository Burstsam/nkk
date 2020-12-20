package org.mosad.teapod.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.mosad.teapod.R
import org.mosad.teapod.databinding.PlayerLanguageSettingsBinding
import org.mosad.teapod.player.PlayerViewModel

class LanguageSettingsPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    model: PlayerViewModel? = null
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = PlayerLanguageSettingsBinding.inflate(LayoutInflater.from(context), this, true)
    var onViewRemovedAction: (() -> Unit)? = null // TODO find a better solution for this

    init {
        addLanguage("primary", true) { model?.changeLanguage(0) }
        addLanguage("secondary", false ) { model?.changeLanguage(1) }

        binding.buttonCloseLanguageSettings.setOnClickListener { close() }
        binding.buttonCancel.setOnClickListener { close() }
    }

    private fun addLanguage(str: String, isSelected: Boolean, onClick: OnClickListener) {
        val text = TextView(context).apply {
            height = 96
            gravity = Gravity.CENTER_VERTICAL
            text = str
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

            if (isSelected) {
                setTextColor(context.resources.getColor(R.color.exo_white, context.theme))
                setTypeface(null, Typeface.BOLD)
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_24, 0, 0, 0)
                compoundDrawablesRelative.getOrNull(0)?.setTint(Color.WHITE)
                compoundDrawablePadding = 12
            } else {
                setTextColor(context.resources.getColor(R.color.textPrimaryDark, context.theme))
                setPadding(75, 0, 0, 0)
            }

            setOnClickListener(onClick)
        }

        binding.linearLanguages.addView(text)
    }

    private fun close() {
        (this.parent as ViewGroup).removeView(this)
        onViewRemovedAction?.invoke()
    }

}