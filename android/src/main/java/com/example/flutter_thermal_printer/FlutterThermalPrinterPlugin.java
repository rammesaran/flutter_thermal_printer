package com.example.flutter_thermal_printer;

import androidx.annotation.NonNull;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

/** FlutterThermalPrinterPlugin */
public class FlutterThermalPrinterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, RequestPermissionsResultListener {
  private MethodChannel channel;
  private Context context;
  private Activity activity;
  private ExecutorService executorService;
  private Handler mainHandler;
  
  // Permission request codes
  private static final int PERMISSION_REQUEST_BLUETOOTH = 1001;
  private static final int PERMISSION_REQUEST_BLUETOOTH_ADMIN = 1002;
  private static final int PERMISSION_REQUEST_BLUETOOTH_CONNECT = 1003;
  private static final int PERMISSION_REQUEST_BLUETOOTH_SCAN = 1004;
  private static final int PERMISSION_REQUEST_STORAGE = 1005;
  private static final int PERMISSION_REQUEST_LOCATION = 1006;
  private static final int PERMISSION_REQUEST_ALL = 1007;
  
  // Store pending print operations
  private MethodCall pendingPrintCall;
  private Result pendingPrintResult;
  
  // Store pending permission requests
  private Result pendingPermissionResult;
  
  // Store pending device list request
  private Result pendingDeviceListResult;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_thermal_printer");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
    executorService = Executors.newSingleThreadExecutor();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;
      case "printTcp":
        handlePrintTcp(call, result);
        break;
      case "printBluetooth":
        handlePrintBluetooth(call, result);
        break;
      case "getBluetoothDevices":
        handleGetBluetoothDevices(call, result);
        break;
      case "printBluetoothDevice":
        handlePrintBluetoothDevice(call, result);
        break;
      case "checkPermissions":
        handleCheckPermissions(call, result);
        break;
      case "requestPermissions":
        handleRequestPermissions(call, result);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void handlePrintTcp(@NonNull MethodCall call, @NonNull Result result) {
    executorService.execute(() -> {
      try {
        android.util.Log.d("ThermalPrinter", "Starting TCP print operation");
        Map<String, Object> arguments = call.arguments();
        
        String ip = (String) arguments.get("ip");
        Integer port = (Integer) arguments.get("port");
        String payload = (String) arguments.get("payload");
        Boolean autoCut = (Boolean) arguments.get("autoCut");
        Boolean openCashbox = (Boolean) arguments.get("openCashbox");
        Integer mmFeedPaper = (Integer) arguments.get("mmFeedPaper");
        Integer printerDpi = (Integer) arguments.get("printerDpi");
        Integer printerWidthMM = (Integer) arguments.get("printerWidthMM");
        Integer printerNbrCharactersPerLine = (Integer) arguments.get("printerNbrCharactersPerLine");
        Integer timeout = (Integer) arguments.get("timeout");

        if (ip == null || port == null || payload == null) {
          android.util.Log.e("ThermalPrinter", "Invalid arguments: IP=" + ip + ", port=" + port + ", payload=" + (payload != null ? "provided" : "null"));
          mainHandler.post(() -> result.error("INVALID_ARGUMENTS", "IP, port, and payload are required", null));
          return;
        }

        int connectionTimeout = timeout != null ? timeout : 30000;
        android.util.Log.d("ThermalPrinter", "Connecting to " + ip + ":" + port + " with timeout " + connectionTimeout + "ms");
        
        // Create TCP connection
        TcpConnection tcpConnection = new TcpConnection(ip, port, connectionTimeout);
        
        EscPosPrinter printer = new EscPosPrinter(
          tcpConnection, 
          printerDpi != null ? printerDpi : 203,
          printerWidthMM != null ? printerWidthMM.floatValue() : 80f,
          printerNbrCharactersPerLine != null ? printerNbrCharactersPerLine : 42
        );

        String formattedPayload = payload;
        if (mmFeedPaper != null && mmFeedPaper > 0) {
          formattedPayload += "\n".repeat(mmFeedPaper / 4);
        }
        
        if (autoCut != null && autoCut) {
          formattedPayload += "\n";
        }

        if (openCashbox != null && openCashbox) {
          // Cashbox command removed - use ESC/POS commands in payload if needed
        }

        android.util.Log.d("ThermalPrinter", "Sending print data to printer");
        printer.printFormattedTextAndCut(formattedPayload);
        android.util.Log.d("ThermalPrinter", "Print operation completed successfully");
        
        mainHandler.post(() -> result.success(true));
      } catch (Exception e) {
        android.util.Log.e("ThermalPrinter", "TCP print error: " + e.getMessage(), e);
        final String errorMessage = "Failed to print via TCP: " + e.getMessage();
        final Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", errorMessage);
        errorDetails.put("type", "PRINT_ERROR");
        errorDetails.put("stackTrace", e.toString());
        mainHandler.post(() -> result.error("PRINT_ERROR", errorMessage, errorDetails));
      }
    });
  }

  private void handleGetBluetoothDevices(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is not attached", null);
      return;
    }

    // Store the result for later use after permission is granted
    pendingDeviceListResult = result;

    android.util.Log.d("ThermalPrinter", "Getting Bluetooth devices - Android SDK version: " + android.os.Build.VERSION.SDK_INT);

    // Check permissions based on Android version
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
      // For Android < 12, check legacy Bluetooth permissions
      int bluetoothPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH);
      int bluetoothAdminPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN);
      
      if (bluetoothPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_REQUEST_BLUETOOTH);
        return;
      }
      if (bluetoothAdminPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_REQUEST_BLUETOOTH_ADMIN);
        return;
      }
    } else {
      // For Android >= 12, check modern Bluetooth permissions
      int connectPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT);
      int scanPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN);
      
      if (connectPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH_CONNECT);
        return;
      }
      if (scanPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_REQUEST_BLUETOOTH_SCAN);
        return;
      }
    }

    // Permissions granted, get the device list
    executeGetBluetoothDevices(result);
  }

  private void executeGetBluetoothDevices(@NonNull Result result) {
    executorService.execute(() -> {
      try {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
          mainHandler.post(() -> result.error("BLUETOOTH_NOT_AVAILABLE", "Bluetooth is not available on this device", null));
          return;
        }

        if (!bluetoothAdapter.isEnabled()) {
          mainHandler.post(() -> result.error("BLUETOOTH_DISABLED", "Bluetooth is disabled", null));
          return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<Map<String, Object>> deviceList = new ArrayList<>();

        if (pairedDevices != null && pairedDevices.size() > 0) {
          for (BluetoothDevice device : pairedDevices) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("name", device.getName());
            deviceInfo.put("address", device.getAddress());
            deviceInfo.put("type", device.getType());
            deviceInfo.put("bondState", device.getBondState());
            deviceList.add(deviceInfo);
          }
        }

        android.util.Log.d("ThermalPrinter", "Found " + deviceList.size() + " paired Bluetooth devices");
        mainHandler.post(() -> result.success(deviceList));
      } catch (SecurityException e) {
        android.util.Log.e("ThermalPrinter", "Security exception while getting Bluetooth devices: " + e.getMessage(), e);
        mainHandler.post(() -> result.error("PERMISSION_DENIED", "Bluetooth permission denied", e.toString()));
      } catch (Exception e) {
        android.util.Log.e("ThermalPrinter", "Error getting Bluetooth devices: " + e.getMessage(), e);
        final String errorMessage = "Failed to get Bluetooth devices: " + e.getMessage();
        mainHandler.post(() -> result.error("BLUETOOTH_ERROR", errorMessage, e.toString()));
      }
    });
  }

  private void handlePrintBluetooth(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is not attached", null);
      return;
    }

    // Store the call and result for later use after permission is granted
    pendingPrintCall = call;
    pendingPrintResult = result;

    android.util.Log.d("ThermalPrinter", "Android SDK version: " + android.os.Build.VERSION.SDK_INT);

    // Check permissions based on Android version
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
      // For Android < 12, check legacy Bluetooth permissions
      int bluetoothPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH);
      int bluetoothAdminPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN);
      
      if (bluetoothPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_REQUEST_BLUETOOTH);
        return;
      }
      if (bluetoothAdminPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_REQUEST_BLUETOOTH_ADMIN);
        return;
      }
    } else {
      // For Android >= 12, check modern Bluetooth permissions
      int connectPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT);
      
      if (connectPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH_CONNECT);
        return;
      }
    }

    // Permissions granted, execute the print operation
    executePrintBluetooth(call, result);
  }

  private void handlePrintBluetoothDevice(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is not attached", null);
      return;
    }

    // Store the call and result for later use after permission is granted
    pendingPrintCall = call;
    pendingPrintResult = result;

    // Check permissions based on Android version
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
      int bluetoothPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH);
      int bluetoothAdminPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN);
      
      if (bluetoothPermission != PackageManager.PERMISSION_GRANTED || 
          bluetoothAdminPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, 
          new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 
          PERMISSION_REQUEST_BLUETOOTH);
        return;
      }
    } else {
      int connectPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT);
      
      if (connectPermission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, 
          new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 
          PERMISSION_REQUEST_BLUETOOTH_CONNECT);
        return;
      }
    }

    // Permissions granted, execute the print operation
    executePrintBluetoothDevice(call, result);
  }

  private void handleCheckPermissions(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is not attached", null);
      return;
    }

    java.util.HashMap<String, Object> permissionStatus = new java.util.HashMap<>();
    
    // Check storage permissions (for Android < 13)
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
      permissionStatus.put("READ_EXTERNAL_STORAGE", 
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
      permissionStatus.put("WRITE_EXTERNAL_STORAGE", 
        ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }
    
    // Check location permissions
    permissionStatus.put("ACCESS_FINE_LOCATION", 
      ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    permissionStatus.put("ACCESS_COARSE_LOCATION", 
      ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    
    // Check Bluetooth permissions based on Android version
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
      permissionStatus.put("BLUETOOTH", 
        ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED);
      permissionStatus.put("BLUETOOTH_ADMIN", 
        ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED);
    } else {
      permissionStatus.put("BLUETOOTH_CONNECT", 
        ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
      permissionStatus.put("BLUETOOTH_SCAN", 
        ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
    }

    result.success(permissionStatus);
  }

  private void handleRequestPermissions(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "Activity is not attached", null);
      return;
    }

    pendingPermissionResult = result;
    
    java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
    
    // Check storage permissions (for Android < 13)
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
      }
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
      }
    }
    
    // Check location permissions
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    
    // Check Bluetooth permissions based on Android version
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH);
      }
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
      }
    } else {
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
      }
      if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
      }
    }

    if (permissionsToRequest.isEmpty()) {
      java.util.HashMap<String, Object> permissionResults = new java.util.HashMap<>();
      permissionResults.put("status", "All permissions already granted");
      result.success(permissionResults);
    } else {
      android.util.Log.d("ThermalPrinter", "Requesting permissions: " + permissionsToRequest.toString());
      String[] permissionArray = permissionsToRequest.toArray(new String[0]);
      ActivityCompat.requestPermissions(activity, permissionArray, PERMISSION_REQUEST_ALL);
    }
  }

  private void executePrintBluetooth(@NonNull MethodCall call, @NonNull Result result) {
    executorService.execute(() -> {
      try {
        Map<String, Object> arguments = call.arguments();
        
        String payload = (String) arguments.get("payload");
        Boolean autoCut = (Boolean) arguments.get("autoCut");
        Boolean openCashbox = (Boolean) arguments.get("openCashbox");
        Integer mmFeedPaper = (Integer) arguments.get("mmFeedPaper");
        Integer printerDpi = (Integer) arguments.get("printerDpi");
        Integer printerWidthMM = (Integer) arguments.get("printerWidthMM");
        Integer printerNbrCharactersPerLine = (Integer) arguments.get("printerNbrCharactersPerLine");

        if (payload == null) {
          mainHandler.post(() -> result.error("INVALID_ARGUMENTS", "Payload is required", null));
          return;
        }

        android.util.Log.d("ThermalPrinter", "Selecting first paired Bluetooth printer");
        BluetoothConnection bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired();
        
        if (bluetoothConnection == null) {
          mainHandler.post(() -> result.error("BLUETOOTH_ERROR", "No paired Bluetooth printer found. Please pair a printer first.", null));
          return;
        }

        android.util.Log.d("ThermalPrinter", "Bluetooth printer selected, creating printer instance");
        EscPosPrinter printer = new EscPosPrinter(
          bluetoothConnection,
          printerDpi != null ? printerDpi : 203,
          printerWidthMM != null ? printerWidthMM.floatValue() : 80f,
          printerNbrCharactersPerLine != null ? printerNbrCharactersPerLine : 42
        );

        String formattedPayload = payload;
        if (mmFeedPaper != null && mmFeedPaper > 0) {
          formattedPayload += "\n".repeat(mmFeedPaper / 4);
        }
        
        if (autoCut != null && autoCut) {
          formattedPayload += "\n";
        }

        if (openCashbox != null && openCashbox) {
          // Cashbox command removed - use ESC/POS commands in payload if needed
        }

        android.util.Log.d("ThermalPrinter", "Printing formatted text");
        printer.printFormattedTextAndCut(formattedPayload);
        android.util.Log.d("ThermalPrinter", "Print completed successfully");
        
        mainHandler.post(() -> result.success(true));
      } catch (Exception e) {
        android.util.Log.e("ThermalPrinter", "Bluetooth print error: " + e.getMessage(), e);
        final String errorMessage = "Failed to print via Bluetooth: " + e.getMessage() + 
          (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : "");
        final Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", errorMessage);
        errorDetails.put("type", "BLUETOOTH_PRINT_ERROR");
        errorDetails.put("stackTrace", e.toString());
        mainHandler.post(() -> result.error("PRINT_ERROR", errorMessage, errorDetails));
      }
    });
  }

  private void executePrintBluetoothDevice(@NonNull MethodCall call, @NonNull Result result) {
    executorService.execute(() -> {
      try {
        Map<String, Object> arguments = call.arguments();
        
        String address = (String) arguments.get("address");
        String payload = (String) arguments.get("payload");
        Boolean autoCut = (Boolean) arguments.get("autoCut");
        Boolean openCashbox = (Boolean) arguments.get("openCashbox");
        Integer mmFeedPaper = (Integer) arguments.get("mmFeedPaper");
        Integer printerDpi = (Integer) arguments.get("printerDpi");
        Integer printerWidthMM = (Integer) arguments.get("printerWidthMM");
        Integer printerNbrCharactersPerLine = (Integer) arguments.get("printerNbrCharactersPerLine");

        if (address == null || payload == null) {
          mainHandler.post(() -> result.error("INVALID_ARGUMENTS", "Address and payload are required", null));
          return;
        }

        android.util.Log.d("ThermalPrinter", "Connecting to Bluetooth device: " + address);
        
        // Get Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
          mainHandler.post(() -> result.error("BLUETOOTH_NOT_AVAILABLE", "Bluetooth is not available on this device", null));
          return;
        }

        if (!bluetoothAdapter.isEnabled()) {
          mainHandler.post(() -> result.error("BLUETOOTH_DISABLED", "Bluetooth is disabled", null));
          return;
        }

        // Get the remote device
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
          mainHandler.post(() -> result.error("DEVICE_NOT_FOUND", "Bluetooth device not found: " + address, null));
          return;
        }

        android.util.Log.d("ThermalPrinter", "Creating Bluetooth connection to device");
        BluetoothConnection bluetoothConnection = new BluetoothConnection(device);
        
        android.util.Log.d("ThermalPrinter", "Creating printer instance");
        EscPosPrinter printer = new EscPosPrinter(
          bluetoothConnection,
          printerDpi != null ? printerDpi : 203,
          printerWidthMM != null ? printerWidthMM.floatValue() : 80f,
          printerNbrCharactersPerLine != null ? printerNbrCharactersPerLine : 42
        );

        String formattedPayload = payload;
        if (mmFeedPaper != null && mmFeedPaper > 0) {
          formattedPayload += "\n".repeat(mmFeedPaper / 4);
        }
        
        if (autoCut != null && autoCut) {
          formattedPayload += "\n";
        }

        if (openCashbox != null && openCashbox) {
          // Cashbox command removed - use ESC/POS commands in payload if needed
        }

        android.util.Log.d("ThermalPrinter", "Printing to device: " + device.getName());
        printer.printFormattedTextAndCut(formattedPayload);
        android.util.Log.d("ThermalPrinter", "Print completed successfully");
        
        mainHandler.post(() -> result.success(true));
      } catch (SecurityException e) {
        android.util.Log.e("ThermalPrinter", "Security exception: " + e.getMessage(), e);
        mainHandler.post(() -> result.error("PERMISSION_DENIED", "Bluetooth permission denied", e.toString()));
      } catch (Exception e) {
        android.util.Log.e("ThermalPrinter", "Bluetooth device print error: " + e.getMessage(), e);
        final String errorMessage = "Failed to print to Bluetooth device: " + e.getMessage() + 
          (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : "");
        final Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", errorMessage);
        errorDetails.put("type", "BLUETOOTH_DEVICE_PRINT_ERROR");
        errorDetails.put("stackTrace", e.toString());
        mainHandler.post(() -> result.error("PRINT_ERROR", errorMessage, errorDetails));
      }
    });
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addRequestPermissionsResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addRequestPermissionsResultListener(this);
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_REQUEST_ALL:
        if (pendingPermissionResult != null) {
          java.util.HashMap<String, Object> permissionResults = new java.util.HashMap<>();
          for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            permissionResults.put(permissions[i], granted);
          }
          pendingPermissionResult.success(permissionResults);
          pendingPermissionResult = null;
          return true;
        }
        break;
      case PERMISSION_REQUEST_BLUETOOTH:
      case PERMISSION_REQUEST_BLUETOOTH_ADMIN:
      case PERMISSION_REQUEST_BLUETOOTH_CONNECT:
      case PERMISSION_REQUEST_BLUETOOTH_SCAN:
        boolean permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        
        if (pendingDeviceListResult != null) {
          if (permissionGranted) {
            executeGetBluetoothDevices(pendingDeviceListResult);
          } else {
            pendingDeviceListResult.error("PERMISSION_DENIED", 
              "Bluetooth permission is required to get device list", null);
          }
          pendingDeviceListResult = null;
          return true;
        }
        
        if (pendingPrintCall != null && pendingPrintResult != null) {
          if (permissionGranted) {
            // Check which method was called
            String method = pendingPrintCall.method;
            if ("printBluetoothDevice".equals(method)) {
              executePrintBluetoothDevice(pendingPrintCall, pendingPrintResult);
            } else {
              executePrintBluetooth(pendingPrintCall, pendingPrintResult);
            }
          } else {
            pendingPrintResult.error("PERMISSION_DENIED", 
              "Bluetooth permission is required for printing", null);
          }
          pendingPrintCall = null;
          pendingPrintResult = null;
          return true;
        }
        break;
      default:
        return false;
    }
    return false;
  }
}