package com.grandfatherpikhto.blin

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BleManager constructor(private val context: Context,
                             dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    enum class State (value: Int) {
        None(0x00),
        Scanning(0x01),
        StopScan(0x02),
        Rescan(0x03),
        Connecting(0x04),
        Connected(0x05),
        Error(0xff)
    }

        companion object {

        }

    private val logTag = this.javaClass.simpleName


}