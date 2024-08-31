package com.hossameid.blescanner.presentation

import android.Manifest
import android.Manifest.permission.BLUETOOTH_SCAN
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hossameid.blescanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkBLEAvailability()

        checkAndRequestPermissions()

    }

    private fun checkBLEAvailability() {
        // Check to see if the BLE feature is available.
        val bluetoothLEAvailable =
            packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        if (!bluetoothLEAvailable) {
            Toast.makeText(this, "BLE not available", Toast.LENGTH_LONG).show()
        }
    }

    private val bluetoothPermissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
            BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        else -> arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val isGranted = it.value
                // Handle the granted/denied status
                if (isGranted) {
                    // Permission granted
                    binding.connectBtn.isEnabled = true
                    binding.disconnectBtn.isEnabled = true
                } else {
                    // Permission denied
                    binding.connectBtn.isEnabled = false
                    binding.disconnectBtn.isEnabled = false
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }else{
            binding.connectBtn.isEnabled = true
            binding.disconnectBtn.isEnabled = true
        }
    }
}