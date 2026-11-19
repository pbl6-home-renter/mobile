package com.rentify.app.tenant.ui.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.ui.R as CoreUiR

class ConversationDividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val density = context.resources.displayMetrics.density
    private val dividerHeightPx = (1f * density).coerceAtLeast(1f)
    private val leftInsetPx = 82f * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, CoreUiR.color.rentify_divider)
        style = Paint.Style.FILL
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapterCount = parent.adapter?.itemCount ?: 0
        if (adapterCount <= 1) return

        val right = (parent.width - parent.paddingRight).toFloat()
        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val adapterPos = parent.getChildAdapterPosition(child)
            if (adapterPos == RecyclerView.NO_POSITION || adapterPos >= adapterCount - 1) {
                continue
            }
            val params = child.layoutParams as? RecyclerView.LayoutParams
            val bottomMargin = params?.bottomMargin ?: 0
            val top = (child.bottom + bottomMargin).toFloat() - dividerHeightPx
            val bottom = top + dividerHeightPx
            c.drawRect(leftInsetPx, top, right, bottom, paint)
        }
    }
}
