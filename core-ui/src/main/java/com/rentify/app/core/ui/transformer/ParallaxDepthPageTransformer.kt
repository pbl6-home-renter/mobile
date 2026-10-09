package com.rentify.app.core.ui.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Hiệu ứng chuyển trang 3D Parallax & Depth cho ViewPager2.
 * Dùng chung được cho Onboarding hoặc bất kỳ ViewPager2 nào trong ứng dụng.
 */
class ParallaxDepthPageTransformer(
    private val minScale: Float = 0.85f,
    private val minAlpha: Float = 0.4f,
    private val parallaxFactor: Float = 0.4f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width

        when {
            position < -1f -> {
                page.alpha = 0f
            }
            position <= 0f -> {
                page.alpha = 1f + position
                page.translationX = 0f
                page.scaleX = 1f
                page.scaleY = 1f

                val animationView = findInnerAnimationView(page)
                animationView?.translationX = position * (pageWidth * parallaxFactor)
            }
            position <= 1f -> {
                val factor = 1f - abs(position)
                page.alpha = minAlpha + factor * (1f - minAlpha)

                page.translationX = pageWidth * -position * 0.25f

                val scaleFactor = minScale + (1f - minScale) * factor
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

                val animationView = findInnerAnimationView(page)
                animationView?.translationX = position * (pageWidth * parallaxFactor)
            }
            else -> {
                page.alpha = 0f
            }
        }
    }

    private fun findInnerAnimationView(page: View): View? {
        val resId = page.resources.getIdentifier("animationView", "id", page.context.packageName)
        return if (resId != 0) page.findViewById(resId) else null
    }
}
