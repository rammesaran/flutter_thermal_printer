import 'package:flutter/material.dart';
import 'package:flutter_thermal_printer_pos/flutter_thermal_printer_pos.dart';

void main() {
  runApp(const TestApp());
}

class TestApp extends StatelessWidget {
  const TestApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Thermal Printer Test',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const TestPage(),
    );
  }
}

class TestPage extends StatefulWidget {
  const TestPage({Key? key}) : super(key: key);

  @override
  State<TestPage> createState() => _TestPageState();
}

class _TestPageState extends State<TestPage> {
  final TextEditingController _ipController = TextEditingController(
    text: '192.168.0.7',
  );
  final TextEditingController _portController = TextEditingController(
    text: '9100',
  );
  bool _isLoading = false;
  String _statusMessage = '';

  /// Test 1: Simple text only (safest test)
  Future<void> _testSimplePrint() async {
    setState(() {
      _isLoading = true;
      _statusMessage = 'Testing simple print...';
    });

    try {
      await FlutterThermalPrinterPos.printTcp(
        ip: _ipController.text,
        port: int.parse(_portController.text),
        payload: '[C]Hello World\n[L]This is a test',
        autoCut: true,
        openCashbox: false, // Important: Set to false
      );

      setState(() {
        _statusMessage = '✅ Simple print successful!';
        _isLoading = false;
      });

      _showSuccess('Simple print test passed!');
    } catch (e) {
      setState(() {
        _statusMessage = '❌ Error: $e';
        _isLoading = false;
      });
      _showError('Print failed: $e');
    }
  }

  /// Test 2: Formatted receipt (more complex)
  Future<void> _testFormattedPrint() async {
    setState(() {
      _isLoading = true;
      _statusMessage = 'Testing formatted print...';
    });

    try {
      const payload = '''[C]<b>TEST RECEIPT</b>
[L]
[C]================================
[L]
[L]Item 1..................\$10.00
[L]Item 2..................\$15.00
[L]Item 3..................\$20.00
[L]
[C]--------------------------------
[R]SUBTOTAL:[R]\$45.00
[R]TAX:[R]\$4.50
[R]TOTAL:[R]\$49.50
[L]
[C]================================
[L]
[C]Thank you for your purchase!
[L]
[L]''';

      await FlutterThermalPrinterPos.printTcp(
        ip: _ipController.text,
        port: int.parse(_portController.text),
        payload: payload,
        autoCut: true,
        openCashbox: false, // Important: Set to false
        mmFeedPaper: 20,
      );

      setState(() {
        _statusMessage = '✅ Formatted print successful!';
        _isLoading = false;
      });

      _showSuccess('Formatted print test passed!');
    } catch (e) {
      setState(() {
        _statusMessage = '❌ Error: $e';
        _isLoading = false;
      });
      _showError('Print failed: $e');
    }
  }

  /// Test 3: Test with barcode and QR code
  Future<void> _testAdvancedPrint() async {
    setState(() {
      _isLoading = true;
      _statusMessage = 'Testing advanced print...';
    });

    try {
      const payload = '''[C]<b>ADVANCED TEST</b>
[L]
[C]Barcode Test:
[C]<barcode type='ean13' height='10'>123456789012</barcode>
[L]
[C]QR Code Test:
[C]<qrcode size='20'>https://flutter.dev</qrcode>
[L]
[C]Test Complete!
[L]''';

      await FlutterThermalPrinterPos.printTcp(
        ip: _ipController.text,
        port: int.parse(_portController.text),
        payload: payload,
        autoCut: true,
        openCashbox: false, // Important: Set to false
      );

      setState(() {
        _statusMessage = '✅ Advanced print successful!';
        _isLoading = false;
      });

      _showSuccess('Advanced print test passed!');
    } catch (e) {
      setState(() {
        _statusMessage = '❌ Error: $e';
        _isLoading = false;
      });
      _showError('Print failed: $e');
    }
  }

  void _showSuccess(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 5),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Thermal Printer Test')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Status Card
            if (_statusMessage.isNotEmpty)
              Card(
                color: _statusMessage.startsWith('✅')
                    ? Colors.green.shade50
                    : _statusMessage.startsWith('❌')
                    ? Colors.red.shade50
                    : Colors.blue.shade50,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(
                    _statusMessage,
                    style: const TextStyle(fontSize: 14),
                  ),
                ),
              ),
            const SizedBox(height: 16),

            // IP and Port
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Printer Configuration',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _ipController,
                      decoration: const InputDecoration(
                        labelText: 'IP Address',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.wifi),
                      ),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _portController,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(
                        labelText: 'Port',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.settings_ethernet),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Test Buttons
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Run Tests',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),

                    ElevatedButton.icon(
                      onPressed: _isLoading ? null : _testSimplePrint,
                      icon: const Icon(Icons.text_fields),
                      label: const Text('Test 1: Simple Text'),
                      style: ElevatedButton.styleFrom(
                        minimumSize: const Size.fromHeight(50),
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Prints basic text only - safest test',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),

                    const SizedBox(height: 16),

                    ElevatedButton.icon(
                      onPressed: _isLoading ? null : _testFormattedPrint,
                      icon: const Icon(Icons.receipt_long),
                      label: const Text('Test 2: Formatted Receipt'),
                      style: ElevatedButton.styleFrom(
                        minimumSize: const Size.fromHeight(50),
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Prints formatted receipt with alignment and formatting',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),

                    const SizedBox(height: 16),

                    ElevatedButton.icon(
                      onPressed: _isLoading ? null : _testAdvancedPrint,
                      icon: const Icon(Icons.qr_code),
                      label: const Text('Test 3: Barcode & QR Code'),
                      style: ElevatedButton.styleFrom(
                        minimumSize: const Size.fromHeight(50),
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Tests barcode and QR code generation',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            if (_isLoading) const Center(child: CircularProgressIndicator()),

            // Instructions
            Card(
              color: Colors.amber.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.amber),
                        SizedBox(width: 8),
                        Text(
                          'Instructions',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 8),
                    Text(
                      '1. Enter your printer IP address and port\n'
                      '2. Run Test 1 first (simplest test)\n'
                      '3. If Test 1 succeeds, try Test 2\n'
                      '4. If Test 2 succeeds, try Test 3\n'
                      '5. Check console/logcat for detailed logs',
                      style: TextStyle(fontSize: 13),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Important Notes
            Card(
              color: Colors.red.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Row(
                      children: [
                        Icon(Icons.warning_amber, color: Colors.red),
                        SizedBox(width: 8),
                        Text(
                          'Important',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 8),
                    Text(
                      '• Always set openCashbox: false\n'
                      '• Make sure printer is connected to network\n'
                      '• Check IP address is correct\n'
                      '• Port is usually 9100 for most printers',
                      style: TextStyle(fontSize: 13),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _ipController.dispose();
    _portController.dispose();
    super.dispose();
  }
}
