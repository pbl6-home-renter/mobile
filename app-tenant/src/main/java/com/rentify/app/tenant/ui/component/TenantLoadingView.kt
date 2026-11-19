package com.rentify.app.tenant.ui.component

import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.rentify.app.tenant.R

class TenantLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var attachedHost: ViewGroup? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(0, 0)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        syncOverlayState()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        syncOverlayState()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncOverlayState()
    }

    override fun onDetachedFromWindow() {
        unregisterFromHost()
        super.onDetachedFromWindow()
    }

    private fun syncOverlayState() {
        val shouldShow = isAttachedToWindow && visibility == VISIBLE && isShown
        if (shouldShow) {
            val host = findWindowHost() ?: return
            if (attachedHost != null && attachedHost !== host) {
                OverlayController.getOrCreate(attachedHost!!).setSourceActive(this, false)
            }
            attachedHost = host
            OverlayController.getOrCreate(host).setSourceActive(this, true)
        } else {
            unregisterFromHost()
        }
    }

    private fun unregisterFromHost() {
        val host = attachedHost ?: findWindowHost()
        if (host != null) {
            OverlayController.get(host)?.setSourceActive(this, false)
        }
        attachedHost = null
    }

    private fun findWindowHost(): ViewGroup? {
        val root = rootView ?: return null
        return root.findViewById<ViewGroup>(android.R.id.content) ?: (root as? ViewGroup)
    }

    private class OverlayController(private val host: ViewGroup) {

        private val activeSources = LinkedHashSet<TenantLoadingView>()
        private val overlayLayout: FrameLayout
        private val lottieView: LottieAnimationView
        private val density = host.resources.displayMetrics.density

        init {
            val sizePx = (320f * density + 0.5f).toInt()
            lottieView = LottieAnimationView(host.context).apply {
                setAnimation("loading_ring_ball.json")
                repeatCount = LottieDrawable.INFINITE
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
            }

            overlayLayout = FrameLayout(host.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Color.parseColor("#B3FFFFFF")
                    } else {
                        Color.parseColor("#E6FFFFFF")
                    }
                )
                isClickable = true
                isFocusable = true
                elevation = 100f * density
                visibility = GONE
                addView(lottieView)
            }
        }

        fun setSourceActive(source: TenantLoadingView, active: Boolean) {
            if (active) {
                activeSources.add(source)
            } else {
                activeSources.remove(source)
            }
             activeSources.removeAll { !it.isAttachedToWindow || it.visibility != VISIBLE || !it.isShown }

            if (activeSources.isNotEmpty()) {
                showOverlay()
            } else {
                hideOverlay()
            }
        }

        private fun showOverlay() {
            if (overlayLayout.parent == null) {
                host.addView(overlayLayout)
            } else {
                overlayLayout.bringToFront()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurPx = 10f * density
                val blurEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                for (i in 0 until host.childCount) {
                    val child = host.getChildAt(i)
                    if (child !== overlayLayout) {
                        child.setRenderEffect(blurEffect)
                    }
                }
            }

            overlayLayout.visibility = VISIBLE
            if (!lottieView.isAnimating) {
                lottieView.playAnimation()
            }
        }

        private fun hideOverlay() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                for (i in 0 until host.childCount) {
                    val child = host.getChildAt(i)
                    if (child !== overlayLayout) {
                        child.setRenderEffect(null)
                    }
                }
            }

            if (lottieView.isAnimating) {
                lottieView.cancelAnimation()
            }
            overlayLayout.visibility = GONE
        }

        companion object {
            fun get(host: ViewGroup): OverlayController? {
                return host.getTag(R.id.pbLoading) as? OverlayController
            }

            fun getOrCreate(host: ViewGroup): OverlayController {
                val existing = get(host)
                if (existing != null) return existing
                return OverlayController(host).also {
                    host.setTag(R.id.pbLoading, it)
                }
            }
        }
    }
}
