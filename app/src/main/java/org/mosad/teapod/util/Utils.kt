package org.mosad.teapod.util

import android.widget.TextView

fun TextView.setDrawableTop(drawable: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0)
}
