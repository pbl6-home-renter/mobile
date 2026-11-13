package com.rentify.app.tenant.ui.chat.detail

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import coil.load
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.DialogPhotoViewerBinding

class PhotoViewerDialogFragment : DialogFragment() {

    private var _binding: DialogPhotoViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPhotoViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL).orEmpty()

        binding.pbLoading.isVisible = true
        binding.ivFullPhoto.load(imageUrl) {
            crossfade(true)
            placeholder(CoreR.drawable.ic_image_outline)
            error(CoreR.drawable.ic_image_outline)
            listener(
                onSuccess = { _, _ -> binding.pbLoading.isVisible = false },
                onError = { _, _ -> binding.pbLoading.isVisible = false }
            )
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URL = "arg_image_url"

        fun newInstance(imageUrl: String): PhotoViewerDialogFragment {
            return PhotoViewerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
        }
    }
}

