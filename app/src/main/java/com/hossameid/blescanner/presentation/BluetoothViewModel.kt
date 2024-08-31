package com.hossameid.blescanner.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = application.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanning = false

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    @SuppressLint("MissingPermission")
    fun scanLeDevice(macAddress: String, name: String) {
        viewModelScope.launch(Dispatchers.Main){
            if (!scanning) {
                // Start scanning with a timeout
                try {
                    scanning = true

                    val filter = ScanFilter.Builder().setDeviceName(name).setDeviceAddress(macAddress).build()
                    val settings = ScanSettings.Builder().setCallbackType(CALLBACK_TYPE_ALL_MATCHES).build()
                    bluetoothLeScanner.startScan(mutableListOf(filter), settings, leScanCallback)

                    // Stops scanning after a pre-defined scan period using withTimeout
                    withTimeout(SCAN_PERIOD) {
                        // The scanning will continue until the timeout occurs
                        delay(SCAN_PERIOD) // This is just to keep the coroutine active
                    }
                } catch (e: TimeoutCancellationException) {
                    // Timeout occurred, stop scanning
                } finally {
                    scanning = false
                    bluetoothLeScanner.stopScan(leScanCallback)
                }
            } else {
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("BLE SCAN", "onScanResult: $result")
        }
    }
}