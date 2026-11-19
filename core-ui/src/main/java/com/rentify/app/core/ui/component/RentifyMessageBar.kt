package com.rentify.app.core.ui.component

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.ImageViewCompat
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.databinding.ViewRentifyMessageBarBinding
import java.lang.ref.WeakReference
import kotlin.math.max

class RentifyMessageBar private constructor(
    private val host: ViewGroup,
    private val message: String,
    private val type: Type,
    private val duration: Duration,
    private val actionText: String?,
    private val onAction: (() -> Unit)?
) {

    enum class Type(
        @DrawableRes val background: Int,
        @DrawableRes val icon: Int,
        @ColorRes val accent: Int,
        @ColorRes val text: Int
    ) {
        SUCCESS(
            R.drawable.bg_message_success,
            R.drawable.ic_check_circle,
            R.color.rentify_success,
            R.color.rentify_message_success_text
        ),
        ERROR(
            R.drawable.bg_message_error,
            R.drawable.ic_message_error,
            R.color.rentify_error,
            R.color.rentify_message_error_text
        ),
        INFO(
            R.drawable.bg_message_info,
            R.drawable.ic_message_info,
            R.color.rentify_message_info_accent,
            R.color.rentify_message_info_text
        )
    }

    enum class Duration(val millis: Long) {
        SHORT(2000L),
        LONG(4000L),
        INDEFINITE(-1L)
    }

    private var binding: ViewRentifyMessageBarBinding? = null
    private var dismissing = false
    private val autoDismiss = Runnable { dismiss() }

    fun show() {
        current?.get()?.removeNow()
        current = WeakReference(this)

        val bar = ViewRentifyMessageBarBinding.inflate(LayoutInflater.from(host.context), host, false)
        binding = bar
        bindStyle(bar)

        val gap = host.resources.getDimensionPixelSize(R.dimen.spacing_sm)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            marginStart = gap * 2
            marginEnd = gap * 2
            topMargin = gap + statusBarOverlap()
        }

        val root = bar.root
        root.alpha = 0f
        root.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        enableSwipeToDismiss(root)
        host.addView(root, params)
        root.doOnLayout {
            root.translationY = -hiddenOffset(root)
            root.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(ANIM_MS)
                .start()
            scheduleAutoDismiss(root)
        }
    }

    fun dismiss() {
        val root = binding?.root ?: return
        if (dismissing) return
        dismissing = true
        root.removeCallbacks(autoDismiss)
        root.animate()
            .translationY(-hiddenOffset(root))
            .alpha(0f)
            .setDuration(ANIM_MS)
            .withEndAction { removeNow() }
            .start()
    }

    private fun bindStyle(bar: ViewRentifyMessageBarBinding) {
        val context = host.context
        val accent = ContextCompat.getColor(context, type.accent)
        bar.root.setBackgroundResource(type.background)
        bar.messageIcon.setImageResource(type.icon)
        ImageViewCompat.setImageTintList(bar.messageIcon, ColorStateList.valueOf(accent))
        bar.messageText.text = message
        bar.messageText.setTextColor(ContextCompat.getColor(context, type.text))
        if (actionText != null) {
            bar.messageAction.text = actionText
            bar.messageAction.setTextColor(accent)
            bar.messageAction.visibility = View.VISIBLE
            bar.messageAction.setOnClickListener {
                onAction?.invoke()
                dismiss()
            }
        }
    }

    private fun scheduleAutoDismiss(root: View) {
        root.removeCallbacks(autoDismiss)
        if (duration.millis > 0) root.postDelayed(autoDismiss, duration.millis)
    }

    private fun statusBarOverlap(): Int {
        val insetTop = ViewCompat.getRootWindowInsets(host)
            ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            ?.top ?: 0
        val location = IntArray(2)
        host.getLocationInWindow(location)
        return max(0, insetTop - location[1])
    }

    private fun hiddenOffset(root: View): Float {
        val topMargin = (root.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        return (root.height + topMargin).toFloat()
    }

    private fun enableSwipeToDismiss(root: View) {
        var downY = 0f
        root.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    view.removeCallbacks(autoDismiss)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.translationY = (event.rawY - downY).coerceAtMost(0f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (view.translationY < -view.height / 3f) {
                        dismiss()
                    } else {
                        view.animate().translationY(0f).setDuration(ANIM_MS).start()
                        scheduleAutoDismiss(view)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun removeNow() {
        binding?.root?.let {
            it.removeCallbacks(autoDismiss)
            it.animate().cancel()
            host.removeView(it)
        }
        binding = null
        if (current?.get() === this) current = null
    }

    companion object {
        private const val ANIM_MS = 250L
        private var current: WeakReference<RentifyMessageBar>? = null

        fun make(
            host: ViewGroup,
            message: String,
            type: Type = Type.INFO,
            duration: Duration? = null,
            actionText: String? = null,
            onAction: (() -> Unit)? = null
        ): RentifyMessageBar {
            val resolved = duration ?: if (actionText != null) Duration.LONG else Duration.SHORT
            return RentifyMessageBar(host, message, type, resolved, actionText, onAction)
        }
    }
}
