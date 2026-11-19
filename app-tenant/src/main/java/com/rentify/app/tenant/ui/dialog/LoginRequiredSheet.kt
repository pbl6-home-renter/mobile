package com.rentify.app.tenant.ui.dialog

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.tenant.R

enum class LoginFeature {
    MESSAGES,
    MY_ROOM,
    ROOMMATE,
    PROFILE
}

class LoginRequiredSheet : BottomSheetDialogFragment() {

    var onLogin: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    private var isLoginTriggered = false

    private val feature: LoginFeature by lazy {
        val name = arguments?.getString(ARG_FEATURE) ?: LoginFeature.PROFILE.name
        runCatching { LoginFeature.valueOf(name) }.getOrDefault(LoginFeature.PROFILE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { sheet ->
                    sheet.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.WHITE)
                        val r = dpF(24f)
                        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                    }
                    BottomSheetBehavior.from(sheet).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        skipCollapsed = true
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val fontRegular = ResourcesCompat.getFont(ctx, CoreUiR.font.plus_jakarta_sans)
        val fontMedium = ResourcesCompat.getFont(ctx, CoreUiR.font.plus_jakarta_sans_medium)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padH = dp(22f)
            setPadding(padH, dp(12f), padH, dp(20f))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                val r = dpF(24f)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }

        // Drag handle (36x4dp, #E2E8F0)
        val dragHandle = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36f), dp(4f))
            background = roundedRect(Color.parseColor("#E2E8F0"), dpF(2f))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        root.addView(dragHandle)

        // 1. Vùng xem trước (teaser): cao 112dp, cách handle 18dp
        val teaserWrapper = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(112f)
            ).apply {
                topMargin = dp(18f)
            }
            clipChildren = true
            clipToPadding = true
        }

        val teaserContent = when (feature) {
            LoginFeature.MESSAGES -> buildMessagesTeaser()
            LoginFeature.MY_ROOM -> buildMyRoomTeaser()
            LoginFeature.ROOMMATE -> buildRoommateTeaser()
            LoginFeature.PROFILE -> buildProfileTeaser()
        }.apply {
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurPx = dpF(3f)
                setRenderEffect(RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP))
            } else {
                alpha = 0.35f
            }
        }
        teaserWrapper.addView(
            teaserContent,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Lớp phủ gradient dọc từ trong suốt (50%) đến trắng (100%)
        val fadeOverlay = View(ctx).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            background = ShapeDrawable(RectShape()).apply {
                shaderFactory = object : ShapeDrawable.ShaderFactory() {
                    override fun resize(width: Int, height: Int): Shader {
                        return LinearGradient(
                            0f,
                            0f,
                            0f,
                            height.toFloat(),
                            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                }
                paint.style = Paint.Style.FILL
            }
        }
        teaserWrapper.addView(
            fadeOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Nhãn chính giữa: viên thuốc nền #161B2C, chữ trắng 12sp medium, icon ổ khóa 14dp, padding 6x12dp
        val badgePill = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
            background = roundedRect(Color.parseColor("#161B2C"), dpF(999f))
            elevation = dpF(2f)

            val lockIcon = ImageView(ctx).apply {
                setImageResource(CoreUiR.drawable.ic_lock_closed)
                setColorFilter(Color.WHITE)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = LinearLayout.LayoutParams(dp(14f), dp(14f)).apply {
                    marginEnd = dp(6f)
                }
            }
            val badgeText = TextView(ctx).apply {
                text = getString(R.string.login_required_badge)
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = fontMedium
                includeFontPadding = false
            }
            addView(lockIcon)
            addView(badgeText)
        }
        teaserWrapper.addView(
            badgePill,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        root.addView(teaserWrapper)

        // 2. Tiêu đề: 18sp, medium, màu #161B2C, maxLines = 1, overflow = Ellipsis, cách teaser 20dp
        val (titleRes, descRes) = when (feature) {
            LoginFeature.MESSAGES -> R.string.login_required_messages_title to R.string.login_required_messages_desc
            LoginFeature.MY_ROOM -> R.string.login_required_my_room_title to R.string.login_required_my_room_desc
            LoginFeature.ROOMMATE -> R.string.login_required_roommate_title to R.string.login_required_roommate_desc
            LoginFeature.PROFILE -> R.string.login_required_profile_title to R.string.login_required_profile_desc
        }

        val tvTitle = TextView(ctx).apply {
            text = getString(titleRes)
            setTextColor(Color.parseColor("#161B2C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = fontMedium
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20f)
            }
        }
        root.addView(tvTitle)

        // 3. Mô tả: 14sp, màu #475569, cách tiêu đề 6dp
        val tvDesc = TextView(ctx).apply {
            text = getString(descRes)
            setTextColor(Color.parseColor("#475569"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = fontRegular
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6f)
            }
        }
        root.addView(tvDesc)

        // 4. Nút "Đăng nhập": dùng lại style pill button (cùng style tab Khám phá) rộng hết chiều ngang, cao 50dp, cách mô tả 22dp
        val btnLogin = TextView(ctx).apply {
            text = getString(R.string.login_required_action)
            setTextColor(ContextCompat.getColorStateList(ctx, CoreUiR.color.text_pill_button))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = fontMedium
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, CoreUiR.drawable.bg_pill_button)
            elevation = dpF(6f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                outlineSpotShadowColor = ContextCompat.getColor(ctx, CoreUiR.color.rentify_primary)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50f)
            ).apply {
                topMargin = dp(22f)
            }
            setOnClickListener {
                isLoginTriggered = true
                dismissAllowingStateLoss()
                onLogin?.invoke()
            }
        }
        root.addView(btnLogin)

        // 5. "Để sau": chữ 14sp medium #475569, căn giữa, vùng bấm cao tối thiểu 44dp, gọi onDismiss
        val btnLater = TextView(ctx).apply {
            text = getString(R.string.login_required_later)
            setTextColor(Color.parseColor("#475569"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = fontMedium
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            val typedValue = TypedValue()
            if (ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                setBackgroundResource(typedValue.resourceId)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44f)
            ).apply {
                topMargin = dp(6f)
            }
            setOnClickListener {
                dismissAllowingStateLoss()
            }
        }
        root.addView(btnLater)

        return root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isLoginTriggered) {
            onDismiss?.invoke()
        }
    }

    private fun buildMessagesTeaser(): View {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4f), dp(2f), dp(4f), 0)
        }
        val avatarColors = intArrayOf(
            Color.parseColor("#D9C9B6"),
            Color.parseColor("#B9C6D9"),
            Color.parseColor("#C9D6C8")
        )
        val topWidths = floatArrayOf(110f, 130f, 96f)
        val bottomWidths = floatArrayOf(165f, 150f, 140f)

        for (i in 0..2) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (i > 0) topMargin = dp(12f)
                }
            }

            val avatar = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40f), dp(40f))
                background = roundedRect(avatarColors[i], dpF(20f))
            }
            row.addView(avatar)

            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12f)
                    marginEnd = dp(12f)
                }
                addView(fakeBar(dp(topWidths[i]), dp(10f), isDark = true))
                addView(fakeBar(dp(bottomWidths[i]), dp(8f), isDark = false).apply {
                    (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6f)
                })
            }
            row.addView(textCol)

            if (i == 1) {
                val unreadDot = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(16f), dp(16f))
                    background = roundedRect(Color.parseColor("#2563EB"), dpF(8f))
                }
                row.addView(unreadDot)
            } else {
                row.addView(fakeBar(dp(28f), dp(8f), isDark = false))
            }

            container.addView(row)
        }
        return container
    }

    private fun buildMyRoomTeaser(): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = dpF(16f)
                setStroke(dp(1f), Color.parseColor("#E2E8F0"))
            }
            clipToOutline = true
        }

        // Dải đầu nền #F2FBFD: ảnh vuông 38dp bo 10dp (#E3DED6) + 2 thanh chữ
        val headerStrip = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F2FBFD"))
                val r = dpF(15f)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        val thumb = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(38f), dp(38f))
            background = roundedRect(Color.parseColor("#E3DED6"), dpF(10f))
        }
        val headerTextCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10f)
            }
            addView(fakeBar(dp(120f), dp(10f), isDark = true))
            addView(fakeBar(dp(84f), dp(8f), isDark = false).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6f)
            })
        }
        headerStrip.addView(thumb)
        headerStrip.addView(headerTextCol)
        card.addView(headerStrip)

        // Hàng hóa đơn: 2 thanh chữ (thanh dưới cao 14dp) + viên thuốc 70x26dp màu #9DB7F5
        val invoiceRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(8f), dp(12f), dp(6f))
        }
        val invoiceTextCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(fakeBar(dp(76f), dp(8f), isDark = false))
            addView(fakeBar(dp(112f), dp(14f), isDark = true).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4f)
            })
        }
        val payPill = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(70f), dp(26f))
            background = roundedRect(Color.parseColor("#9DB7F5"), dpF(13f))
        }
        invoiceRow.addView(invoiceTextCol)
        invoiceRow.addView(payPill)
        card.addView(invoiceRow)

        // Thanh hợp đồng cao 5dp: nền #EEF0F4, phần chạy 55% màu #008394
        val progressTrack = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 100f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(5f)
            ).apply {
                marginStart = dp(12f)
                marginEnd = dp(12f)
                bottomMargin = dp(8f)
            }
            background = roundedRect(Color.parseColor("#EEF0F4"), dpF(2.5f))
        }
        val progressFill = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 55f)
            background = roundedRect(Color.parseColor("#008394"), dpF(2.5f))
        }
        progressTrack.addView(progressFill)
        card.addView(progressTrack)

        return card
    }

    private fun buildRoommateTeaser(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
        }
        val avatarColors = intArrayOf(
            Color.parseColor("#DBE1FF"),
            Color.parseColor("#D9C9B6"),
            Color.parseColor("#C9D6C8")
        )

        for (i in 0..2) {
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
                layoutParams = LinearLayout.LayoutParams(dp(120f), dp(108f)).apply {
                    if (i > 0) marginStart = dp(10f)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.WHITE)
                    cornerRadius = dpF(16f)
                    setStroke(dp(1f), Color.parseColor("#E2E8F0"))
                }
            }
            val avatar = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(38f), dp(38f))
                background = roundedRect(avatarColors[i], dpF(19f))
            }
            val bar1 = fakeBar(dp(76f), dp(10f), isDark = true).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(10f)
            }
            val bar2 = fakeBar(dp(54f), dp(8f), isDark = false).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6f)
            }
            card.addView(avatar)
            card.addView(bar1)
            card.addView(bar2)
            row.addView(card)
        }
        return row
    }

    private fun buildProfileTeaser(): View {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8f), dp(2f), dp(8f), 0)
        }

        // Hàng đầu: avatar tròn 48dp (#DBE1FF) + 2 thanh chữ
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val avatar = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48f), dp(48f))
            background = roundedRect(Color.parseColor("#DBE1FF"), dpF(24f))
        }
        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12f)
            }
            addView(fakeBar(dp(124f), dp(10f), isDark = true))
            addView(fakeBar(dp(168f), dp(8f), isDark = false).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6f)
            })
        }
        headerRow.addView(avatar)
        headerRow.addView(textCol)
        container.addView(headerRow)

        // Bên dưới 2 dòng menu: mỗi dòng gồm ô vuông 18dp bo 5dp (#CBD5E1) + 1 thanh chữ
        val menuWidths = floatArrayOf(150f, 120f)
        for (i in 0..1) {
            val menuRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10f)
                }
            }
            val iconBox = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(18f), dp(18f))
                background = roundedRect(Color.parseColor("#CBD5E1"), dpF(5f))
            }
            val bar = fakeBar(dp(menuWidths[i]), dp(10f), isDark = false).apply {
                (layoutParams as LinearLayout.LayoutParams).marginStart = dp(10f)
            }
            menuRow.addView(iconBox)
            menuRow.addView(bar)
            container.addView(menuRow)
        }

        return container
    }

    private fun fakeBar(widthPx: Int, heightPx: Int, isDark: Boolean): View {
        val color = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#E2E8F0")
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, heightPx)
            background = roundedRect(color, dpF(4f))
        }
    }

    private fun roundedRect(color: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radiusPx
        }
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun dpF(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    companion object {
        const val TAG = "LoginRequiredSheet"
        private const val ARG_FEATURE = "arg_feature"

        fun newInstance(
            feature: LoginFeature,
            onLogin: () -> Unit,
            onDismiss: () -> Unit = {}
        ): LoginRequiredSheet {
            return LoginRequiredSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FEATURE, feature.name)
                }
                this.onLogin = onLogin
                this.onDismiss = onDismiss
            }
        }
    }
}

fun Fragment.showLoginRequiredSheet(
    feature: LoginFeature,
    onDismiss: () -> Unit = {},
    onLogin: () -> Unit
) {
    showLoginRequiredSheetInternal(childFragmentManager, feature, onDismiss, onLogin)
}

fun FragmentActivity.showLoginRequiredSheet(
    feature: LoginFeature,
    onDismiss: () -> Unit = {},
    onLogin: () -> Unit
) {
    showLoginRequiredSheetInternal(supportFragmentManager, feature, onDismiss, onLogin)
}

private fun showLoginRequiredSheetInternal(
    fragmentManager: FragmentManager,
    feature: LoginFeature,
    onDismiss: () -> Unit,
    onLogin: () -> Unit
) {
    val existing = fragmentManager.findFragmentByTag(LoginRequiredSheet.TAG) as? LoginRequiredSheet
    existing?.dismissAllowingStateLoss()

    val sheet = LoginRequiredSheet.newInstance(
        feature = feature,
        onLogin = onLogin,
        onDismiss = onDismiss
    )
    sheet.show(fragmentManager, LoginRequiredSheet.TAG)
}
