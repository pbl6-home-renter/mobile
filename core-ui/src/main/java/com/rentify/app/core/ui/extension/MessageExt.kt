package com.rentify.app.core.ui.extension

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.rentify.app.core.ui.component.RentifyMessageBar
import com.rentify.app.core.ui.component.RentifyMessageBar.Duration
import com.rentify.app.core.ui.component.RentifyMessageBar.Type

fun Activity.showMessage(
    message: String,
    type: Type = Type.INFO,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) {
    if (isFinishing || isDestroyed) return
    val host = findViewById<ViewGroup>(android.R.id.content) ?: return
    RentifyMessageBar.make(host, message, type, duration, actionText, onAction).show()
}

fun Activity.showSuccess(
    message: String,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) = showMessage(message, Type.SUCCESS, actionText, duration, onAction)

fun Activity.showError(
    message: String,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) = showMessage(message, Type.ERROR, actionText, duration, onAction)

fun Dialog.showMessage(
    message: String,
    type: Type = Type.INFO,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) {
    val host = window?.decorView?.findViewById<ViewGroup>(android.R.id.content) ?: return
    RentifyMessageBar.make(host, message, type, duration, actionText, onAction).show()
}

fun Dialog.showSuccess(
    message: String,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) = showMessage(message, Type.SUCCESS, actionText, duration, onAction)

fun Dialog.showError(
    message: String,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) = showMessage(message, Type.ERROR, actionText, duration, onAction)

fun Fragment.showMessage(
    message: String,
    type: Type = Type.INFO,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) {
    if (!isAdded || view == null) return
    val activeDialog = (this as? DialogFragment)?.dialog
    if (activeDialog != null) {
        activeDialog.showMessage(message, type, actionText, duration, onAction)
        return
    }
    activity?.showMessage(message, type, actionText, duration, onAction)
}

fun Fragment.showSuccess(
    message: String,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) = showMessage(message, Type.SUCCESS, actionText, duration, onAction)

fun Fragment.showError(
    message: String,
    actionText: String? = null,
    duration: Duration? = null,
    onAction: (() -> Unit)? = null
) = showMessage(message, Type.ERROR, actionText, duration, onAction)
