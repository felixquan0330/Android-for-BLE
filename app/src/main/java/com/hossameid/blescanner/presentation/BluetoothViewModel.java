package com.hossameid.blescanner.presentation;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import java.util.List;

public class BluetoothViewModel extends AndroidViewModel {
    private final android.bluetooth.le.BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning = false;
    private final Handler handler = new Handler();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public BluetoothViewModel(Application application) {
        super(application);
        BluetoothManager bluetoothManager = application.getSystemService(BluetoothManager.class);
        android.bluetooth.BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    @SuppressLint("MissingPermission")
    public void scanLeDevice(String macAddress, String name) {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(() -> {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                Log.d("BLE_SCAN_CALLBACK", "scanLeDevice: stopped scanning");
            }, SCAN_PERIOD);

            scanning = true;
            ScanFilter.Builder builder = new ScanFilter.Builder();

            if (!macAddress.isEmpty())
                builder.setDeviceAddress(macAddress);

            if(!name.isEmpty())
                builder.setDeviceName(name);

            ScanFilter filter = builder.build();

            ScanSettings settings = new ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build();

            bluetoothLeScanner.startScan(List.of(filter), settings, leScanCallback);
            Log.d("BLE_SCAN_CALLBACK", "scanLeDevice: started scanning");
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // Device scan callback.
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d("BLE_SCAN_CALLBACK", "onScanResult: " + result);
        }
    };
}
