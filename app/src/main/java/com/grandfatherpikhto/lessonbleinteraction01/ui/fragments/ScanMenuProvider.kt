package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.lifecycle.LifecycleCoroutineScope
import com.grandfatherpikhto.blin.BleManager
import com.grandfatherpikhto.blin.BleScanManager
import com.grandfatherpikhto.lessonbleinteraction01.BleApplication
import com.grandfatherpikhto.lessonbleinteraction01.R
import kotlinx.coroutines.launch

class ScanMenuProvider (
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) : MenuProvider {

    private val logTag = this.javaClass.simpleName
    private val bleManager: BleManager by lazy {
        (context.applicationContext as BleApplication).bleManager!!
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        Log.d(logTag, "onCreateMenu()")
        menuInflater.inflate(R.menu.menu_scan, menu)
        menu.findItem(R.id.action_scan)?.let { actionScan ->
            lifecycleScope.launch {
                bleManager.flowScanState.collect { state ->
                    if (state == BleScanManager.State.Scanning) {
                        actionScan.title = context.getString(R.string.action_stop_scan)
                        actionScan.setIcon(R.drawable.ic_baseline_man_24)
                    } else {
                        actionScan.title = context.getString(R.string.action_scan)
                        actionScan.setIcon(R.drawable.ic_baseline_directions_run_24)
                    }
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.action_scan -> {
                if (bleManager.scanState == BleScanManager.State.Scanning) {
                    bleManager.stopScan()
                } else {
                    bleManager.startScan(stopTimeout = 10000L)
                }
                true
            }
            else -> { true }
        }
    }
}
