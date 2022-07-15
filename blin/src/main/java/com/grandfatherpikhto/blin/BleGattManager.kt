package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class BleGattManager constructor(private val bleManager: BleManager,
                                 private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    enum class State(val value:Int) {
        Disconnected  (0x00), // Отключены
        Disconnecting (0x01), // Отключаемся
        Connecting    (0x02), // Подключаемся
        Connected     (0x02), // Подключены
        Error         (0xFF), // Получена ошибка
    }

    private val logTag = this.javaClass.simpleName
    private val bluetoothAdapter = bleManager.adapter
    private val bleGattCallback  = BleGattCallback(this, dispatcher)
    private var bluetoothDevice:BluetoothDevice? = null
    val device get() = bluetoothDevice
    private var scope = CoroutineScope(dispatcher)

    private var attemptReconnect = true
    private var reconnectAttempts = 0
    private val maxAttempts = 6

    private val msfConnectionState  = MutableStateFlow(State.Disconnected)
    val flowConnectionState get() = msfConnectionState.asStateFlow()
    val connectionState get() = msfConnectionState.value

//    private val msfBluetoothGatt = MutableStateFlow<BluetoothGatt?>(null)
//    val flowGatt get() = msfBluetoothGatt.asStateFlow()
//    val bluetoothGatt:BluetoothGatt? get() = msfBluetoothGatt.value
    private var bluetoothGatt:BluetoothGatt? = null
    val gatt get() = bluetoothGatt


    fun onDestroy() {
        disconnect()
    }

    init {
        scope.launch {
            bleManager.flowScanState.collect() { state ->
                if (attemptReconnect
                    && state == BleScanManager.State.Stopped
                    && bleManager.scannedDevice != null
                    && bluetoothDevice == bleManager.scannedDevice
                ) {
                    doConnect()
                }
            }
        }
    }

    /**
     *
     */
    private fun doRescan() {
        Log.d(logTag, "doRescan()")
        if (attemptReconnect && reconnectAttempts < maxAttempts) {
            bluetoothDevice?.let { device ->
                bleManager.startScan(addresses = listOf(device.address),
                    stopTimeout = 2000L,
                    stopOnFind = true)
            }
        }
    }

    fun connect(address:String) : BluetoothGatt? {
        Log.d(logTag, "connect($address)")
        bluetoothAdapter.getRemoteDevice(address)?.let { device ->
            msfConnectionState.tryEmit(State.Connecting)
            bluetoothDevice = device
            attemptReconnect = true
            reconnectAttempts = 0
            doConnect()
        }

        return null
    }

    private fun doConnect() : BluetoothGatt? {
        bluetoothDevice?.let { device ->
            Log.d(logTag, "doConnect($device), reconnect = $attemptReconnect, attempts = $reconnectAttempts")
            if (attemptReconnect) {
                reconnectAttempts ++
            }
            return device.connectGatt(
                bleManager.applicationContext,
                device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                bleGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }

        return null
    }

    /**
     * Даёт команду на отключение подключения (gatt.disconnect()).
     * Если статус Disconnected, сразу закрывает подключение gatt.close()
     * Если нет, блокирует поток и ждёт, пока не будет получено состояние
     * Disconnected и после этого закрыает подключение
     * Это нужно для того, чтобы сбросить счётчик подключений
     * Если он переполнится, нужно будет очищать кэш Bluetooth
     */
    fun disconnect() {
        Log.d(logTag, "disconnect()")
        attemptReconnect = false
        reconnectAttempts = 0
        msfConnectionState.tryEmit(State.Disconnecting)
        doDisconnect()
    }

    private fun doDisconnect() = runBlocking {
        bluetoothGatt?.let { gatt ->
            Log.d(logTag, "doDisconnect($bluetoothDevice)")
            gatt.disconnect()
            while (connectionState != State.Disconnected) {
                delay(100)
            }
            gatt.close()
        }
    }

    /**
     * Интерфейсы исследованы. Сбрасываем счётчик переподключений и
     * генерируем событие о полном подключении. Можно принимать и передавать данные
     */
    fun onGattDiscovered(discoveredGatt: BluetoothGatt?, status: Int) {
        Log.d(logTag, "onGattDiscovered(status = $status)")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            discoveredGatt?.let {
                bluetoothGatt = it
                msfConnectionState.tryEmit(State.Connected)
            }
        }
    }

    fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_DISCONNECTED -> {
                    msfConnectionState.tryEmit(State.Disconnected)
                    bluetoothGatt = null
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.let {
                        if(!it.discoverServices()) {
                            msfConnectionState.tryEmit(State.Error)
                            attemptReconnect  = false
                            reconnectAttempts = 0
                            doDisconnect()
                        }
                    }
                }
                else -> {
                    // Log.e(logTag, "Unknown newState: $newState")
                }
            }
        } else {
            if (attemptReconnect) {
                if (reconnectAttempts < maxAttempts) {
                    doRescan()
                } else {
                    msfConnectionState.tryEmit(State.Error)
                    attemptReconnect  = false
                    reconnectAttempts = 0
                }
            }
        }
    }
}