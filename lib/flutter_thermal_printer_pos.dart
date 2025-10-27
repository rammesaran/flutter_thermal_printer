import 'package:flutter/services.dart';
import 'dart:async';

class FlutterThermalPrinterPos {
  static const MethodChannel _channel = MethodChannel(
    'flutter_thermal_printer_pos',
  );

  // Default configuration similar to React Native package
  static ThermalPrinterConfig defaultConfig = ThermalPrinterConfig(
    ip: '192.168.192.168',
    port: 9100,
    autoCut: true,
    openCashbox: false,
    mmFeedPaper: 20,
    printerDpi: 203,
    printerWidthMM: 80,
    printerNbrCharactersPerLine: 42,
    timeout: 30000,
  );

  /// Get platform version
  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Print via TCP/IP with optional config override
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   await FlutterThermalPrinterPos.printTcp(
  ///     payload: '[C]<b>Hello World</b>\n[L]Test print',
  ///     ip: '192.168.100.246',
  ///     port: 9100,
  ///   );
  ///   print('Print successful');
  /// } catch (e) {
  ///   print('Print error: $e');
  /// }
  /// ```
  static Future<bool> printTcp({
    required String payload,
    String? ip,
    int? port,
    bool? autoCut,
    bool? openCashbox,
    int? mmFeedPaper,
    int? printerDpi,
    int? printerWidthMM,
    int? printerNbrCharactersPerLine,
    int? timeout,
  }) async {
    try {
      final config = {
        'payload': payload,
        'ip': ip ?? defaultConfig.ip,
        'port': port ?? defaultConfig.port,
        'autoCut': autoCut ?? defaultConfig.autoCut,
        'openCashbox': openCashbox ?? defaultConfig.openCashbox,
        'mmFeedPaper': mmFeedPaper ?? defaultConfig.mmFeedPaper,
        'printerDpi': printerDpi ?? defaultConfig.printerDpi,
        'printerWidthMM': printerWidthMM ?? defaultConfig.printerWidthMM,
        'printerNbrCharactersPerLine':
            printerNbrCharactersPerLine ??
            defaultConfig.printerNbrCharactersPerLine,
        'timeout': timeout ?? defaultConfig.timeout,
      };

      final result = await _channel.invokeMethod('printTcp', config);
      return result == true;
    } on PlatformException catch (e) {
      throw ThermalPrinterException(
        code: e.code,
        message: e.message ?? 'Unknown error',
        details: e.details,
      );
    }
  }

  /// Print via Bluetooth to first paired printer with optional config override
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   await FlutterThermalPrinterPos.printBluetooth(
  ///     payload: '[C]<b>Hello World</b>\n[L]Bluetooth test',
  ///     printerNbrCharactersPerLine: 38,
  ///   );
  ///   print('Print successful');
  /// } catch (e) {
  ///   print('Print error: $e');
  /// }
  /// ```
  static Future<bool> printBluetooth({
    required String payload,
    bool? autoCut,
    bool? openCashbox,
    int? mmFeedPaper,
    int? printerDpi,
    int? printerWidthMM,
    int? printerNbrCharactersPerLine,
  }) async {
    try {
      final config = {
        'payload': payload,
        'autoCut': autoCut ?? defaultConfig.autoCut,
        'openCashbox': openCashbox ?? defaultConfig.openCashbox,
        'mmFeedPaper': mmFeedPaper ?? defaultConfig.mmFeedPaper,
        'printerDpi': printerDpi ?? defaultConfig.printerDpi,
        'printerWidthMM': printerWidthMM ?? defaultConfig.printerWidthMM,
        'printerNbrCharactersPerLine':
            printerNbrCharactersPerLine ??
            defaultConfig.printerNbrCharactersPerLine,
      };

      final result = await _channel.invokeMethod('printBluetooth', config);
      return result == true;
    } on PlatformException catch (e) {
      throw ThermalPrinterException(
        code: e.code,
        message: e.message ?? 'Unknown error',
        details: e.details,
      );
    }
  }

  /// Get list of paired Bluetooth devices
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   List<BluetoothDevice> devices = await FlutterThermalPrinterPos.getBluetoothDevices();
  ///   for (var device in devices) {
  ///     print('Device: ${device.name} - ${device.address}');
  ///   }
  /// } catch (e) {
  ///   print('Error getting devices: $e');
  /// }
  /// ```
  static Future<List<BluetoothDevice>> getBluetoothDevices() async {
    try {
      final List<dynamic>? result = await _channel.invokeMethod(
        'getBluetoothDevices',
      );

      if (result == null) return [];

      return result.map((device) => BluetoothDevice.fromMap(device)).toList();
    } on PlatformException catch (e) {
      throw ThermalPrinterException(
        code: e.code,
        message: e.message ?? 'Unknown error',
        details: e.details,
      );
    }
  }

  /// Print via Bluetooth to specific device by address
  ///
  /// Example:
  /// ```dart
  /// try {
  ///   List<BluetoothDevice> devices = await FlutterThermalPrinterPos.getBluetoothDevices();
  ///   if (devices.isNotEmpty) {
  ///     await FlutterThermalPrinterPos.printBluetoothDevice(
  ///       address: devices.first.address,
  ///       payload: '[C]<b>Hello from specific device</b>',
  ///     );
  ///   }
  /// } catch (e) {
  ///   print('Print error: $e');
  /// }
  /// ```
  static Future<bool> printBluetoothDevice({
    required String address,
    required String payload,
    bool? autoCut,
    bool? openCashbox,
    int? mmFeedPaper,
    int? printerDpi,
    int? printerWidthMM,
    int? printerNbrCharactersPerLine,
  }) async {
    try {
      final config = {
        'address': address,
        'payload': payload,
        'autoCut': autoCut ?? defaultConfig.autoCut,
        'openCashbox': openCashbox ?? defaultConfig.openCashbox,
        'mmFeedPaper': mmFeedPaper ?? defaultConfig.mmFeedPaper,
        'printerDpi': printerDpi ?? defaultConfig.printerDpi,
        'printerWidthMM': printerWidthMM ?? defaultConfig.printerWidthMM,
        'printerNbrCharactersPerLine':
            printerNbrCharactersPerLine ??
            defaultConfig.printerNbrCharactersPerLine,
      };

      final result = await _channel.invokeMethod(
        'printBluetoothDevice',
        config,
      );
      return result == true;
    } on PlatformException catch (e) {
      throw ThermalPrinterException(
        code: e.code,
        message: e.message ?? 'Unknown error',
        details: e.details,
      );
    }
  }

  /// Check current permissions status
  static Future<Map<String, bool>> checkPermissions() async {
    try {
      final Map<dynamic, dynamic>? result = await _channel.invokeMethod(
        'checkPermissions',
      );

      if (result == null) return {};

      return result.map(
        (key, value) => MapEntry(key.toString(), value as bool),
      );
    } on PlatformException catch (e) {
      throw ThermalPrinterException(
        code: e.code,
        message: e.message ?? 'Unknown error',
        details: e.details,
      );
    }
  }

  /// Request required permissions
  static Future<Map<String, bool>> requestPermissions() async {
    try {
      final dynamic result = await _channel.invokeMethod('requestPermissions');

      if (result is Map) {
        return result.map(
          (key, value) => MapEntry(key.toString(), value as bool),
        );
      }

      return {};
    } on PlatformException catch (e) {
      throw ThermalPrinterException(
        code: e.code,
        message: e.message ?? 'Unknown error',
        details: e.details,
      );
    }
  }
}

/// Configuration class for thermal printer settings
class ThermalPrinterConfig {
  final String ip;
  final int port;
  final bool autoCut;
  final bool openCashbox;
  final int mmFeedPaper;
  final int printerDpi;
  final int printerWidthMM;
  final int printerNbrCharactersPerLine;
  final int timeout;

  ThermalPrinterConfig({
    this.ip = '192.168.192.168',
    this.port = 9100,
    this.autoCut = true,
    this.openCashbox = false,
    this.mmFeedPaper = 20,
    this.printerDpi = 203,
    this.printerWidthMM = 80,
    this.printerNbrCharactersPerLine = 42,
    this.timeout = 30000,
  });

  ThermalPrinterConfig copyWith({
    String? ip,
    int? port,
    bool? autoCut,
    bool? openCashbox,
    int? mmFeedPaper,
    int? printerDpi,
    int? printerWidthMM,
    int? printerNbrCharactersPerLine,
    int? timeout,
  }) {
    return ThermalPrinterConfig(
      ip: ip ?? this.ip,
      port: port ?? this.port,
      autoCut: autoCut ?? this.autoCut,
      openCashbox: openCashbox ?? this.openCashbox,
      mmFeedPaper: mmFeedPaper ?? this.mmFeedPaper,
      printerDpi: printerDpi ?? this.printerDpi,
      printerWidthMM: printerWidthMM ?? this.printerWidthMM,
      printerNbrCharactersPerLine:
          printerNbrCharactersPerLine ?? this.printerNbrCharactersPerLine,
      timeout: timeout ?? this.timeout,
    );
  }
}

/// Bluetooth device information
class BluetoothDevice {
  final String name;
  final String address;
  final int type;
  final int bondState;

  BluetoothDevice({
    required this.name,
    required this.address,
    required this.type,
    required this.bondState,
  });

  factory BluetoothDevice.fromMap(Map<dynamic, dynamic> map) {
    return BluetoothDevice(
      name: map['name'] ?? 'Unknown',
      address: map['address'] ?? '',
      type: map['type'] ?? 0,
      bondState: map['bondState'] ?? 0,
    );
  }

  @override
  String toString() {
    return 'BluetoothDevice(name: $name, address: $address, type: $type, bondState: $bondState)';
  }
}

/// Custom exception for thermal printer errors
class ThermalPrinterException implements Exception {
  final String code;
  final String message;
  final dynamic details;

  ThermalPrinterException({
    required this.code,
    required this.message,
    this.details,
  });

  @override
  String toString() {
    return 'ThermalPrinterException($code): $message${details != null ? '\nDetails: $details' : ''}';
  }
}
