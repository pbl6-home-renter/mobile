package com.rentify.app.core.ui.extension

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun Activity.enableRentifyEdgeToEdge() {
    if (this is ComponentActivity) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
    }
}

fun View.applySystemBarsPadding(top: Boolean = true, bottom: Boolean = true) {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val newTop = if (top) initialPaddingTop + systemBars.top else initialPaddingTop
        val newBottom = if (bottom) initialPaddingBottom + systemBars.bottom else initialPaddingBottom
        v.setPadding(initialPaddingLeft, newTop, initialPaddingRight, newBottom)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.applyImePadding() {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.setPadding(
            initialPaddingLeft,
            initialPaddingTop,
            initialPaddingRight,
            initialPaddingBottom + imeInsets.bottom
        )
        insets
    }
    requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })
    }
}
