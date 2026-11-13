package com.rentify.app.tenant.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.rentify.app.tenant.databinding.DialogRequireLoginBinding

class RequireLoginDialogFragment : DialogFragment() {

    private var _binding: DialogRequireLoginBinding? = null
    private val binding get() = _binding!!

    var onLoginClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRequireLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customTitle = arguments?.getString(ARG_TITLE)
        val customMessage = arguments?.getString(ARG_MESSAGE)

        if (!customTitle.isNullOrBlank()) {
            binding.tvDialogTitle.text = customTitle
        }

        if (!customMessage.isNullOrBlank()) {
            binding.tvDialogMessage.text = customMessage
        }

        binding.btnDialogLogin.setOnClickListener {
            dismiss()
            onLoginClick?.invoke()
        }

        binding.btnDialogCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RequireLoginDialogFragment"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"

        fun newInstance(
            title: String? = null,
            message: String? = null,
            onLoginClick: () -> Unit
        ): RequireLoginDialogFragment {
            return RequireLoginDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                }
                this.onLoginClick = onLoginClick
            }
        }
    }
}

fun Fragment.showRequireLoginDialog(
    message: String? = null,
    title: String? = null,
    onLoginClick: () -> Unit
) {
    val dialog = RequireLoginDialogFragment.newInstance(
        title = title,
        message = message,
        onLoginClick = onLoginClick
    )
    dialog.show(childFragmentManager, RequireLoginDialogFragment.TAG)
}

fun FragmentActivity.showRequireLoginDialog(
    message: String? = null,
    title: String? = null,
    onLoginClick: () -> Unit
) {
    val dialog = RequireLoginDialogFragment.newInstance(
        title = title,
        message = message,
        onLoginClick = onLoginClick
    )
    dialog.show(supportFragmentManager, RequireLoginDialogFragment.TAG)
}
