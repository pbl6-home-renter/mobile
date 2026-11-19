package com.rentify.app.core.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.card.MaterialCardView
import com.rentify.app.core.ui.R

class RentifyFloatingBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    data class TabItem(
        val id: Int,
        val titleRes: Int,
        val iconRes: Int
    )

    private var tabs: List<TabItem> = emptyList()

    private val colorInactive = Color.parseColor("#9CA3AF")
    private val colorActive = Color.parseColor("#FFFFFF")

    private val pillHeightPx = dpToPx(42f)
    private val minTouchTargetPx = dpToPx(44f)
    private val horizontalPaddingPx = dpToPx(6f)

    private val pillGlowView: View
    private val pillIndicatorView: View
    private val tabViewsContainer: FrameLayout
    private val tabItemViews = mutableListOf<TabViewHolder>()

    private var selectedIndex = 0
    private var currentAnimator: ValueAnimator? = null

    private var onTabSelectedListener: ((Int) -> Boolean)? = null

    init {
        radius = dpToPx(30f).toFloat()
        cardElevation = dpToPx(8f).toFloat()
        maxCardElevation = dpToPx(10f).toFloat()
        setCardBackgroundColor(Color.WHITE)
        strokeWidth = 0
        strokeColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            outlineSpotShadowColor = Color.parseColor("#262563EB")
            outlineAmbientShadowColor = Color.parseColor("#14000000")
        }

        val rootLayout = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            clipChildren = false
            clipToPadding = false
        }
        pillGlowView = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_nav_pill_glow)
            val lp = FrameLayout.LayoutParams(0, pillHeightPx).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = lp
            translationY = dpToPx(3f).toFloat()
        }
        rootLayout.addView(pillGlowView)
        pillIndicatorView = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_pill_gradient)
            val lp = FrameLayout.LayoutParams(0, pillHeightPx).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = lp
        }
        rootLayout.addView(pillIndicatorView)
        tabViewsContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            clipChildren = false
            clipToPadding = false
        }

        setupTabViews()
        rootLayout.addView(tabViewsContainer)
        addView(rootLayout)
    }

    private class TabViewHolder(
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView,
        val item: TabItem
    )

    private fun setupTabViews() {
        tabItemViews.clear()
        tabViewsContainer.removeAllViews()

        val typeface = try {
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_semibold)
        } catch (_: Exception) {
            android.graphics.Typeface.DEFAULT_BOLD
        }

        tabs.forEachIndexed { index, item ->
            val tabLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                isBaselineAligned = false
                clipChildren = false
                clipToPadding = false
                isClickable = true
                isFocusable = true
                val lp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    pillHeightPx
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                layoutParams = lp
                val typedValue = TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)) {
                    setBackgroundResource(typedValue.resourceId)
                }

                setOnClickListener {
                    handleTabClick(index)
                }
            }

            val iconView = ImageView(context).apply {
                setImageResource(item.iconRes)
                imageTintList = ColorStateList.valueOf(if (index == selectedIndex) colorActive else colorInactive)
                val size = dpToPx(24f)
                val lp = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                layoutParams = lp
            }

            val labelView = TextView(context).apply {
                setText(item.titleRes)
                setTextColor(colorActive)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface)
                isSingleLine = true
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL
                visibility = if (index == selectedIndex) View.VISIBLE else View.GONE
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    marginStart = dpToPx(6f)
                }
                layoutParams = lp
            }

            tabLayout.addView(iconView)
            tabLayout.addView(labelView)

            val holder = TabViewHolder(tabLayout, iconView, labelView, item)
            tabItemViews.add(holder)
            tabViewsContainer.addView(tabLayout)
        }
    }

    fun setTabs(items: List<TabItem>) {
        tabs = items
        selectedIndex = 0
        setupTabViews()
        post { applyLayoutState(selectedIndex) }
    }

    private fun handleTabClick(targetIndex: Int) {
        if (targetIndex == selectedIndex) return

        val targetTab = tabs[targetIndex]
        val canSelect = onTabSelectedListener?.invoke(targetTab.id) ?: true
        if (canSelect) {
            animateToTab(targetIndex)
        }
    }

    fun setOnTabSelectedListener(listener: (Int) -> Boolean) {
        this.onTabSelectedListener = listener
    }

    var selectedItemId: Int
        get() = tabs.getOrNull(selectedIndex)?.id ?: View.NO_ID
        set(value) {
            val index = tabs.indexOfFirst { it.id == value }
            if (index != -1 && index != selectedIndex) {
                animateToTab(index)
            }
        }

    fun setSelectedTab(id: Int, animate: Boolean = true) {
        val index = tabs.indexOfFirst { it.id == valueOrFirst(id) }
        if (index != -1) {
            if (animate) {
                animateToTab(index)
            } else {
                selectedIndex = index
                applyLayoutState(index)
            }
        }
    }

    private fun valueOrFirst(id: Int): Int {
        return if (tabs.any { it.id == id }) id else tabs.first().id
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post {
            applyLayoutState(selectedIndex)
        }
    }

    private data class TabMetrics(
        val left: Float,
        val width: Float
    )

    private fun calculateMetrics(activeIndex: Int, totalW: Float): List<TabMetrics> {
        val count = tabs.size
        val availableW = totalW - horizontalPaddingPx * 2

        val activeHolder = tabItemViews[activeIndex]
        val activeTextWidth = activeHolder.label.paint.measureText(context.getString(activeHolder.item.titleRes))
        val activeIconWidth = dpToPx(24f).toFloat()
        val spacing = dpToPx(6f).toFloat()
        val pillPadding = dpToPx(26f).toFloat()

        val activePillW = (activeIconWidth + spacing + activeTextWidth + pillPadding).coerceAtLeast(dpToPx(88f).toFloat())
        val remainingW = (availableW - activePillW).coerceAtLeast((count - 1) * minTouchTargetPx.toFloat())
        val inactiveW = remainingW / (count - 1)

        val result = mutableListOf<TabMetrics>()
        var currentX = horizontalPaddingPx.toFloat()

        for (i in 0 until count) {
            if (i == activeIndex) {
                result.add(TabMetrics(currentX, activePillW))
                currentX += activePillW
            } else {
                result.add(TabMetrics(currentX, inactiveW))
                currentX += inactiveW
            }
        }
        return result
    }

    private fun applyLayoutState(activeIndex: Int) {
        if (width <= 0 || tabItemViews.isEmpty()) return

        val metrics = calculateMetrics(activeIndex, width.toFloat())
        val activeMetrics = metrics[activeIndex]
        pillIndicatorView.translationX = activeMetrics.left
        val lp = pillIndicatorView.layoutParams
        lp.width = activeMetrics.width.toInt()
        pillIndicatorView.layoutParams = lp

        pillGlowView.translationX = activeMetrics.left
        val glowLp = pillGlowView.layoutParams
        glowLp.width = activeMetrics.width.toInt()
        pillGlowView.layoutParams = glowLp
        for (i in tabItemViews.indices) {
            val holder = tabItemViews[i]
            val m = metrics[i]

            holder.container.translationX = m.left
            val containerLp = holder.container.layoutParams as? FrameLayout.LayoutParams
                ?: FrameLayout.LayoutParams(m.width.toInt(), pillHeightPx).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            containerLp.width = m.width.toInt()
            containerLp.height = pillHeightPx
            containerLp.gravity = Gravity.CENTER_VERTICAL
            holder.container.layoutParams = containerLp

            if (i == activeIndex) {
                holder.label.visibility = View.VISIBLE
                holder.label.alpha = 1f
                holder.icon.imageTintList = ColorStateList.valueOf(colorActive)
            } else {
                holder.label.visibility = View.GONE
                holder.label.alpha = 0f
                holder.icon.imageTintList = ColorStateList.valueOf(colorInactive)
            }
        }
    }

    private fun animateToTab(targetIndex: Int) {
        if (width <= 0) {
            selectedIndex = targetIndex
            return
        }

        currentAnimator?.cancel()

        val fromIndex = selectedIndex
        selectedIndex = targetIndex

        val fromMetrics = calculateMetrics(fromIndex, width.toFloat())
        val toMetrics = calculateMetrics(targetIndex, width.toFloat())

        val fromPill = fromMetrics[fromIndex]
        val toPill = toMetrics[targetIndex]

        val oldHolder = tabItemViews[fromIndex]
        val newHolder = tabItemViews[targetIndex]

        newHolder.label.visibility = View.VISIBLE

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280L
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animator ->
                val f = animator.animatedValue as Float
                val pillX = lerp(fromPill.left, toPill.left, f)
                val pillW = lerp(fromPill.width, toPill.width, f)

                pillIndicatorView.translationX = pillX
                pillIndicatorView.layoutParams.width = pillW.toInt()
                pillIndicatorView.requestLayout()

                pillGlowView.translationX = pillX
                pillGlowView.layoutParams.width = pillW.toInt()
                pillGlowView.requestLayout()
                for (i in tabItemViews.indices) {
                    val holder = tabItemViews[i]
                    val startM = fromMetrics[i]
                    val endM = toMetrics[i]

                    holder.container.translationX = lerp(startM.left, endM.left, f)
                    val cLp = holder.container.layoutParams
                    cLp.width = lerp(startM.width, endM.width, f).toInt()
                    cLp.height = pillHeightPx
                    holder.container.layoutParams = cLp
                }
                oldHolder.label.alpha = (1f - f * 1.5f).coerceIn(0f, 1f)
                newHolder.label.alpha = ((f - 0.2f) * 1.25f).coerceIn(0f, 1f)

                oldHolder.icon.imageTintList = ColorStateList.valueOf(
                    ColorUtils.blendARGB(colorActive, colorInactive, f)
                )
                newHolder.icon.imageTintList = ColorStateList.valueOf(
                    ColorUtils.blendARGB(colorInactive, colorActive, f)
                )
            }

            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    oldHolder.label.visibility = View.GONE
                    applyLayoutState(targetIndex)
                }
            })

            start()
        }
    }

    private fun lerp(a: Float, b: Float, f: Float): Float {
        return a + (b - a) * f
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
