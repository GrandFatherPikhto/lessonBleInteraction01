package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.grandfatherpikhto.blin.BleScanManager
import com.grandfatherpikhto.lessonbleinteraction01.R
import kotlinx.coroutines.launch

class ScanMenuProvider (
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val bleScanManager: BleScanManager) : MenuProvider {

    private val logTag = this.javaClass.simpleName
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        Log.d(logTag, "onCreateMenu()")
        menuInflater.inflate(R.menu.menu_scan, menu)
        menu.findItem(R.id.action_scan)?.let { actionScan ->
            lifecycleScope.launch {
                BleScanManager.stateFlowScanning.collect { scanning ->
                    if (scanning) {
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
        Log.d(logTag, "onMenuItemSelected()")
        return when(menuItem.itemId) {
            R.id.action_scan -> {
                if (BleScanManager.valueScanning) {
                    bleScanManager?.stopScan()
                } else {
                    Log.d(logTag, "action_scan $bleScanManager")
                    bleScanManager?.startScan(stopTimeout = 10000L)
                }
                true
            }
            else -> { true }
        }
    }
}
