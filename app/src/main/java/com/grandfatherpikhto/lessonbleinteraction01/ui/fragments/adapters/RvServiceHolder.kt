package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters

import android.bluetooth.BluetoothGattService
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.lessonbleinteraction01.databinding.LayoutServiceBinding

class RvServiceHolder constructor(private val view: View) : RecyclerView.ViewHolder(view) {
    private val binding = LayoutServiceBinding.bind(view)
    private val rvCharacteristicAdapter = RvCharacteristicAdapter()

    fun bind(bluetoothService: BluetoothGattService) {
        binding.apply {
            tvService.text = bluetoothService.uuid.toString()
            rvCharacteristics.adapter = rvCharacteristicAdapter
            rvCharacteristics.layoutManager = LinearLayoutManager(view.context)
            rvCharacteristicAdapter.bluetoothGattService = bluetoothService
        }
    }
}