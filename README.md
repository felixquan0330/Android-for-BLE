# BLE Connection Android Application

## Objective
This Android application is designed to connect to a Bluetooth Low Energy (BLE) server using either a MAC address or a device name. Once connected, the app continuously reads or listens to a specific BLE characteristic:
- If the characteristic is of type **Read**, the app reads the value every second.
- If the characteristic is of type **Notify**, the app listens for updates and processes the data whenever the characteristic changes.

The app ensures a stable connection by operating in both the foreground and background, even if the app is removed from the recent apps list. It will automatically attempt to reconnect to the BLE server every 5 seconds if the connection is lost.

## How to Use
1. **Connect to a BLE Server:**
   - Launch the application.
   - Enter the BLE server's MAC address or device name.
   - Enter the UUID of the required characteristic.
   - Tap the "Connect" button to establish a connection.

2. **Reading Characteristics:**
   - Upon successful connection, the app will begin to read or listen to the specified characteristic based on its type.

3. **Background Operation:**
   - The app will continue to operate in the background, maintaining the BLE connection even if the app is closed or removed from recent apps.

4. **Reconnection:**
   - If the BLE server disconnects for any reason, the app will automatically attempt to reconnect every 5 seconds.

## Technologies Used
- **Language:** Java
- **IDE:** Android Studio (Koala)
- **Target SDK:** 34
- **Minimum SDK:** 25
- **BLE APIs:** Android Native BLE APIs

## How to Run the App

### Running in Android Studio
1. Clone this repository to your local machine.
2. Open Android Studio and select "Open an existing Android Studio project."
3. Navigate to the cloned repository and open it.
4. Connect your Android device or start an emulator.
5. Click the "Run" button in Android Studio to build and deploy the app to your device.

### Running via APK
1. Go to the [Releases](https://github.com/tomas0330/Android-for-BLE/releases) section of this repository.
2. Download the latest APK file.
3. Transfer the APK to your Android device.
4. On your device, navigate to the file manager and locate the APK.
5. Tap the APK file to install it.
6. Once installed, open the app and follow the usage instructions to connect to a BLE server.

---
