package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.lessonbleinteraction01.BleApplication
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.databinding.FragmentServicesBinding
import com.grandfatherpikhto.lessonbleinteraction01.ui.MainActivity
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.MainActivityViewModel
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.ServicesViewModel

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ServicesFragment : Fragment() {

    private var _binding: FragmentServicesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val bleGattManager by lazy {
        // (requireContext().applicationContext as BleApplication).bleGattManager
        (requireActivity() as MainActivity).bleGattManager
    }

    private val logTag = this.javaClass.name
    private val servicesViewModel by viewModels<ServicesViewModel>()
    private val mainActivityViewModel by viewModels<MainActivityViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentServicesBinding.inflate(inflater, container, false)

        binding.apply {
            buttonSecond.setOnClickListener {
                findNavController().navigate(R.id.action_ServicesFragment_to_ScanFragment)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainActivityViewModel.currentDevice?.let { device ->
            Log.d(logTag, "Try connect to device $device")
            // bleGattManager?.connect(device.address)
        }
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

