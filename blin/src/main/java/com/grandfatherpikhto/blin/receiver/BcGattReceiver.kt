package com.grandfatherpikhto.blin.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.grandfatherpikhto.blin.BleGattManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BcGattReceiver constructor(private val bleGattManager: BleGattManager,
                                 private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    val previousBondState: Int =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    val bluetoothDevice: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    bluetoothDevice?.let {
                        bleGattManager.onBondStateChanged(it, previousBondState, bondState)
                    }
                    // BluetoothDevice.BOND_NONE    10
                    // BluetoothDevice.BOND_BONDING 11
                    // BluetoothDevice.BOND_BONDED  12
                    // Log.d(TAG, "ACTION_BOND_STATE_CHANGED(${device?.address}): $previousBondState => $bondState")
                }
                else -> {

                }
            }
        }
    }
}