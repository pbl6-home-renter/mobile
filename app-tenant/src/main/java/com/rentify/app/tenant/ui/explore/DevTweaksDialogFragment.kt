package com.rentify.app.tenant.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.rentify.app.tenant.databinding.DialogDevTweaksBinding

class DevTweaksDialogFragment(
    private val currentTweakHasRoom: Boolean?,
    private val onTweakChanged: (Boolean?) -> Unit
) : DialogFragment() {

    private var _binding: DialogDevTweaksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDevTweaksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchHasRoom.isChecked = currentTweakHasRoom == true
        updateDescText(binding.switchHasRoom.isChecked)

        binding.switchHasRoom.setOnCheckedChangeListener { _, isChecked ->
            updateDescText(isChecked)
            onTweakChanged(isChecked)
        }

        binding.btnCloseTweaks.setOnClickListener {
            dismiss()
        }
    }

    private fun updateDescText(isChecked: Boolean) {
        binding.tvHasRoomDesc.text = if (isChecked) {
            "Đang bật: Khối 'Phòng của tôi' sẽ luôn hiển thị"
        } else {
            "Đang tắt: Khối 'Phòng của tôi' sẽ bị ẩn"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DevTweaksDialogFragment"

        fun newInstance(
            currentTweak: Boolean?,
            onTweakChanged: (Boolean?) -> Unit
        ): DevTweaksDialogFragment = DevTweaksDialogFragment(currentTweak, onTweakChanged)
    }
}
