package org.mosad.teapod.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * hide the status and navigation bar
 */
fun Activity.hideBars() {
    window.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setDecorFitsSystemWindows(false)
            insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE
            }
        } else {
            @Suppress("deprecation")
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
}

fun Activity.isInPiPMode(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        isInPictureInPictureMode
    } else {
        false // pip mode not supported
    }
}

/**
 * Bring up launcher task to front
 */
fun Activity.navToLauncherTask() {
    val activityManager = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
    activityManager.appTasks.forEach { task ->
        val baseIntent = task.taskInfo.baseIntent
        val categories = baseIntent.categories
        if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
            task.moveToFront()
            return
        }
    }
}

