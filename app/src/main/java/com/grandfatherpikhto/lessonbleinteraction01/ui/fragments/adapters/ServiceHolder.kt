package com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.adapters

import android.bluetooth.BluetoothGattService
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.lessonbleinteraction01.databinding.LayoutServiceBinding

class ServiceHolder constructor(private val view: View) : RecyclerView.ViewHolder(view) {
    private val binding = LayoutServiceBinding.bind(view)
    private val characteristicAdapter = CharacteristicAdapter()

    fun bind(bluetoothService: BluetoothGattService) {
        binding.apply {
            tvService.text = bluetoothService.uuid.toString()
            rvCharacteristics.adapter = characteristicAdapter
            rvCharacteristics.layoutManager = LinearLayoutManager(view.context)
            characteristicAdapter.bluetoothGattService = bluetoothService
        }
    }
}