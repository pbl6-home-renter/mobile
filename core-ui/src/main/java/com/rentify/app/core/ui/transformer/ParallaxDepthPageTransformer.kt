package com.rentify.app.core.ui.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Hiệu ứng chuyển trang 3D Parallax & Depth mượt mà cho ViewPager2.
 * Đảm bảo trang tiếp theo hoàn toàn mờ (alpha = 0) khi ở ranh giới trang để không bị chờm chữ.
 */
class ParallaxDepthPageTransformer(
    private val minScale: Float = 0.85f,
    private val parallaxFactor: Float = 0.3f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width

        when {
            position < -1f -> {
                page.alpha = 0f
            }
            position <= 0f -> {
                // Trang hiện tại trượt ra bên trái
                val factor = 1f + position
                page.alpha = factor
                page.translationX = 0f
                page.scaleX = 1f
                page.scaleY = 1f

                val animationView = findInnerAnimationView(page)
                animationView?.translationX = position * (pageWidth * parallaxFactor)
            }
            position <= 1f -> {
                // Trang tiếp theo đi vào từ bên phải
                val factor = 1f - abs(position)

                // Alpha = 0 khi ở ranh giới (position = 1), tăng dần lên 1 khi vào chính giữa
                page.alpha = factor
                page.translationX = 0f

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
