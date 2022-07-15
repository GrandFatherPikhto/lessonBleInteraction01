package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.clickHandler
import com.grandfatherpikhto.lessonbleinteraction01.longClickHandler
import kotlin.properties.Delegates

class RvServicesAdapter : RecyclerView.Adapter<RvServiceHolder>() {

    var bluetoothGatt:BluetoothGatt? by Delegates.observable(null) { _, oldValue, newValue ->
        if (newValue == null) {
            notifyItemRangeRemoved(0, oldValue?.services?.size ?: 0)
        } else {
            notifyItemRangeInserted(0, newValue?.services?.size ?: 0)
        }
    }

    private var handlerClick: clickHandler<BluetoothGattService>? = null
    private var handlerLongClick: longClickHandler<BluetoothGattService>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvServiceHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_service, parent, false)

        return RvServiceHolder(view)
    }

    override fun onBindViewHolder(holder: RvServiceHolder, position: Int) {
        bluetoothGatt?.let { gatt ->
            gatt.services?.let { services ->
                holder.itemView.setOnClickListener { view ->
                    handlerClick?.let { it(services[position], view) }
                }

                holder.itemView.setOnLongClickListener { view ->
                    handlerLongClick?.let { it(services[position], view) }
                    true
                }

                holder.bind(services[position])
            }
        }
    }

    override fun getItemCount(): Int = bluetoothGatt?.services?.size ?: 0
}