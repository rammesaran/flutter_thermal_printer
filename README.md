# flutter_thermal_printer_pos

A Flutter plugin for thermal printer support with TCP/Network and Bluetooth printing capabilities using ESC/POS commands. This plugin provides a bridge to native thermal printer libraries.

## Features

- ✅ **TCP/Network Printing** - Print over WiFi or Ethernet
- ✅ **Bluetooth Printing** - Print via paired Bluetooth devices (Android only)
- ✅ **ESC/POS Commands** - Full support for ESC/POS formatting
- ✅ **Image Printing** - Support for images via URL
- ✅ **Barcode & QR Code** - Generate barcodes and QR codes
- ✅ **Text Formatting** - Bold, underline, different font sizes
- ✅ **Layout Control** - Left, center, right alignment
- ✅ **Auto-cut & Cash Drawer** - Hardware control features
- ✅ **Android & iOS Support** - Cross-platform implementation

## Supported Printers

This plugin has been tested with the following thermal printer models:

- Epson TM-T82, TM-T82X, TM-T88VI, TM-T20III
- Zywell, VSC, EPPOS
- Most ESC/POS compatible thermal printers

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  flutter_thermal_printer_pos: ^0.0.1
```

Then run:

```bash
flutter pub get
```

## Permissions

### Android

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- For network printing -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- For Bluetooth printing -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

### iOS

Bluetooth permissions are handled automatically by the plugin.

## Usage

### Import the package

```dart
import 'package:flutter_thermal_printer_pos/flutter_thermal_printer_pos.dart';
```

### Initialize the plugin

```dart
// Simple usage - direct method calls
```

### TCP/Network Printing

```dart
// Print via TCP/IP
try {
  await FlutterThermalPrinterPos.printTcp(
    ip: '192.168.1.100',
    port: 9100,
    payload: '[C]Hello World!\n[L]This is a test',
    autoCut: true,
    openCashbox: false,
  );
  print('Print successful!');
} catch (e) {
  print('Print failed: $e');
}
```

### Bluetooth Printing

```dart
try {
  await FlutterThermalPrinterPos.printBluetooth(
    payload: '[C]Hello World!\n[L]Bluetooth test',
    printerNbrCharactersPerLine: 38,
  );
  print('Print successful!');
} catch (e) {
  print('Print failed: $e');
}
```

### ESC/POS Formatting

The plugin supports ESC/POS formatting syntax:

```dart
final String receiptContent = '''
[C]<img>https://via.placeholder.com/300.jpg</img>
[L]
[C]<u><font size='big'>ORDER N°045</font></u>
[L]
[C]================================
[L]
[L]<b>BEAUTIFUL SHIRT</b>[R]9.99€
[L]  + Size : S
[L]
[L]<b>AWESOME HAT</b>[R]24.99€
[L]  + Size : 57/58
[L]
[C]--------------------------------
[R]TOTAL PRICE :[R]34.98€
[R]TAX :[R]4.23€
[L]
[C]================================
[L]
[L]<font size='tall'>Customer :</font>
[L]Raymond DUPONT
[L]5 rue des girafes
[L]31547 PERPETES
[L]Tel : +33801201456
[L]
[C]<barcode type='ean13' height='10'>831254784551</barcode>
[C]<qrcode size='20'>https://example.com</qrcode>
''';

await FlutterThermalPrinterPos.printTcp(
  ip: '192.168.1.100',
  port: 9100,
  payload: receiptContent,
);
```

## Configuration Options

### TCP Printing Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| ip | String | Required | Printer IP address |
| port | int | Required | Printer port (usually 9100) |
| payload | String | Required | Content to print |
| autoCut | bool | true | Automatically cut paper |
| openCashbox | bool | false | Open cash drawer |
| mmFeedPaper | int | 20 | Paper feed amount (mm) |
| printerDpi | int | 203 | Printer DPI |
| printerWidthMM | int | 80 | Paper width (mm) |
| printerNbrCharactersPerLine | int | 42 | Characters per line |
| timeout | int | 30000 | Connection timeout (ms) |

### Bluetooth Printing Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| payload | String | Required | Content to print |
| autoCut | bool | true | Automatically cut paper |
| openCashbox | bool | false | Open cash drawer |
| mmFeedPaper | int | 20 | Paper feed amount (mm) |
| printerDpi | int | 203 | Printer DPI |
| printerWidthMM | int | 80 | Paper width (mm) |
| printerNbrCharactersPerLine | int | 42 | Characters per line |

## ESC/POS Formatting Guide

### Text Alignment
- `[L]` - Left align
- `[C]` - Center align  
- `[R]` - Right align

### Text Formatting
- `<b>text</b>` - Bold text
- `<u>text</u>` - Underlined text
- `<font size='big'>text</font>` - Large text
- `<font size='tall'>text</font>` - Tall text

### Images
- `<img>https://example.com/image.jpg</img>` - Print image from URL

### Barcodes
- `<barcode type='ean13' height='10'>123456789012</barcode>` - EAN13 barcode
- `<barcode type='ean8' height='10'>1234567</barcode>` - EAN8 barcode
- `<barcode type='upca' height='10'>123456789012</barcode>` - UPC-A barcode

### QR Codes
- `<qrcode size='20'>Your text here</qrcode>` - QR code

## Error Handling

The plugin throws specific exceptions that you can catch:

```dart
try {
  await FlutterThermalPrinterPos.printTcp(
    ip: '192.168.1.100',
    port: 9100,
    payload: '[C]Test Print',
  );
} catch (e) {
  if (e.toString().contains('INVALID_ARGUMENTS')) {
    print('Invalid printer configuration');
  } else if (e.toString().contains('PRINT_ERROR')) {
    print('Printer communication error');
  } else if (e.toString().contains('BLUETOOTH_ERROR')) {
    print('Bluetooth connection issue');
  } else {
    print('Unknown error: $e');
  }
}
```

## Platform Support

| Feature | Android | iOS |
|---------|---------|-----|
| TCP/Network Printing | ✅ | ✅ |
| Bluetooth Printing | ✅ | ⏳ |
| ESC/POS Commands | ✅ | ✅* |
| Image Printing | ✅ | ⏳ |
| Barcode Generation | ✅ | ⏳ |
| QR Code Generation | ✅ | ⏳ |

*iOS has basic ESC/POS support, full formatting support coming soon

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Android implementation uses [ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android) library
- Inspired by [react-native-thermal-printer](https://github.com/AllInOneYT/react-native-thermal-printer)
