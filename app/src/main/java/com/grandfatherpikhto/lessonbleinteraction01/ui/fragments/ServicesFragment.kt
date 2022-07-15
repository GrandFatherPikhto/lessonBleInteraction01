package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.blin.BleManager
import com.grandfatherpikhto.lessonbleinteraction01.BleApplication
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.databinding.FragmentServicesBinding
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters.RvServicesAdapter
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.MainActivityViewModel
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
    // private val servicesViewModel by viewModels<ServicesViewModel>()
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

    private val servicesAdapter = RvServicesAdapter()

    private val servicesMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_services, menu)
            menu.findItem(R.id.action_connect).let { actionConnect ->
                lifecycleScope.launch {
                    bleManager.flowConnectionState.collect { state ->
                        when(state) {
                            BleGattManager.State.Connected -> {
                                actionConnect.isEnabled = true
                                actionConnect.title = getString(R.string.device_disconnect)
                                actionConnect.setIcon(R.drawable.ic_bluetooth_disconnected)
                            }
                            BleGattManager.State.Disconnected -> {
                                actionConnect.isEnabled = true
                                actionConnect.title = getString(R.string.device_connect)
                                actionConnect.setIcon(R.drawable.ic_bluetooth_connected)
                            }
                            else -> {
                                actionConnect.isEnabled = false
                            }
                        }
                    }
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            Log.d(logTag, "onMenuItemSelected(${menuItem.itemId})")
            return when(menuItem.itemId) {
                R.id.action_connect -> {
                    when(bleManager.connectionState) {
                        BleGattManager.State.Connected -> {
                            bleManager.close()
                        }
                        BleGattManager.State.Disconnected -> {
                            mainActivityViewModel.currentDevice?.let { device ->
                                bleManager.connect(device.address)
                            }
                        }
                        BleGattManager.State.Error -> {

                        }
                        else -> {

                        }
                    }

                    true
                }
                R.id.action_scanner -> {
                    bleManager.close()
                    findNavController().navigate(R.id.action_ServicesFragment_to_ScanFragment)
                    true
                }
                else -> {
                    true
                }
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _bleManager = (requireContext().applicationContext as BleApplication).bleManager
        _binding = FragmentServicesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initServicesFragment()
        linkMenu(true)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        bleManager.close()
        linkMenu(false)
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

    private fun linkMenu(link: Boolean) {
        val menuHost: MenuHost = requireActivity()
        if (link) {
            menuHost.addMenuProvider(servicesMenuProvider)
        } else {
            menuHost.removeMenuProvider(servicesMenuProvider)
        }
    }

    private fun initServicesFragment() {
        Log.d(logTag, "BluetoothDevice ${mainActivityViewModel.currentDevice}")
        binding.apply {
            rvServices.adapter = servicesAdapter
            rvServices.layoutManager = LinearLayoutManager(requireContext())
            btnConnect.setOnClickListener {
                if (bleManager.connectionState == BleGattManager.State.Connected) {
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

            lifecycleScope.launch {
                bleManager.flowBondState.collect {
                    visibleBondButton()
                }
            }
        }

        fillBleDevice(mainActivityViewModel.currentDevice)
    }

    private fun visibleBondButton() {
        binding.apply {
            btnBond.isVisible =
                mainActivityViewModel.currentDevice?.let { bluetoothDevice ->
                    bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED
                } ?: false
        }
    }
}

