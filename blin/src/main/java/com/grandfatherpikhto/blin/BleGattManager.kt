package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.receiver.BcGattReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BleGattManager constructor(private val context: Context,
                                 private val bleScanManager: BleScanManager,
                                 private val dispatcher: CoroutineDispatcher = Dispatchers.IO)
        : DefaultLifecycleObserver {
    companion object {

    }

    private val logTag = this.javaClass.simpleName
    private val bluetoothManager:BluetoothManager
            = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bleGattCallback  = BleGattCallback(this, dispatcher)
    private var bluetoothDevice:BluetoothDevice? = null
    private var reconnectOnError = true
    private var reconnectCounter = 0
    private val mutexClose = Mutex(locked = false)
    private var scope = CoroutineScope(dispatcher)
    private val maxReconnect = 6
    private var waitForBound = false
    private val bcGattReceiver = BcGattReceiver(this)

    private val mutableStateFlowConnected = MutableStateFlow(false)
    val flowConnected get() = mutableStateFlowConnected.asStateFlow()
    val connected get() = mutableStateFlowConnected.value

    private val msfConnectionState  = MutableStateFlow(-1)
    val flowConnectionState get() = msfConnectionState.asStateFlow()
    val connectionState get() = msfConnectionState.value

    private val msfConnectionStatus  = MutableStateFlow(-1)
    val flowConnectionStatus get() = msfConnectionState.asStateFlow()
    val connectionStatus get() = msfConnectionStatus.value

    private var bluetoothGatt:BluetoothGatt? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        context.applicationContext.registerReceiver(bcGattReceiver,
            makeIntentFilter())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        disconnect()
        context.applicationContext.unregisterReceiver(bcGattReceiver)
    }

    init {

    }

    fun connect(address:String) {
        bleScanManager.stopScan()
        bluetoothAdapter?.getRemoteDevice(address).let { device ->
            bluetoothDevice = device
            connect()
        }
    }

    /**
     * Если устройство не сопряжено, сопрягаем его и ждём оповещение сопряжения
     * после получения, повторяем попытку подключения.
     */
    private fun connect() {
        reconnectOnError = true
        bluetoothDevice?.let { device ->
            if (device.bondState
                == BluetoothDevice.BOND_NONE) {
                Log.d(logTag, "Пытаемся сопрячь устройство ${device.address}")
                waitForBound = true
                device.createBond()
            } else {
                doConnect()
            }
        }
    }

    private fun doConnect() {
        bluetoothDevice?.let { device ->
            Log.d(logTag, "Попытка подключения $reconnectCounter к ${device.address}")
            device.connectGatt(
                context,
                device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                bleGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }
    }

    private fun doRescan() {
        doDisconnect()

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
     * Получает от рессивера подтверждение сопряжения и повторяет попытку подключения
     * BluetoothDevice.BOND_NONE    10
     * BluetoothDevice.BOND_BONDING 11
     * BluetoothDevice.BOND_BONDED  12
     */
    fun onBondStateChanged(bluetoothDevice: BluetoothDevice, prevBondState: Int, bondState: Int) {
        if (waitForBound && bluetoothDevice == this.bluetoothDevice) {
            waitForBound = false
            connect()
        }
    }

    /**
     * Интерфейсы исследованы. Сбрасываем счётчик переподключений и
     * генерируем событие о полном подключении. Можно принимать и передавать данные
     */
    fun onGattDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt?.let {
                bluetoothGatt = it
                Log.d(logTag, "Services discovered $bluetoothDevice")
                mutableStateFlowConnected.tryEmit(true)
                reconnectCounter = 0
            }
        }
    }

    fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        msfConnectionState.tryEmit(newState)
        msfConnectionStatus.tryEmit(status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(logTag, "Device $bluetoothDevice disconnected")
                    if (mutexClose.isLocked) {
                        mutexClose.unlock(this)
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    reconnectCounter ++
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(logTag, "Connected: $bluetoothDevice")
                    gatt?.let {
                        if(!it.discoverServices()) {
                            doDisconnect()
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
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
            if (reconnectOnError) {
                if (reconnectCounter < maxReconnect) {
                    doConnect()
                } else {
                    disconnect()
                }
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
}