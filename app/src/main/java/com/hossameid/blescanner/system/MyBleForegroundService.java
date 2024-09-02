package com.hossameid.blescanner.system;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.hossameid.blescanner.R;

public class MyBleForegroundService extends Service {
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BLE_CONNECTION", "onStartCommand: Starting background service");
        device = intent.getParcelableExtra("bluetooth_device");
        if (device != null) {
            connectToDevice();
        }
        createNotificationChannel();

        // Display the notification for the foreground service
        startForeground(1, createNotification());

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "channel_id",
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("BLE Connection")
                .setContentText("Your app is connected to a BLE device.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder.build();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Successfully connected
                Log.d("BLE_CONNECTION", "onConnectionStateChange: connected");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Disconnected, attempt to reconnect
                Log.d("BLE_CONNECTION", "onConnectionStateChange: disconnected" + status);
                gatt.close();
                reconnectToDevice();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE_CONNECTION", "onServicesDiscovered: " + gatt.getServices());
            }
        }
    };


    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (device != null) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
        }
    }

    private void reconnectToDevice() {
        // Reconnect with a delay to avoid excessive connection attempts
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (device != null) {
                connectToDevice();
            }
        }, 5000); // Reconnect after 5 seconds
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        super.onDestroy();
    }
}
