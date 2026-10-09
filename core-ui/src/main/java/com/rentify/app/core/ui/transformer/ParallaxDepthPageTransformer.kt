package com.rentify.app.core.ui.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Hiệu ứng chuyển trang 3D Card Parallax & Depth nổi bật cho ViewPager2.
 * Kết hợp 3D RotationY, Scaling, Alpha Fade và Multi-layer Parallax cho Lottie Animation & Text.
 */
class ParallaxDepthPageTransformer(
    private val minScale: Float = 0.80f,
    private val minAlpha: Float = 0.3f,
    private val maxRotationY: Float = 15f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width

        page.pivotX = if (position < 0f) pageWidth.toFloat() else 0f
        page.pivotY = page.height * 0.5f

        when {
            position < -1f -> {
                page.alpha = 0f
            }
            position <= 0f -> {
                val factor = 1f + position
                page.alpha = minAlpha + factor * (1f - minAlpha)

                page.rotationY = position * maxRotationY

                val scaleFactor = minScale + (1f - minScale) * factor
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

                page.translationX = pageWidth * -position * 0.2f

                applyMultiLayerParallax(page, position, pageWidth)
            }
            position <= 1f -> {
                val factor = 1f - abs(position)
                page.alpha = minAlpha + factor * (1f - minAlpha)

                page.rotationY = position * maxRotationY

                val scaleFactor = minScale + (1f - minScale) * factor
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

                page.translationX = pageWidth * -position * 0.2f

                applyMultiLayerParallax(page, position, pageWidth)
            }
            else -> {
                page.alpha = 0f
            }
        }
    }

    private fun applyMultiLayerParallax(page: View, position: Float, pageWidth: Int) {
        val animationResId = page.resources.getIdentifier("animationView", "id", page.context.packageName)
        if (animationResId != 0) {
            page.findViewById<View>(animationResId)?.translationX = position * (pageWidth * 0.45f)
        }

        val titleResId = page.resources.getIdentifier("tvTitle", "id", page.context.packageName)
        if (titleResId != 0) {
            page.findViewById<View>(titleResId)?.translationX = position * (pageWidth * 0.15f)
        }
    }
}
