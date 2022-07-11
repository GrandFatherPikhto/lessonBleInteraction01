package com.grandfatherpikhto.blin.receiver

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.grandfatherpikhto.blin.BleScanManager

class BcScanReceiver constructor(private val context: Context,
                                 private val bleScanManager: BleScanManager) : BroadcastReceiver() {
    companion object Receiver {
        const val ACTION_BLE_SCAN = "com.grandfatherpikhto.lessonbleinteraction01.ACTION_BLE_SCAN"
        const val REQUEST_CODE_BLE_SCANNER_PENDING_INTENT = 1000
    }

    private val logTag = this.javaClass.simpleName
    private var bcPendingIntent:PendingIntent? = null
    private val applicationContext:Context = context.applicationContext

    val pendingIntent get() = bcPendingIntent

    init {
        bcPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            REQUEST_CODE_BLE_SCANNER_PENDING_INTENT,
            Intent(ACTION_BLE_SCAN),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        applicationContext.registerReceiver(this, makeIntentFilters())
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            when (intent.action) {
                ACTION_BLE_SCAN -> {
                    bleScanManager.onScanReceived(intent)
                }
                else -> {
                    Log.d(logTag, "Action: ${intent.action}")
                }
            }
        }
    }

    private fun makeIntentFilters() : IntentFilter = IntentFilter().let { intentFilter ->
        intentFilter.addAction(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(BcScanReceiver.ACTION_BLE_SCAN)
        intentFilter
    }

    fun destroy() {
        applicationContext.unregisterReceiver(this)
    }
}