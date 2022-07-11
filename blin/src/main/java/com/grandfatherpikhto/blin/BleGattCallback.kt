package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleGattCallback constructor(private val bleGattManager: BleGattManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO)
        : BluetoothGattCallback() {
    companion object Receiver {
    }

    private val logTag = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.e(logTag, "gatt: $gatt, $status, $newState")
        bleGattManager.onConnectionStateChange(gatt, status, newState)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        bleGattManager.onGattDiscovered(gatt, status)
    }
}