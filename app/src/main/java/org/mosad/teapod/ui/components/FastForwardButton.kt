package org.mosad.teapod.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import org.mosad.teapod.R
import org.mosad.teapod.databinding.ButtonFastForwardBinding

class FastForwardButton(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs) {

    private val binding = ButtonFastForwardBinding.inflate(LayoutInflater.from(context))
    private val animationDuration: Long = 800
    private val buttonAnimation: ObjectAnimator
    private val labelAnimation: ObjectAnimator

    var onAnimationEndCallback: (() -> Unit)? = null

    init {
        addView(binding.root)

        buttonAnimation = ObjectAnimator.ofFloat(binding.imageButton, View.ROTATION, 0f, 50f).apply {
            duration = animationDuration / 4
            repeatCount = 1
            repeatMode = ObjectAnimator.REVERSE
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    binding.imageButton.isEnabled = false // disable button
                    binding.imageButton.setBackgroundResource(R.drawable.ic_baseline_forward_24)
                }
            })
        }

        labelAnimation = ObjectAnimator.ofFloat(binding.textView, View.TRANSLATION_X, 35f).apply {
            duration = animationDuration
            addListener(object : AnimatorListenerAdapter() {
                // the label animation takes longer then the button animation, reset stuff in here
                override fun onAnimationEnd(animation: Animator?) {
                    binding.imageButton.isEnabled = true // enable button
                    binding.imageButton.setBackgroundResource(R.drawable.ic_baseline_forward_10_24)

                    binding.textView.visibility = View.GONE
                    binding.textView.animate().translationX(0f)

                    onAnimationEndCallback?.invoke()
                }
            })
        }
    }

    fun setOnButtonClickListener(func: FastForwardButton.() -> Unit) {
        binding.imageButton.setOnClickListener {
            func()
        }
    }

    fun runOnClickAnimation() {
        // run button animation
        buttonAnimation.start()

        // run lbl animation
        binding.textView.visibility = View.VISIBLE
        labelAnimation.start()
    }

}