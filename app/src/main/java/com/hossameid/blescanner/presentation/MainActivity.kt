package com.hossameid.blescanner.presentation

import android.Manifest
import android.Manifest.permission.BLUETOOTH_SCAN
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hossameid.blescanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: BluetoothViewModel by viewModels()

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

        binding.connectBtn.setOnClickListener { onConnectBtnClick() }
    }

    private fun onConnectBtnClick()
    {
        val permissions = checkPermissions()

        //Check if the permissions were granted
        if(permissions.isNotEmpty())
        {
            Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_SHORT).show()
        }else{
            binding.macAddressLayout.error = null
            binding.usernameEditText.error = null

            val macAddress = binding.macAddressEditText.text.toString()
            val name = binding.usernameEditText.text.toString()

            if(macAddress.isEmpty() and name.isEmpty())
            {
                binding.macAddressLayout.error = "Please enter a MAC address or a name"
                binding.usernameEditText.error = "Please enter a name or a MAC address"
                return
            }

            //Start scanning
            viewModel.scanLeDevice(macAddress, name)
        }
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
        val permissionsToRequest = checkPermissions()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }else{
            binding.connectBtn.isEnabled = true
            binding.disconnectBtn.isEnabled = true
        }
    }

    private fun checkPermissions(): List<String>{
        return bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}