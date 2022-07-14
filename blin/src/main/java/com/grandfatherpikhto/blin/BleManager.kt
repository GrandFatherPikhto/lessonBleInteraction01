package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BleManager constructor(private val context: Context,
                             private val dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    companion object {

    }

    private val logTag = this.javaClass.simpleName
    private val attempts = 6
    private val scope = CoroutineScope(dispatcher)

    private var address: String? = null
    private var doReconnect = false
    private var tryReconnect = 0

    private val bluetoothManager: BluetoothManager
            = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)
            as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    val adapter: BluetoothAdapter get() = bluetoothAdapter

    private val bleScanManager:BleScanManager = BleScanManager(this, dispatcher)
    val scanner:BleScanManager get() = bleScanManager

    private val bleGattManager:BleGattManager = BleGattManager(this, dispatcher)
    val connector:BleGattManager get() = bleGattManager

    private val bleBondManager = BleBondManager(context, dispatcher)
    val applicationContext: Context get() = context.applicationContext
    val flowScanState get() = scanner.flowState
    val scanState get() = scanner.state
    val flowConnectionState get() = connector.flowConnectionState
    val connectState get() = connector.connectionState
    // Не забывай про то, что свойство не последовательность, ей геттер нужен!
    val gatt:BluetoothGatt? get() = connector.gatt
    val flowDevice get() = scanner.flowDevice
    val flowScanError get() = scanner.flowError
    val flowBondState get() = bleBondManager.flowState
    val bondState get() = bleBondManager.state

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        scope.launch {
            connector.flowConnectionState.collect { state ->
                if (state == BleGattManager.State.Error) {
                    address?.let { bluetoothAddress ->
                        if (doReconnect) {
                            scanner.startScan(
                                addresses = listOf(bluetoothAddress),
                                stopOnFind = true
                            )
                        }
                    }
                }
            }
        }

        scope.launch {
            scanner.flowState.collect { state ->
                if (state == BleScanManager.State.Stopped && doReconnect && tryReconnect < attempts) {
                    address?.let { bluetoothAddress ->
                        tryReconnect ++
                        connector.connect(bluetoothAddress)
                    }
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        bleGattManager.onDestroy(owner)
        bleScanManager.onDestroy(owner)
        bleBondManager.onDestroy(owner)
    }

    fun startScan(addresses: List<String> = listOf(),
                  names: List<String> = listOf(),
                  services: List<String> = listOf(),
                  stopOnFind: Boolean = false,
                  filterRepeatable: Boolean = true,
                  stopTimeout: Long = 0L,
                  clearDevices:Boolean = true
    ) : Boolean {
        if (connector.connectionState == BleGattManager.State.Discovered) {
            connector.disconnect()
        }

        doReconnect = false

        return scanner.startScan(addresses,
            names,
            services,
            stopOnFind,
            filterRepeatable,
            stopTimeout,
            clearDevices)
    }

    fun stopScan() = scanner.stopScan()

    fun connect(address: String): BluetoothGatt? {
        doReconnect = true
        if (scanner.state == BleScanManager.State.Scanning) {
            scanner.stopScan()
        }

        return connector.connect(address)
    }

    fun close() {
        connector.disconnect()
    }

    fun bondRequest(bluetoothDevice: BluetoothDevice)
        = bleBondManager.bondRequest(bluetoothDevice)
}