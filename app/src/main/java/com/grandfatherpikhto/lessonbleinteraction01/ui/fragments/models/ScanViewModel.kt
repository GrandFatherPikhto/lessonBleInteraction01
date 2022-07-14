package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.blin.BleScanManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {
    private val mutableStateFlowDevice = MutableStateFlow<BluetoothDevice?>(null)
    val flowDevice get() = mutableStateFlowDevice.asStateFlow()
    val device get() = mutableStateFlowDevice.value

    private val mutableStateFlowError = MutableStateFlow(-1)
    val sfError get() = mutableStateFlowDevice.asStateFlow()
    val error get() = mutableStateFlowDevice.value

    init {
    }

    fun changeDevice(bluetoothDevice: BluetoothDevice) {
            mutableStateFlowDevice.tryEmit(bluetoothDevice)
    }

    fun changeScanError(errorCode: Int) {
        mutableStateFlowError.tryEmit(errorCode)
    }
}