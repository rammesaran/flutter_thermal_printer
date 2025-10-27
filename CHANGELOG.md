## 0.0.1

### Initial Release

* **TCP/Network Printing**: Full support for printing over WiFi/Ethernet connections
* **Bluetooth Printing**: Android support for paired Bluetooth thermal printers
* **ESC/POS Commands**: Complete ESC/POS formatting support including:
  - Text alignment (left, center, right)
  - Text formatting (bold, underline, different font sizes)
  - Image printing via URL
  - Barcode generation (EAN13, EAN8, UPC-A)
  - QR code generation
* **Configuration Options**: Extensive printer configuration including:
  - DPI settings
  - Paper width
  - Characters per line
  - Auto-cut and cash drawer control
  - Connection timeout
* **Cross-Platform**: Android and iOS support
* **Example App**: Complete demo application showing all features
* **Error Handling**: Comprehensive error handling and reporting

### Android Implementation
* Uses ESCPOS-ThermalPrinter-Android library v3.4.0 (latest)
* Full TCP and Bluetooth printing support
* Complete ESC/POS command support including Code 39 barcode support
* Thread-safe implementation with proper error handling
* Bluetooth Android 33+ compatibility

### iOS Implementation
* TCP printing support with Network framework
* Basic ESC/POS command support
* Bluetooth printing planned for future release

### Breaking Changes
None - this is the initial release.
