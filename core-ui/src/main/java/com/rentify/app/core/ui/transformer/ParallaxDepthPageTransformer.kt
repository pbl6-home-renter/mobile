package com.rentify.app.core.ui.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Hiệu ứng chuyển trang 3D Card Parallax & Depth nổi bật cho ViewPager2.
 * Đảm bảo ẩn tuyệt đối các trang nằm ngoài vùng vuốt active (absPosition >= 0.95f) để không lộ bất kỳ chữ hay hình ảnh nào.
 */
class ParallaxDepthPageTransformer(
    private val minScale: Float = 0.85f,
    private val maxRotationY: Float = 10f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val absPosition = abs(position)
        val pageWidth = page.width

        // Nếu trang nằm ngoài vùng hiển thị active (absPosition >= 0.95f), ẩn hoàn toàn để không lộ chữ/hình ảnh
        if (absPosition >= 0.95f) {
            page.alpha = 0f
            page.visibility = View.INVISIBLE
            page.translationX = 0f
            resetMultiLayerParallax(page)
            return
        }

        page.visibility = View.VISIBLE
        page.pivotX = if (position < 0f) pageWidth.toFloat() else 0f
        page.pivotY = page.height * 0.5f

        // Normalize factor mượt từ 1 (center) đến 0 (khi chạm ngưỡng 0.95f)
        val normalizedFactor = ((0.95f - absPosition) / 0.95f).coerceIn(0f, 1f)

        page.alpha = normalizedFactor
        page.rotationY = position * maxRotationY

        val scaleFactor = minScale + (1f - minScale) * normalizedFactor
        page.scaleX = scaleFactor
        page.scaleY = scaleFactor

        page.translationX = pageWidth * -position * 0.10f

        applyMultiLayerParallax(page, position, pageWidth)
    }

    private fun applyMultiLayerParallax(page: View, position: Float, pageWidth: Int) {
        val animationResId = page.resources.getIdentifier("animationView", "id", page.context.packageName)
        val imageResId = if (animationResId != 0) animationResId else page.resources.getIdentifier("ivImage", "id", page.context.packageName)
        if (imageResId != 0) {
            page.findViewById<View>(imageResId)?.translationX = position * (pageWidth * 0.25f)
        }

        val titleResId = page.resources.getIdentifier("tvTitle", "id", page.context.packageName)
        if (titleResId != 0) {
            page.findViewById<View>(titleResId)?.translationX = position * (pageWidth * 0.08f)
        }
    }

    private fun resetMultiLayerParallax(page: View) {
        val animationResId = page.resources.getIdentifier("animationView", "id", page.context.packageName)
        if (animationResId != 0) {
            page.findViewById<View>(animationResId)?.translationX = 0f
        }
        val imageResId = page.resources.getIdentifier("ivImage", "id", page.context.packageName)
        if (imageResId != 0) {
            page.findViewById<View>(imageResId)?.translationX = 0f
        }

        val titleResId = page.resources.getIdentifier("tvTitle", "id", page.context.packageName)
        if (titleResId != 0) {
            page.findViewById<View>(titleResId)?.translationX = 0f
        }
    }
}
