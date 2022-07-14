package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class BleRescanManager constructor(private val bleScanManager: BleScanManager,
                                   private val dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    enum class State(val value: Int) {
        None(0x00),
        Rescanning(0x01),
        Rescanned(0x02),
        Error(0xff)
    }

    private var expectedDevice: BluetoothDevice? = null

    private val scope = CoroutineScope(dispatcher)

    private val msfState = MutableStateFlow(State.None)
    val flowState get() = msfState.asStateFlow()
    val state get() = msfState.value

    init {
        scope.launch {
            BleScanManager.stateFlowScanning.collect { scanning ->
                if (state == State.Rescanning && !scanning) {
                    msfState.tryEmit(State.Error)
                }
            }
        }

        scope.launch {
            BleScanManager.stateFlowDevice.filterNotNull().collect { bluetoothDevice ->
                if (expectedDevice == bluetoothDevice) {
                    msfState.tryEmit(State.Rescanned)
                }
            }
        }
    }

    fun rescan(bluetoothDevice: BluetoothDevice) {
        if (bleScanManager.startScan(
            addresses = listOf(bluetoothDevice.address),
            stopTimeout = 1000L,
            stopOnFind = true )) {
            expectedDevice = bluetoothDevice
            msfState.tryEmit(State.Rescanning)
        } else {
            msfState.tryEmit(State.Error)
        }
    }
}