package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.lessonbleinteraction01.databinding.LayoutCharacteristicBinding
import kotlin.math.log

class RvCharacteristicHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val logTag = this.javaClass.simpleName
    private val binding = LayoutCharacteristicBinding.bind(view)

    fun bind(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        binding.apply {
            tvCharacteristic.text = bluetoothGattCharacteristic.uuid.toString()
            val value = bluetoothGattCharacteristic.value?.let { value ->
                value.toUByteArray()
                    .joinToString(" ") { String.format("%02X") }
            } ?: ""
            // Log.d(logTag, "Value: $value")
            tvValue.text = value
        }
    }
}