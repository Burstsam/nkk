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
import androidx.core.view.children
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

    private var currentStreamUrl = model?.currentStreamUrl ?: ""

    init {
        model?.let {
            model.currentEpisode.streams.forEach { stream ->
                addLanguage(stream.language.displayName, stream.url == currentStreamUrl) {
                    currentStreamUrl = stream.url
                    updateSelectedLanguage(it as TextView)
                }
            }
        }

        binding.buttonCloseLanguageSettings.setOnClickListener { close() }
        binding.buttonCancel.setOnClickListener { close() }
        binding.buttonSelect.setOnClickListener {
            model?.changeLanguage(currentStreamUrl)
            close()
        }
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

    private fun updateSelectedLanguage(selected: TextView) {
        // rest all tf to not selected style
        binding.linearLanguages.children.forEach { child ->
            if (child is TextView) {
                child.apply {
                    setTextColor(context.resources.getColor(R.color.textPrimaryDark, context.theme))
                    setTypeface(null, Typeface.NORMAL)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                    setPadding(75, 0, 0, 0)
                }
            }

        }

        // set selected to selected style
        selected.apply {
            setTextColor(context.resources.getColor(R.color.exo_white, context.theme))
            setTypeface(null, Typeface.BOLD)
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_24, 0, 0, 0)
            setPadding(0, 0, 0, 0)
            compoundDrawablesRelative.getOrNull(0)?.setTint(Color.WHITE)
            compoundDrawablePadding = 12
        }
    }

    private fun close() {
        (this.parent as ViewGroup).removeView(this)
        onViewRemovedAction?.invoke()
    }

}