package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuHost
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.blin.BleScanManager
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.databinding.FragmentScanBinding
import com.grandfatherpikhto.lessonbleinteraction01.ui.MainActivity
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters.RvBtAdapter
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.MainActivityViewModel
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.ScanViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val rvBtAdapter = RvBtAdapter()
    private val logTag = this.javaClass.simpleName

    private val mainActivityViewModel:MainActivityViewModel by activityViewModels<MainActivityViewModel>()
    private val scanViewModel:ScanViewModel by viewModels<ScanViewModel> ()

    private val bleScanManager:BleScanManager by lazy {
        // (requireActivity().application as BleApplication).bleScanManager
        (requireActivity() as MainActivity).bleScanManager
    }

    private val scanMenuProvider:ScanMenuProvider by lazy {
        ScanMenuProvider(requireContext(), lifecycleScope, bleScanManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentScanBinding.inflate(inflater, container, false)

        rvBtAdapter.setItems(bleScanManager.devices)

        binding.apply {
            rvBtDevices.adapter = rvBtAdapter
            rvBtDevices.layoutManager = LinearLayoutManager(requireContext())
        }

        rvBtAdapter.setOnClickListener { bluetoothDevice, _ ->
            mainActivityViewModel.changeCurrentDevice(bluetoothDevice)
            val navController = findNavController()
            val currentDestination = navController.currentDestination
            lifecycleScope.launch {
                if (currentDestination?.id == R.id.ScanFragment) {
                    navController.navigate(R.id.action_ScanFragment_to_ServicesFragment)
                }
            }
        }

        lifecycleScope.launch {
            scanViewModel.sfDevice.filterNotNull().collect { bluetoothDevice ->
                rvBtAdapter.addItem(bluetoothDevice)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addScanMenu()
    }

    override fun onPause() {
        bleScanManager.stopScan()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        val menuHost: MenuHost = requireActivity()
        menuHost.removeMenuProvider(scanMenuProvider)
    }

    private fun addScanMenu() {
        Log.d(logTag, "addScanMenu($bleScanManager)")
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(scanMenuProvider)
    }
}