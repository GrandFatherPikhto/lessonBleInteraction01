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
    private val scope = CoroutineScope(dispatcher)

    private var address: String? = null

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

    val flowScannedDevice get() = scanner.flowDevice
    val scannedDevice get() = scanner.device

    val flowConnectionState get() = connector.flowConnectionState
    val connectionState get() = connector.connectionState
    // Не забывай про то, что свойство не последовательность, ей геттер нужен!
    val gatt:BluetoothGatt? get() = connector.gatt
    val flowDevice get() = scanner.flowDevice
    val flowScanError get() = scanner.flowError
    val flowBondState get() = bleBondManager.flowState
    val bondState get() = bleBondManager.state

    val currentDevice get() = connector.device

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        bleGattManager.onDestroy()
        bleScanManager.onDestroy()
        bleBondManager.onDestroy()
    }

    fun startScan(addresses: List<String> = listOf(),
                  names: List<String> = listOf(),
                  services: List<String> = listOf(),
                  stopOnFind: Boolean = false,
                  filterRepeatable: Boolean = true,
                  stopTimeout: Long = 0L,
                  clearDevices:Boolean = true
    ) : Boolean {
        if (connector.connectionState == BleGattManager.State.Connected) {
            connector.disconnect()
        }

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
        this.address = address
        if (scanner.state == BleScanManager.State.Scanning) {
            scanner.stopScan()
        }

        return connector.connect(address)
    }

    fun close() = connector.disconnect()

    fun bondRequest(bluetoothDevice: BluetoothDevice)
        = bleBondManager.bondRequest(bluetoothDevice)

    fun getBluetoothDevice(address: String) : BluetoothDevice? {
        return adapter.getRemoteDevice(address)
    }
}