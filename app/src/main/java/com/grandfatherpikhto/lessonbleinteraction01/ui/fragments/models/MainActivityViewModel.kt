package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivityViewModel : ViewModel() {
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    val devices get() = bluetoothDevices.toList()

    private val msfCurrentDevice = MutableStateFlow<BluetoothDevice?>(null)
    val flowCurrentDevice get() = msfCurrentDevice.asStateFlow()
    val currentDevice get() = msfCurrentDevice.value

    fun changeCurrentDevice(bluetoothDevice: BluetoothDevice) {
        msfCurrentDevice.tryEmit(bluetoothDevice)
    }

    init {

    }
}