package com.grandfatherpikhto.blin.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BcGattReceiver constructor(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : BroadcastReceiver() {

    private val logTag = this.javaClass.simpleName
    private var bondingDevice:BluetoothDevice? = null

    enum class State (val value: Int) {
        Unknown(0x00),
        Bonding(0x01),
        Bondend(0x02),
        Reject(0x03),
        Error(0x04)
    }

    private val msfState = MutableStateFlow(State.Unknown)
    val flowState get() = msfState.asStateFlow()
    val state get() = msfState.value


    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    val previousBondState: Int =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    val bluetoothDevice: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // BluetoothDevice.BOND_NONE    10
                    // BluetoothDevice.BOND_BONDING 11
                    // BluetoothDevice.BOND_BONDED  12
                    // Log.d(TAG, "ACTION_BOND_STATE_CHANGED(${device?.address}): $previousBondState => $bondState")
                    if (bondingDevice != null && bluetoothDevice != null &&
                            bondingDevice == bluetoothDevice) {
                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                msfState.tryEmit(State.Bondend)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                msfState.tryEmit(State.Reject)
                            }
                            else -> {
                                Log.d(logTag, "Unknown state: $bondState")
                            }
                        }
                    }
                }
                else -> {

                }
            }
        }
    }

    fun bondRequest(bluetoothDevice: BluetoothDevice) {
        if(bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
            bondingDevice = bluetoothDevice
            if (bluetoothDevice.createBond()) {
                msfState.tryEmit(State.Bonding)
            } else {
                msfState.tryEmit(State.Error)
            }
        }
    }
 }