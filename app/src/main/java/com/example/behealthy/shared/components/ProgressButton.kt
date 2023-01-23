package com.example.behealthy.shared.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.behealthy.R
import com.example.behealthy.databinding.LayoutProgressButtonBinding

class ProgressButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding = LayoutProgressButtonBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )

    private var buttonText = ""
    private var isGray = false
    private var isTextBlack = false
    private var buttonEnabled = false
    private var outlined = false
    private var buttonStyle = 0

    private val enableTextColor
        get() = ResourcesCompat.getColor(resources, R.color.enable_button_text, null)
    private val disableTextColor
        get() = ResourcesCompat.getColor(resources, R.color.disable_button_text, null)

    var isLoading: Boolean = false
        set(value) {
            field = value
            if (value) setLoadingState() else clearLoading()
        }

    init {
        getAtributes(attrs)
        setText(buttonText)
        setBackgroundButton(isGray)
        setButtonEnable(buttonEnabled)
    }

    override fun isEnabled(): Boolean = buttonEnabled

    override fun setEnabled(enabled: Boolean) {
        setButtonEnable(enabled)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.buttonProgress.setOnClickListener(l)
    }

    fun setText(buttonText: String) {
        binding.buttonText.text = buttonText
    }

    fun setButtonEnable(buttonsEnable: Boolean) = with(binding) {
        buttonProgress.isEnabled = buttonsEnable
        val color = if (buttonsEnable && !isTextBlack) {
            enableTextColor
        } else if (buttonsEnable && isTextBlack) {
            resources.getColor(R.color.black)
        } else { disableTextColor }
        buttonText.setTextColor(color)
    }

    fun getAtributes(attrs: AttributeSet?) = context.theme.obtainStyledAttributes(
        attrs,
        R.styleable.ProgressButton,
        0,
        0,
    ).apply {
        buttonText = getString(R.styleable.ProgressButton_textButton).toString()
        isGray = getBoolean(R.styleable.ProgressButton_isBackgroundGray, isGray)
        isTextBlack = getBoolean(R.styleable.ProgressButton_isTextBlack, isTextBlack)
        buttonEnabled = getBoolean(R.styleable.ProgressButton_enabledButton, buttonEnabled)
        buttonStyle = getResourceId(R.styleable.ProgressButton_iconButton, 0)
        outlined = getBoolean(R.styleable.ProgressButton_styleButton, outlined)
    }

    private fun setBackgroundButton(isGray: Boolean) = with(binding) {
        binding.buttonProgress.background =
            if (isGray) {
                ResourcesCompat.getDrawable(resources, R.drawable.background_custom_button_gray, null)
            } else {
                ResourcesCompat.getDrawable(resources, R.drawable.background_custom_button, null)
            }
    }

    private fun setLoadingState() = with(binding) {
        buttonProgress.text = ""
        setButtonEnable(false)
        buttonText.visibility = View.GONE
        progressButton.visibility = View.VISIBLE
    }

    private fun clearLoading() {
        setText(buttonText)
        setButtonEnable(true)
        with(binding) {
            buttonText.visibility = View.VISIBLE
            buttonText.isEnabled = true
            progressButton.visibility = View.GONE
        }
    }
}
