package com.rentify.app.core.ui.extension

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun View.setOnSingleClickListener(intervalMs: Long = 600L, onClick: (View) -> Unit) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0L

        override fun onClick(v: View) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= intervalMs) {
                lastClickTime = currentTime
                onClick(v)
            }
        }
    })
}

fun Fragment.hideKeyboard() {
    val currentView = activity?.currentFocus ?: view ?: return
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(currentView.windowToken, 0)
}

fun Activity.hideKeyboard() {
    val currentView = currentFocus ?: window.decorView
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(currentView.windowToken, 0)
}
