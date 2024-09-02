package com.hossameid.blescanner.system;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.hossameid.blescanner.R;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MyBleForegroundService extends Service {
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;
    private String characteristicUUID;
    private BluetoothGattCharacteristic characteristic;
    private Handler handler;
    private Runnable readRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper()); // Runs on the main thread
        initReadRunnable();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BLE_CONNECTION", "onStartCommand: Starting background service");
        device = intent.getParcelableExtra("bluetooth_device");
        characteristicUUID = intent.getStringExtra("characteristic");

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
                sendConnectionStatusBroadcast("Connected");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Disconnected, attempt to reconnect
                Log.d("BLE_CONNECTION", "onConnectionStateChange: disconnected" + status);
                gatt.close();
                sendConnectionStatusBroadcast("Disconnected");
                reconnectToDevice();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();
                boolean charaFound = false;

                for (BluetoothGattService service : services) {
                    Log.d("BLE_CONNECTION", "onServicesDiscovered: " + service.getUuid().toString());

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    for (BluetoothGattCharacteristic characteristic : characteristics) {

                        //If the user inputs a 16 bit UUID then convert 128 bit UUID to 16 bit
                        String currentCharacteristicUUID = getCharacteristicUUID(characteristic);

                        if (characteristicUUID.equalsIgnoreCase(currentCharacteristicUUID)) {
                            listenToCharacteristic(characteristic);

                            //Stop the search
                            charaFound = true;
                        }
                        Log.d("BLE_CONNECTION", "onServicesDiscovered: " +
                                characteristic.getUuid().toString());
                    }

                    //Stop the search
                    if (charaFound)
                        break;
                }
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.d("BLE_CONNECTION", "onCharacteristicRead: " + Arrays.toString(value));
            extractData(characteristic);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt
                                                    gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            Log.d("BLE_CONNECTION", "onCharacteristicChanged: " + Arrays.toString(value));
            extractData(characteristic);
        }
    };

    private void extractData(BluetoothGattCharacteristic characteristic) {
        String UUID_HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid().toString())) {
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d("BLE_CONNECTION", "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d("BLE_CONNECTION", "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d("BLE_CONNECTION", String.format("Received heart rate: %d", heartRate));
            sendCharacteristicValueBroadcast(String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                sendCharacteristicValueBroadcast(new String(data) + "  " + stringBuilder);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void listenToCharacteristic(BluetoothGattCharacteristic characteristic) {
        //Check the type of characteristic property
        int property = characteristic.getProperties();

        if (property == BluetoothGattCharacteristic.PROPERTY_READ) {
            //Use this in case of read
            this.characteristic = characteristic;

            startReadingCharacteristic();
        } else if (property == BluetoothGattCharacteristic.PROPERTY_NOTIFY ||
                property == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
            // Use this in case of notify or indicate
            setCharacteristicNotification(characteristic, true);
        }

        sendCharacteristicUuidBroadcast(characteristic.getUuid().toString());
    }

    // Initialize the Runnable that will read the characteristic
    private void initReadRunnable() {
        readRunnable = new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (bluetoothGatt != null && characteristic != null) {
                    bluetoothGatt.readCharacteristic(characteristic);
                }
                // Schedule the next read after 1 second
                handler.postDelayed(this, 1000);
            }
        };
    }

    // Start reading the characteristic every second
    public void startReadingCharacteristic() {
        handler.post(readRunnable); // Start the first execution
    }

    private String getCharacteristicUUID(BluetoothGattCharacteristic characteristic) {
        return (characteristicUUID.length() == 4) ?
                convertUUID(characteristic.getUuid().toString()) :
                characteristic.getUuid().toString();
    }

    private String convertUUID(String uuid) {
        String[] splitedString = uuid.split("-");

        return splitedString[0].replaceFirst("^0+", "");
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w("BLE_CONNECTION", "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

        BluetoothGattDescriptor descriptor = characteristic.
                getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));

        int property = characteristic.getProperties();
        if (property == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        else if (property == BluetoothGattCharacteristic.PROPERTY_INDICATE)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

        bluetoothGatt.writeDescriptor(descriptor);
    }


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
                sendConnectionStatusBroadcast("Reconnecting");
                connectToDevice();
            }
        }, 5000); // Reconnect after 5 seconds
    }

    private void sendConnectionStatusBroadcast(String status) {
        Intent intent = new Intent("ACTION_CONNECTION_STATUS");
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCharacteristicValueBroadcast(String value) {
        Intent intent = new Intent("ACTION_CHARACTERISTIC_VALUE");
        intent.putExtra("value", value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCharacteristicUuidBroadcast(String uuid) {
        Intent intent = new Intent("ACTION_CHARACTERISTIC_UUID");
        intent.putExtra("uuid", uuid);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        super.onDestroy();
        stopForeground(true);
        stopSelf();
    }
}
