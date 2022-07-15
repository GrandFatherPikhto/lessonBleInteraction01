package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.blin.BleBondManager
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.blin.BleManager
import com.grandfatherpikhto.lessonbleinteraction01.BleApplication
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.databinding.FragmentServicesBinding
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters.RvServicesAdapter
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

    private var _bleManager:BleManager? = null
    private val bleManager get() = _bleManager!!

    private val logTag = this.javaClass.simpleName
    private val servicesViewModel by viewModels<ServicesViewModel>()
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

    private val servicesAdapter = RvServicesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _bleManager = (requireContext().applicationContext as BleApplication).bleManager
        _binding = FragmentServicesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initServicesFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        bleManager.close()
        _binding = null
    }

    private fun fillBleDevice(bluetoothDevice: BluetoothDevice?) {
        binding.apply {
            tvBleDeviceAddress.text =
                bluetoothDevice?.address ?: getString(R.string.default_device_address)
            tvBleDeviceName.text =
                bluetoothDevice?.name ?: getString(R.string.default_device_name)
        }
    }

    private fun initServicesFragment() {
        Log.d(logTag, "BluetoothDevice ${mainActivityViewModel.currentDevice}")
        binding.apply {
            rvServices.adapter = servicesAdapter
            rvServices.layoutManager = LinearLayoutManager(requireContext())
            btnConnect.setOnClickListener {
                if (bleManager.connectState == BleGattManager.State.Connected) {
                    Log.d(logTag, "Close connect ${mainActivityViewModel.currentDevice}")
                    bleManager.close()
                } else {
                    mainActivityViewModel.currentDevice?.let { device ->
                        Log.d(logTag, "connecting $device")
                        bleManager.connect(device.address)
                    }
                }
            }

            lifecycleScope.launch {
                bleManager.flowConnectionState.collect { state ->
                    Log.d(logTag, "Connection State: $state")
                    when (state) {
                        BleGattManager.State.Connected -> {
                            servicesAdapter.bluetoothGatt = bleManager.gatt
                            btnConnect.text = getString(R.string.device_disconnect)
                        }
                        BleGattManager.State.Disconnected -> {
                            servicesAdapter.bluetoothGatt = null
                            btnConnect.text = getString(R.string.device_connect)
                        }
                        BleGattManager.State.Error -> {
                            findNavController().navigate(R.id.action_ServicesFragment_to_ScanFragment)
                        }
                        else -> {

                        }
                    }
                }
            }

            btnBond.setOnClickListener {
                mainActivityViewModel.currentDevice?.let { device ->
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        bleManager.bondRequest(device)
                    }
                }
            }

            btnBond.isVisible =
                mainActivityViewModel.currentDevice?.let { bluetoothDevice ->
                    bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED
                } ?: false

            lifecycleScope.launch {
                bleManager.flowBondState.collect() { state ->
                    when(state) {
                        BleBondManager.State.Bondend -> {
                            btnBond.isVisible = false
                        }
                        else -> {
                            btnBond.isVisible = true
                        }
                    }
                }
            }
        }

        fillBleDevice(mainActivityViewModel.currentDevice)
    }
}

