package com.grandfatherpikhto.lessonbleinteraction01

import android.app.Application
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.blin.BleManager
import com.grandfatherpikhto.blin.BleScanManager

class BleApplication : Application() {
    var bleManager: BleManager? = null
}