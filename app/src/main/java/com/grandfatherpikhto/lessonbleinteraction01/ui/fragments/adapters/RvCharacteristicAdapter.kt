package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.clickHandler
import com.grandfatherpikhto.lessonbleinteraction01.longClickHandler
import kotlin.properties.Delegates

class RvCharacteristicAdapter : RecyclerView.Adapter<RvCharacteristicHolder> () {
    private val logTag = this.javaClass.simpleName
    var bluetoothGattService:BluetoothGattService?
        by Delegates.observable(null) { _, oldValue, newValue ->
            if (newValue == null) {
                notifyItemRangeRemoved(0, oldValue?.characteristics?.size ?: 0)
            } else {
                notifyItemRangeInserted(0, newValue.characteristics?.size ?: 0)
                // Log.d(logTag, "size: ${newValue?.characteristics?.size ?: 0}")
            }
    }
    private var handlerClick: clickHandler<BluetoothGattCharacteristic>? = null
    private var handlerLongClick: longClickHandler<BluetoothGattCharacteristic>? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvCharacteristicHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_characteristic, parent, false)
        return RvCharacteristicHolder(view)
    }

    override fun onBindViewHolder(holder: RvCharacteristicHolder, position: Int) {
        bluetoothGattService?.let { service ->
            service?.characteristics?.let { characteristics ->
                holder.itemView.setOnClickListener { view ->
                    handlerClick?.let { it(characteristics[position], view) }
                }

                holder.itemView.setOnLongClickListener { view ->
                    handlerLongClick?.let { it(characteristics[position], view) }
                    true
                }

                holder.bind(characteristics[position])
            }
        }
    }

    override fun getItemCount(): Int = bluetoothGattService?.characteristics?.size ?: 0
}
