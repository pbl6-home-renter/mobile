package com.rentify.app.core.ui.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import coil.transform.CircleCropTransformation
import com.rentify.app.core.network.client.NetworkConfig
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.ui.R
import kotlin.math.roundToInt

class RentifyAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    data class AvatarMember(
        val userId: String?,
        val displayName: String?,
        val avatarUrl: String?
    )

    private data class AvatarPalette(
        val backgroundColorRes: Int,
        val textColorRes: Int
    )

    private val density = resources.displayMetrics.density
    private var configuredSizePx: Int = (40f * density).roundToInt()

    // Chế độ 1 avatar tròn (Cá nhân, hoặc Nhóm có ảnh, hoặc Nhóm < 2 thành viên khác)
    private val singleContainer: FrameLayout
    private val tvSingleInitial: TextView
    private val ivSingleImage: ImageView

    // Chế độ ghép 2 avatar chồng chéo (Nhóm không ảnh và >= 2 thành viên khác)
    private val compositeContainer: FrameLayout
    private val topRightContainer: FrameLayout
    private val tvTopRightInitial: TextView
    private val ivTopRightImage: ImageView
    private val bottomLeftBorderContainer: FrameLayout
    private val bottomLeftInnerContainer: FrameLayout
    private val tvBottomLeftInitial: TextView
    private val ivBottomLeftImage: ImageView

    // Huy hiệu nhóm ở góc dưới phải (~38% đường kính)
    private val badgeContainer: FrameLayout
    private val ivBadgeIcon: ImageView

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.RentifyAvatarView)
            configuredSizePx = a.getDimensionPixelSize(
                R.styleable.RentifyAvatarView_avatarSize,
                (40f * density).roundToInt()
            )
            a.recycle()
        }

        clipChildren = false
        clipToPadding = false

        val mediumFont = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_medium)

        // 1. Single container
        singleContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        tvSingleInitial = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
            includeFontPadding = false
            typeface = mediumFont
        }
        ivSingleImage = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            isVisible = false
        }
        singleContainer.addView(tvSingleInitial)
        singleContainer.addView(ivSingleImage)
        addView(singleContainer)

        // 2. Composite container (2 overlapping circles)
        compositeContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            isVisible = false
        }

        // Top-right sub-avatar (drawn behind)
        topRightContainer = FrameLayout(context)
        tvTopRightInitial = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
            includeFontPadding = false
            typeface = mediumFont
        }
        ivTopRightImage = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            isVisible = false
        }
        topRightContainer.addView(tvTopRightInitial)
        topRightContainer.addView(ivTopRightImage)
        compositeContainer.addView(topRightContainer)

        // Bottom-left sub-avatar (drawn in front, with 2dp white border)
        bottomLeftBorderContainer = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.rentify_surface))
            }
        }
        bottomLeftInnerContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        tvBottomLeftInitial = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
            includeFontPadding = false
            typeface = mediumFont
        }
        ivBottomLeftImage = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            isVisible = false
        }
        bottomLeftInnerContainer.addView(tvBottomLeftInitial)
        bottomLeftInnerContainer.addView(ivBottomLeftImage)
        bottomLeftBorderContainer.addView(bottomLeftInnerContainer)
        compositeContainer.addView(bottomLeftBorderContainer)

        addView(compositeContainer)

        // 3. Group badge (~38% avatar diameter, bottom-right)
        badgeContainer = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.rentify_surface))
            }
            isVisible = false
        }
        ivBadgeIcon = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.rentify_teal_surface))
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_group)
            setColorFilter(ContextCompat.getColor(context, R.color.rentify_teal))
        }
        badgeContainer.addView(ivBadgeIcon)
        addView(badgeContainer)

        applyProportions(configuredSizePx)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val finalW = if (wMode == MeasureSpec.EXACTLY && wSize > 0) wSize else configuredSizePx
        val finalH = if (hMode == MeasureSpec.EXACTLY && hSize > 0) hSize else configuredSizePx
        val diameter = minOf(finalW, finalH).coerceAtLeast(1)

        applyProportions(diameter)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(diameter, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(diameter, MeasureSpec.EXACTLY)
        )
    }

    private fun applyProportions(diameterPx: Int) {
        // 1. Single initial text size ~1/3 diameter
        tvSingleInitial.setTextSize(TypedValue.COMPLEX_UNIT_PX, diameterPx / 3f)

        // 2. Sub-avatars in composite mode: 65% of diameter
        val subDiameter = (diameterPx * 0.65f).roundToInt().coerceAtLeast(1)
        val subTextSize = subDiameter / 3f
        val borderPx = (2f * density).roundToInt().coerceAtLeast(1)

        topRightContainer.layoutParams = LayoutParams(subDiameter, subDiameter, Gravity.TOP or Gravity.END)
        tvTopRightInitial.setTextSize(TypedValue.COMPLEX_UNIT_PX, subTextSize)

        bottomLeftBorderContainer.layoutParams = LayoutParams(subDiameter, subDiameter, Gravity.BOTTOM or Gravity.START)
        bottomLeftBorderContainer.setPadding(borderPx, borderPx, borderPx, borderPx)
        val innerSubDiameter = (subDiameter - borderPx * 2).coerceAtLeast(1)
        tvBottomLeftInitial.setTextSize(TypedValue.COMPLEX_UNIT_PX, innerSubDiameter / 3f)

        // 3. Group badge: ~38% of diameter at bottom-right
        val badgeDiameter = (diameterPx * 0.38f).roundToInt().coerceAtLeast(10)
        val badgeWhiteRingPx = (1.5f * density).roundToInt().coerceAtLeast(1)
        val badgeIconPadPx = ((badgeDiameter - badgeWhiteRingPx * 2) * 0.22f).roundToInt().coerceAtLeast(1)
        badgeContainer.layoutParams = LayoutParams(badgeDiameter, badgeDiameter, Gravity.BOTTOM or Gravity.END)
        badgeContainer.setPadding(badgeWhiteRingPx, badgeWhiteRingPx, badgeWhiteRingPx, badgeWhiteRingPx)
        ivBadgeIcon.setPadding(badgeIconPadPx, badgeIconPadPx, badgeIconPadPx, badgeIconPadPx)
    }

    /**
     * 1.1 Cá nhân:
     * - Có ảnh: ảnh cắt tròn.
     * - Không ảnh / đang tải / tải lỗi: chữ cái đầu của TỪ CUỐI trong họ tên, màu cố định theo hash của userId.
     */
    fun bindPersonal(
        userId: String?,
        displayName: String?,
        avatarUrl: String?
    ) {
        val descName = displayName?.takeIf { it.isNotBlank() } ?: "người dùng"
        contentDescription = "Ảnh đại diện $descName"

        compositeContainer.isVisible = false
        badgeContainer.isVisible = false
        singleContainer.isVisible = true

        ivTopRightDispose()

        bindSingleCircle(
            tvInitial = tvSingleInitial,
            ivImage = ivSingleImage,
            seed = userId?.takeIf { it.isNotBlank() } ?: displayName.orEmpty(),
            initialText = extractGivenNameInitial(displayName),
            avatarUrl = avatarUrl
        )
    }

    /**
     * 1.2 Nhóm, theo thứ tự ưu tiên:
     * 1. Nhóm có ảnh: ảnh cắt tròn + huy hiệu nhóm ở góc dưới phải.
     * 2. Không ảnh và có từ 2 thành viên khác người dùng trở lên: ghép 2 avatar cá nhân chồng chéo (không thêm huy hiệu).
     * 3. Nhóm chưa đủ 2 thành viên khác: chữ cái đầu của tên nhóm, màu chọn theo hash của groupId + huy hiệu nhóm.
     */
    fun bindGroup(
        groupId: String?,
        groupName: String?,
        avatarUrl: String?,
        otherMembers: List<AvatarMember> = emptyList()
    ) {
        val descName = groupName?.takeIf { it.isNotBlank() } ?: "nhóm"
        contentDescription = "Ảnh đại diện $descName"

        val resolvedGroupUrl = resolveFullAvatarUrl(avatarUrl)
        val groupSeed = groupId?.takeIf { it.isNotBlank() } ?: groupName.orEmpty()
        val groupInitial = extractGroupTitleInitial(groupName)

        fun showFallbackWithoutGroupImage() {
            ivSingleImage.dispose()
            ivSingleImage.setImageDrawable(null)
            ivSingleImage.isVisible = false

            if (otherMembers.size >= 2) {
                // Ưu tiên 2: Ghép 2 avatar cá nhân chồng chéo, KHÔNG thêm huy hiệu nhóm
                singleContainer.isVisible = false
                badgeContainer.isVisible = false
                compositeContainer.isVisible = true

                val memberFront = otherMembers[0] // Ưu tiên người gửi gần nhất ở dưới trái (phía trước)
                val memberBack = otherMembers[1]  // Thành viên thứ 2 ở trên phải (phía sau)

                bindSingleCircle(
                    tvInitial = tvTopRightInitial,
                    ivImage = ivTopRightImage,
                    seed = memberBack.userId?.takeIf { it.isNotBlank() } ?: memberBack.displayName.orEmpty(),
                    initialText = extractGivenNameInitial(memberBack.displayName),
                    avatarUrl = memberBack.avatarUrl
                )
                bindSingleCircle(
                    tvInitial = tvBottomLeftInitial,
                    ivImage = ivBottomLeftImage,
                    seed = memberFront.userId?.takeIf { it.isNotBlank() } ?: memberFront.displayName.orEmpty(),
                    initialText = extractGivenNameInitial(memberFront.displayName),
                    avatarUrl = memberFront.avatarUrl
                )
            } else {
                // Ưu tiên 3: Chữ cái đầu của tên nhóm + màu theo hash groupId + huy hiệu nhóm
                ivTopRightDispose()
                compositeContainer.isVisible = false
                singleContainer.isVisible = true
                badgeContainer.isVisible = true

                applyPaletteToInitialView(tvSingleInitial, groupSeed, groupInitial)
                tvSingleInitial.isVisible = true
            }
        }

        if (resolvedGroupUrl.isNullOrBlank()) {
            showFallbackWithoutGroupImage()
            return
        }

        // Ưu tiên 1: Nhóm có ảnh -> hiển thị fallback ngay lập tức trong lúc tải, khi tải xong hiện ảnh + huy hiệu nhóm
        showFallbackWithoutGroupImage()
        ivSingleImage.load(resolvedGroupUrl) {
            crossfade(false)
            transformations(CircleCropTransformation())
            listener(
                onSuccess = { _, _ ->
                    compositeContainer.isVisible = false
                    singleContainer.isVisible = true
                    tvSingleInitial.isVisible = false
                    ivSingleImage.isVisible = true
                    badgeContainer.isVisible = true
                },
                onError = { _, _ ->
                    showFallbackWithoutGroupImage()
                }
            )
        }
    }

    private fun ivTopRightDispose() {
        ivTopRightImage.dispose()
        ivTopRightImage.setImageDrawable(null)
        ivBottomLeftImage.dispose()
        ivBottomLeftImage.setImageDrawable(null)
    }

    private fun bindSingleCircle(
        tvInitial: TextView,
        ivImage: ImageView,
        seed: String,
        initialText: String,
        avatarUrl: String?
    ) {
        applyPaletteToInitialView(tvInitial, seed, initialText)
        tvInitial.isVisible = true
        ivImage.isVisible = false

        val resolvedUrl = resolveFullAvatarUrl(avatarUrl)
        if (resolvedUrl.isNullOrBlank()) {
            ivImage.dispose()
            ivImage.setImageDrawable(null)
            return
        }

        ivImage.load(resolvedUrl) {
            crossfade(false)
            transformations(CircleCropTransformation())
            listener(
                onStart = {
                    tvInitial.isVisible = true
                    ivImage.isVisible = false
                },
                onSuccess = { _, _ ->
                    ivImage.isVisible = true
                    tvInitial.isVisible = false
                },
                onError = { _, _ ->
                    tvInitial.isVisible = true
                    ivImage.isVisible = false
                }
            )
        }
    }

    private fun applyPaletteToInitialView(tv: TextView, seed: String, initialText: String) {
        val paletteIndex = (seed.hashCode() and Int.MAX_VALUE) % AVATAR_PALETTES.size
        val palette = AVATAR_PALETTES[paletteIndex]
        tv.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, palette.backgroundColorRes))
        }
        tv.setTextColor(ContextCompat.getColor(context, palette.textColorRes))
        tv.text = initialText
    }

    companion object {
        private val AVATAR_PALETTES = listOf(
            AvatarPalette(R.color.rentify_avatar_bg_1, R.color.rentify_avatar_text_1),
            AvatarPalette(R.color.rentify_avatar_bg_2, R.color.rentify_avatar_text_2),
            AvatarPalette(R.color.rentify_avatar_bg_3, R.color.rentify_avatar_text_3),
            AvatarPalette(R.color.rentify_avatar_bg_4, R.color.rentify_avatar_text_4)
        )

        /**
         * Chữ cái đầu của TỪ CUỐI trong họ tên tiếng Việt ("Trần Văn Tâm" -> "T")
         */
        fun extractGivenNameInitial(displayName: String?): String {
            val cleaned = displayName?.trim().orEmpty()
            if (cleaned.isEmpty()) return "U"
            val lastWord = cleaned.split(Regex("\\s+")).lastOrNull { it.isNotEmpty() } ?: cleaned
            return lastWord.first().uppercaseChar().toString()
        }

        /**
         * Chữ cái đầu của tên nhóm ("Nhóm phòng 302" -> "N")
         */
        fun extractGroupTitleInitial(groupName: String?): String {
            val cleaned = groupName?.trim().orEmpty()
            if (cleaned.isEmpty()) return "N"
            return cleaned.first().uppercaseChar().toString()
        }

        fun resolveFullAvatarUrl(avatarUrl: String?): String? {
            val raw = avatarUrl?.trim()
            if (raw.isNullOrEmpty()) return null
            if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
                return raw
            }
            val base = NetworkConfig.DEFAULT_BASE_URL.trimEnd('/')
            return if (raw.startsWith("/")) {
                val originEnd = base.indexOf("/api/")
                val origin = if (originEnd > 0) base.substring(0, originEnd) else base
                "$origin$raw"
            } else {
                "$base/$raw"
            }
        }

        /**
         * Chọn 2 thành viên khác người dùng hiện tại, ưu tiên người gửi tin gần nhất.
         */
        fun selectGroupAvatarMembers(
            currentUserId: String?,
            members: List<ConversationMemberDto>,
            recentMessages: List<MessageListResponseDto> = emptyList()
        ): List<AvatarMember> {
            val otherUserMembers = members.filter { m ->
                m.participantType.equals("user", ignoreCase = true) &&
                    !m.userId.isNullOrBlank() &&
                    (currentUserId.isNullOrBlank() || m.userId != currentUserId)
            }

            if (otherUserMembers.size >= 2) {
                val memberById = otherUserMembers.associateBy { it.userId!! }
                val prioritized = ArrayList<ConversationMemberDto>(2)
                val addedIds = HashSet<String>()

                for (msg in recentMessages) {
                    val senderId = msg.senderUserId ?: continue
                    if (senderId == currentUserId || addedIds.contains(senderId)) continue
                    val matched = memberById[senderId]
                    if (matched != null) {
                        prioritized.add(matched)
                        addedIds.add(senderId)
                        if (prioritized.size == 2) break
                    }
                }

                if (prioritized.size < 2) {
                    for (m in otherUserMembers) {
                        val id = m.userId ?: continue
                        if (!addedIds.contains(id)) {
                            prioritized.add(m)
                            addedIds.add(id)
                            if (prioritized.size == 2) break
                        }
                    }
                }

                return prioritized.map {
                    AvatarMember(
                        userId = it.userId,
                        displayName = it.displayName,
                        avatarUrl = it.avatarUrl
                    )
                }
            }

            // Fallback từ recentMessages nếu danh sách members chưa tải xong
            val fromMessages = ArrayList<AvatarMember>(2)
            val seenSenderIds = HashSet<String>()
            for (msg in recentMessages) {
                val senderId = msg.senderUserId ?: continue
                if (!msg.senderType.equals("user", ignoreCase = true)) continue
                if (!currentUserId.isNullOrBlank() && senderId == currentUserId) continue
                if (seenSenderIds.add(senderId)) {
                    fromMessages.add(
                        AvatarMember(
                            userId = senderId,
                            displayName = msg.senderName,
                            avatarUrl = msg.senderAvatarUrl
                        )
                    )
                    if (fromMessages.size == 2) break
                }
            }
            return if (fromMessages.size >= 2) fromMessages else otherUserMembers.map {
                AvatarMember(
                    userId = it.userId,
                    displayName = it.displayName,
                    avatarUrl = it.avatarUrl
                )
            }
        }
    }
}
