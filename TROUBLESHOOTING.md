# Flutter Thermal Printer - Troubleshooting Guide

## Issue: Release APK showing loading with no print output or exceptions

### Problem Description
When building a release APK from the example project, the thermal printer functionality would show a loading state but wouldn't produce any print output or display meaningful error messages. The app would appear to hang during print operations.

### Root Cause
The issues were:
1. **Missing permissions** in the example app's `AndroidManifest.xml` - the plugin declared permissions but the consuming app needed to explicitly declare them
2. **Missing runtime permission requests** - for Android 12+ (API 31+), Bluetooth permissions like `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` are dangerous permissions that require runtime permission dialogs, not just manifest declarations

### Solution

#### 1. Added Required Permissions
Updated `flutter_thermal_printer/example/android/app/src/main/AndroidManifest.xml` to include:

```xml
<!-- Permissions for network printing -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Permissions for Bluetooth printing -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- For Android 12+ Bluetooth permissions -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

#### 2. Enhanced Error Handling
Improved the Android native code in `FlutterThermalPrinterPlugin.java` to:

- Log full stack traces with `e.printStackTrace()` for better debugging
- Include cause information in error messages
- Provide more detailed error information to the Flutter side
- Fixed Java compilation issues with lambda expressions

#### 3. Runtime Permission Handling
Implemented proper runtime permission requests for Bluetooth functionality:

- Added `RequestPermissionsResultListener` interface implementation
- Automatic permission checking based on Android version (< 12 vs >= 12)
- Permission dialog prompts for users when needed
- Proper handling of permission grant/deny responses
- Seamless retry of print operations after permission is granted

#### 3. Key Changes Made

**AndroidManifest.xml permissions:**
- `INTERNET` and `ACCESS_NETWORK_STATE`: Required for TCP/network printing
- `BLUETOOTH*` permissions: Required for Bluetooth printer connections
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: Required for Bluetooth operations on Android 12+

**Error handling improvements:**
- Better exception logging and reporting
- More descriptive error messages
- Proper lambda variable handling (final/effectively final)

**Runtime permission handling:**
- Automatic permission detection and request
- Android version-aware permission handling (legacy vs modern Bluetooth permissions)
- User-friendly permission dialogs
- Graceful handling of permission denial

### For App Developers Using This Plugin

When integrating this plugin into your own apps, make sure to include the following permissions in your app's `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Network printing permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Bluetooth printing permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    
    <!-- Required for Android 12+ -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <application>
        <!-- Your app configuration -->
    </application>
</manifest>
```

### Testing the Fix

1. Clean and rebuild your project:
   ```bash
   flutter clean
   flutter pub get
   flutter build apk --release
   ```

2. Install the release APK on a device
3. Test both TCP and Bluetooth printing functionality
4. Verify that error messages are now properly displayed if there are connection issues

### Additional Notes

- **Bluetooth Permission Dialogs**: When using Bluetooth printing for the first time, users will see permission request dialogs on Android 12+ devices. This is expected behavior and required for security.
- **Permission Requirements**: 
  - For Android < 12: `BLUETOOTH` and `BLUETOOTH_ADMIN` permissions
  - For Android >= 12: `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions
  - Location permissions may also be needed on some devices for Bluetooth discovery
- **Network Printing**: Ensure your network printer is accessible from the device's network
- **Bluetooth Setup**: Make sure the printer is paired with the device before attempting to print

The plugin now properly handles both manifest permissions and runtime permission requests, ensuring it works correctly in release builds with appropriate user permission dialogs.

### Testing the Runtime Permissions

1. Install the updated release APK on an Android 12+ device
2. Try to use Bluetooth printing - you should now see permission request dialogs
3. Grant the permissions when prompted
4. The printing should proceed normally after permissions are granted
5. TCP/Network printing should work without permission dialogs (only needs manifest permissions)
