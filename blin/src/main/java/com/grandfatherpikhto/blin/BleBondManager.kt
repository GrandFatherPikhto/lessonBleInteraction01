package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.receiver.BcBondReceiver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.properties.Delegates

class BleBondManager constructor(private val context: Context,
                                 private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private val scope = CoroutineScope(dispatcher)

    private var requestDevice:BluetoothDevice? = null

    enum class State (val value: Int) {
        None(0x00),
        Bonding(0x01),
        Bondend(0x02),
        Reject(0x03),
        Error(0x04)
    }

    private val msfState = MutableStateFlow(State.None)
    val flowState get() = msfState.asStateFlow()
    val state get() = msfState.value

    private val bcBondReceiver by lazy {
        BcBondReceiver(this, dispatcher)
    }

    init {
        context.applicationContext.registerReceiver(bcBondReceiver,
            makeIntentFilter())
    }

    fun onDestroy() {
        context.applicationContext.unregisterReceiver(bcBondReceiver)
    }

    fun bondRequest(bluetoothDevice: BluetoothDevice) {
        if(bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
            msfState.tryEmit(State.Bondend)
        } else {
            requestDevice = bluetoothDevice
            if (bluetoothDevice.createBond()) {
                msfState.tryEmit(State.Bonding)
            } else {
                msfState.tryEmit(State.Error)
            }
        }
    }

    private fun makeIntentFilter() = IntentFilter().let { intentFilter ->
        intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        intentFilter
    }

    /**
      * BluetoothDevice.BOND_NONE    10
      * BluetoothDevice.BOND_BONDING 11
      * BluetoothDevice.BOND_BONDED  12
      */
    fun onSetBondingDevice(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {
        bluetoothDevice?.let { device ->
            if (device == requestDevice) {
                if (newState == BluetoothDevice.BOND_BONDED) {
                    msfState.tryEmit(State.Bondend)
                } else {
                    msfState.tryEmit(State.Reject)
                }
            }
        }
    }
}