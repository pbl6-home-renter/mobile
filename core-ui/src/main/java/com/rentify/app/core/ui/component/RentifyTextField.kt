package com.rentify.app.core.ui.component

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.databinding.ViewRentifyTextFieldBinding

class RentifyTextField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewRentifyTextFieldBinding.inflate(LayoutInflater.from(context), this, true)

    val editText: TextInputEditText
        get() = binding.editText

    val textInputLayout: TextInputLayout
        get() = binding.textInputLayout

    var text: String
        get() = binding.editText.text?.toString().orEmpty()
        set(value) {
            binding.editText.setText(value)
        }

    private var hasErrorState = false

    private val defaultStrokeSelector: ColorStateList? =
        ContextCompat.getColorStateList(context, R.color.sl_textfield_card_stroke)
    private val defaultBgSelector: ColorStateList? =
        ContextCompat.getColorStateList(context, R.color.sl_textfield_card_bg)
    private val errorStrokeColor: ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rentify_error))
    private val errorBgColor: ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rentify_field_focus_bg))

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.RentifyTextField, defStyleAttr, 0)
            try {
                val hintText = ta.getString(R.styleable.RentifyTextField_hint)
                if (!hintText.isNullOrEmpty()) {
                    binding.textInputLayout.hint = hintText
                }

                val startIconRes = ta.getResourceId(R.styleable.RentifyTextField_startIcon, 0)
                if (startIconRes != 0) {
                    binding.textInputLayout.setStartIconDrawable(startIconRes)
                    binding.textInputLayout.setStartIconTintList(
                        ContextCompat.getColorStateList(context, R.color.sl_textfield_start_icon)
                    )
                }

                val isPassword = ta.getBoolean(R.styleable.RentifyTextField_isPassword, false)
                val inputType = ta.getInt(
                    R.styleable.RentifyTextField_android_inputType,
                    if (isPassword) {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT
                    }
                )
                binding.editText.inputType = inputType

                if (isPassword) {
                    binding.textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    binding.textInputLayout.setEndIconTintList(
                        ContextCompat.getColorStateList(context, R.color.rentify_text_muted)
                    )
                }

                val imeOptions = ta.getInt(
                    R.styleable.RentifyTextField_android_imeOptions,
                    EditorInfo.IME_ACTION_UNSPECIFIED
                )
                if (imeOptions != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    binding.editText.imeOptions = imeOptions
                }
            } finally {
                ta.recycle()
            }
        }

        binding.editText.setOnFocusChangeListener { _, hasFocus ->
            binding.cardContainer.isActivated = hasFocus
            binding.textInputLayout.isActivated = hasFocus
        }
    }

    fun setErrorHighlight() {
        hasErrorState = true
        binding.cardContainer.setStrokeColor(errorStrokeColor)
        binding.cardContainer.setCardBackgroundColor(errorBgColor)
        binding.tvFieldError.text = null
        binding.tvFieldError.visibility = View.GONE
    }

    fun setError(message: String?) {
        if (!message.isNullOrBlank()) {
            setErrorHighlight()
            binding.tvFieldError.text = message
            binding.tvFieldError.visibility = View.VISIBLE
        } else {
            hasErrorState = false
            binding.cardContainer.setStrokeColor(defaultStrokeSelector)
            binding.cardContainer.setCardBackgroundColor(defaultBgSelector)
            binding.tvFieldError.text = null
            binding.tvFieldError.visibility = View.GONE
        }
    }
}
