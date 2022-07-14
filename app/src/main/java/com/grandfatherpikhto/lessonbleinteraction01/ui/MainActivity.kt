package com.grandfatherpikhto.lessonbleinteraction01.ui

import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.blin.BleManager
import com.grandfatherpikhto.blin.BleScanManager
import com.grandfatherpikhto.blin.permissions.RequestPermissions
import com.grandfatherpikhto.lessonbleinteraction01.BleApplication
import com.grandfatherpikhto.lessonbleinteraction01.R
import com.grandfatherpikhto.lessonbleinteraction01.databinding.ActivityMainBinding
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.MainActivityViewModel
import com.grandfatherpikhto.lessonbleinteraction01.ui.fragments.models.ScanViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private val logTag = this.javaClass.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val mainActivityViewModel by viewModels<MainActivityViewModel>()
    private val scanViewModel by viewModels<ScanViewModel>()

    private var _bleManager:BleManager? = null
    private val bleManager:BleManager get() = _bleManager!!

    private val requestPermissions: RequestPermissions by lazy {
        RequestPermissions(this).let {
            lifecycle.addObserver(it)
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BleManager(this).let {
            lifecycle.addObserver(it)
            (application as BleApplication).bleManager = it
            _bleManager = it
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        addMainMenuProvider()

        lifecycleScope.launch {
            requestPermissions.stateFlowRequestPermission.filterNotNull().collect { permission ->
                Log.e(logTag, "Permission ${permission.permission}, ${permission.granted}")
                if (permission.granted) {
                    Toast.makeText(baseContext, getString(R.string.message_permission_granted, permission.permission), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext, getString(R.string.message_permission_not_granted, permission.permission), Toast.LENGTH_SHORT).show()
                    finishAndRemoveTask()
                    exitProcess(0)
                }
            }
        }

        lifecycleScope.launch {
            mainActivityViewModel.flowCurrentDevice.collect { bluetoothDevice ->
                if (bluetoothDevice == null) {
                    bleManager.close()
                } else {
                    bleManager.connect(bluetoothDevice.address)
                }
            }
        }

        requestPermissions.requestPermissions(listOf(
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
        ))
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    /**
     * Теперь, можно выделить меню провайдер в отдельный класс, объект, что угодно. Это удобно
     * https://developer.android.com/reference/androidx/activity/ComponentActivity#addMenuProvider(androidx.core.view.MenuProvider,androidx.lifecycle.LifecycleOwner)
     */
    private fun addMainMenuProvider() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_settings -> true
                    else -> true
                }
            }
        })
    }
}