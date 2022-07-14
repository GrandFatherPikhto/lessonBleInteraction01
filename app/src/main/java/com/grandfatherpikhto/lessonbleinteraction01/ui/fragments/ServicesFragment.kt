package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.lessonbleinteraction01.BleApplication
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.databinding.FragmentServicesBinding
import com.grandfatherpikhto.lessonbleinteraction01.ui.MainActivity
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters.ServicesAdapter
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.MainActivityViewModel
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.ServicesViewModel
import kotlinx.coroutines.launch

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

    private val servicesAdapter = ServicesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentServicesBinding.inflate(inflater, container, false)

        binding.apply {
            rvServices.adapter = servicesAdapter
            rvServices.layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            bleGattManager.flowConnectionState.collect { state ->
                if (state == BleGattManager.State.Discovered) {
                    servicesAdapter.bluetoothGatt = bleGattManager.gatt
                    bindBleDevice(bleGattManager.gatt?.device)
                } else {
                    servicesAdapter.bluetoothGatt = null
                    bindBleDevice(null)
                }
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
        bleGattManager.disconnect()
        _binding = null
    }

    private fun bindBleDevice(bluetoothDevice: BluetoothDevice?) {
        binding.apply {
            tvBleDeviceAddress.text =
                bluetoothDevice?.address ?: getString(R.string.default_device_address)
            tvBleDeviceName.text =
                bluetoothDevice?.name ?: getString(R.string.default_device_name)
        }
    }
}

