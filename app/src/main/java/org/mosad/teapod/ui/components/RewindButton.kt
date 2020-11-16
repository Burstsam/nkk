package org.mosad.teapod.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.button_rewind.view.*
import org.mosad.teapod.R

class RewindButton(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {

    private val animationDuration: Long = 800

    init {
        inflate(context, R.layout.button_rewind, this)
    }

    fun setOnButtonClickListener(func: RewindButton.() -> Unit) {
        imageButton.setOnClickListener {
            func()
            runOnClickAnimation()
        }
    }

    fun runOnClickAnimation() {
        // run button animation
        ObjectAnimator.ofFloat(imageButton, View.ROTATION, 0f, -50f).apply {
            duration = animationDuration / 4
            repeatCount = 1
            repeatMode = ObjectAnimator.REVERSE
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    imageButton.isEnabled = false // disable button
                    imageButton.setImageResource(R.drawable.ic_baseline_rewind_24)
                }
                override fun onAnimationEnd(animation: Animator?) {
                    imageButton.isEnabled = true // enable button
                }
            })
            start()
        }

        // run lbl animation
        textView.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(textView, View.TRANSLATION_X, -35f).apply {
            duration = animationDuration
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    imageButton.isEnabled = true // enable button
                    imageButton.setImageResource(R.drawable.ic_baseline_rewind_10_24)

                    textView.visibility = View.GONE
                    textView.animate().translationX(0f)
                }
            })
            start()
        }

    }

}