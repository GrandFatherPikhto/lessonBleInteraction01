package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.log

class BleGattManager constructor(private val bleManager: BleManager,
                                 private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    enum class State(val value:Int) {
        Disconnected  (0x00),  // Отключены
        Disconnecting (0x01),  // Отключение от GATT
        Connecting    (0x02),  // Процесс подключения к GATT
        Connected     (0x03),  // Подключены
        Discovering   (0x04),  // Начали исследовать сервисы
        Discovered    (0x05),  // Сервисы исследованы
        Error         (0xFF),  // Получена ошибка
    }

    private val logTag = this.javaClass.simpleName
    private val bluetoothAdapter = bleManager.adapter
    private val bleGattCallback  = BleGattCallback(this, dispatcher)
    private var bluetoothDevice:BluetoothDevice? = null
    private var reconnectOnError = true
    private var reconnectCounter = 0
    private val mutexClose = Mutex(locked = false)
    private var scope = CoroutineScope(dispatcher)

    private val msfConnectionState  = MutableStateFlow(State.Disconnected)
    val flowConnectionState get() = msfConnectionState.asStateFlow()
    val connectionState get() = msfConnectionState.value

//    private val msfConnectionStatus  = MutableStateFlow(-1)
//    val flowConnectionStatus get() = msfConnectionState.asStateFlow()
//    val connectionStatus get() = msfConnectionStatus.value

    private var bluetoothGatt:BluetoothGatt? = null
    val gatt get() = bluetoothGatt


    fun onDestroy(owner: LifecycleOwner) {
        disconnect()
    }

    init {
        scope.launch {
            bleManager.scanner.flowDevice.filterNotNull().collect { device ->
                if (device == bluetoothDevice && reconnectOnError) {
                    connect()
                }
            }
        }
    }

    fun connect(address:String) : BluetoothGatt? {
        bluetoothAdapter.getRemoteDevice(address).let { device ->
            bluetoothDevice = device
            return connect()
        }
    }

    /**
     * Если устройство не сопряжено, сопрягаем его и ждём оповещение сопряжения
     * после получения, повторяем попытку подключения.
     */
    private fun connect() : BluetoothGatt? {
        reconnectOnError = true
        bluetoothDevice?.let { device ->
            Log.d(logTag, "Попытка подключения $reconnectCounter к ${device.address}")
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
    private fun doDisconnect() = runBlocking {
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
                launch {
                    mutexClose.withLock {
                        gatt.close()
                    }
                }
            }
        }


    /**
     * Дождаться состояния Disconnect.
     * Если этого не сделать, устройство в течение 30-180 секунд
     * будет недоступно для повторного подключения и сканирования
     */
    fun disconnect() {
        reconnectOnError = false
        Log.d(logTag, "Disconnect $bluetoothDevice")
        doDisconnect()
    }

    /**
     * Интерфейсы исследованы. Сбрасываем счётчик переподключений и
     * генерируем событие о полном подключении. Можно принимать и передавать данные
     */
    fun onGattDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt?.let {
                bluetoothGatt = it
                msfConnectionState.tryEmit(State.Discovered)
            }
        }
    }

    fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//        msfConnectionState.tryEmit(newState)
//        msfConnectionStatus.tryEmit(status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_DISCONNECTED -> {
                    msfConnectionState.tryEmit(State.Disconnected)
                    Log.d(logTag, "Device $bluetoothDevice disconnected")
                    if (mutexClose.isLocked) {
                        mutexClose.unlock(this)
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    msfConnectionState.tryEmit(State.Connecting)
                    reconnectCounter ++
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(logTag, "Connected: $bluetoothDevice")
                    msfConnectionState.tryEmit(State.Connected)
                    gatt?.let {
                        msfConnectionState.tryEmit(State.Discovering)
                        if(!it.discoverServices()) {
                            msfConnectionState.tryEmit(State.Error)
                            doDisconnect()
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    msfConnectionState.tryEmit(State.Disconnecting)
                    scope.launch {
                        if (!mutexClose.isLocked) {
                            mutexClose.lock(this)
                        }
                    }
                }
                else -> {
                    Log.e(logTag, "Unknown newState: $newState")
                }
            }
        } else {
            msfConnectionState.tryEmit(State.Error)
        }
    }
}