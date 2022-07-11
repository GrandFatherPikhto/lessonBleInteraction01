package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.clickHandler
import com.grandfatherpikhto.lessonbleinteraction01.longClickHandler

class RvBtAdapter : RecyclerView.Adapter<RvBtHolder>() {
    private val devices = mutableListOf<BluetoothDevice>()

    private var handlerClick: clickHandler<BluetoothDevice>? = null
    private var handlerLongClick: longClickHandler<BluetoothDevice>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvBtHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_device, parent, false)

        return RvBtHolder(view)
    }

    override fun onBindViewHolder(holder: RvBtHolder, position: Int) {
        holder.itemView.setOnClickListener { view ->
            handlerClick?.let { it(devices[position], view) }
        }

        holder.itemView.setOnLongClickListener { view ->
            handlerLongClick?.let { it(devices[position], view) }
            true
        }
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    fun setOnClickListener(clickListener : clickHandler<BluetoothDevice>) {
        handlerClick = clickListener
    }

    fun setOnLongClickListener (longClickListener : longClickHandler<BluetoothDevice>) {
        handlerLongClick = longClickListener
    }

    fun addItem(bluetoothDevice: BluetoothDevice) {
        if (!devices.contains(bluetoothDevice)) {
            devices.add(bluetoothDevice)
            notifyItemInserted(devices.size - 1)
        }
    }

    fun removeItems() {
        val size = devices.size
        devices.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun addItems(bluetoothDevices: List<BluetoothDevice>) {
        val size = if (devices.isEmpty()) 0 else devices.size
        if (bluetoothDevices.isNotEmpty()) {
            // devices.addAll(bluetoothDevices)
            bluetoothDevices.forEach { bluetoothDevice ->
                if (!devices.contains(bluetoothDevice)) {
                    devices.add(bluetoothDevice)
                }
            }
            notifyItemRangeInserted(size - 1, bluetoothDevices.size)
        }
    }

    fun setItems(bluetoothDevices: List<BluetoothDevice>) {
        val size = devices.size
        devices.clear()
        if (bluetoothDevices.isNotEmpty()) {
            devices.addAll(bluetoothDevices)
            notifyItemRangeInserted(0, bluetoothDevices.size)
        } else {
            notifyItemRangeRemoved(0, size)
        }
    }
}