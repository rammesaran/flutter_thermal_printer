import Flutter
import UIKit
import Network

public class FlutterThermalPrinterPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_thermal_printer", binaryMessenger: registrar.messenger())
    let instance = FlutterThermalPrinterPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
      result("iOS " + UIDevice.current.systemVersion)
    case "printTcp":
      handlePrintTcp(call: call, result: result)
    case "printBluetooth":
      handlePrintBluetooth(call: call, result: result)
    default:
      result(FlutterMethodNotImplemented)
    }
  }
  
  private func handlePrintTcp(call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let arguments = call.arguments as? [String: Any],
          let ip = arguments["ip"] as? String,
          let port = arguments["port"] as? Int,
          let payload = arguments["payload"] as? String else {
      result(FlutterError(code: "INVALID_ARGUMENTS", message: "IP, port, and payload are required", details: nil))
      return
    }
    
    let timeout = arguments["timeout"] as? Int ?? 30000
    
    DispatchQueue.global(qos: .background).async {
      do {
        try self.sendDataToTcpPrinter(ip: ip, port: port, data: payload, timeout: timeout)
        DispatchQueue.main.async {
          result(nil)
        }
      } catch {
        DispatchQueue.main.async {
          result(FlutterError(code: "PRINT_ERROR", message: "Failed to print via TCP: \(error.localizedDescription)", details: nil))
        }
      }
    }
  }
  
  private func handlePrintBluetooth(call: FlutterMethodCall, result: @escaping FlutterResult) {
    // For now, return an error as Bluetooth printing requires more complex implementation
    result(FlutterError(code: "NOT_IMPLEMENTED", message: "Bluetooth printing is not yet implemented on iOS", details: nil))
  }
  
  private func sendDataToTcpPrinter(ip: String, port: Int, data: String, timeout: Int) throws {
    let semaphore = DispatchSemaphore(value: 0)
    var connectionError: Error?
    
    guard let host = IPv4Address(ip) else {
      throw NSError(domain: "InvalidIPAddress", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid IP address"])
    }
    
    let connection = NWConnection(host: .ipv4(host), port: .init(integerLiteral: UInt16(port)), using: .tcp)
    
    connection.stateUpdateHandler = { state in
      switch state {
      case .ready:
        // Send the data
        let escPosData = self.convertToESCPOS(payload: data)
        connection.send(content: escPosData, completion: .contentProcessed { error in
          if let error = error {
            connectionError = error
          }
          connection.cancel()
          semaphore.signal()
        })
      case .failed(let error):
        connectionError = error
        semaphore.signal()
      case .cancelled:
        semaphore.signal()
      default:
        break
      }
    }
    
    connection.start(queue: .global())
    
    // Wait for completion or timeout
    let timeoutResult = semaphore.wait(timeout: .now() + .milliseconds(timeout))
    
    if timeoutResult == .timedOut {
      connection.cancel()
      throw NSError(domain: "ConnectionTimeout", code: 2, userInfo: [NSLocalizedDescriptionKey: "Connection timed out"])
    }
    
    if let error = connectionError {
      throw error
    }
  }
  
  private func convertToESCPOS(payload: String) -> Data {
    // Basic ESC/POS formatting - this is a simplified version
    // In a full implementation, you'd want to parse the formatted text properly
    var escPosCommands = Data()
    
    // Initialize printer
    escPosCommands.append(Data([0x1B, 0x40])) // ESC @
    
    // Convert payload string to data
    if let textData = payload.data(using: .utf8) {
      escPosCommands.append(textData)
    }
    
    // Line feed
    escPosCommands.append(Data([0x0A]))
    
    // Cut paper (if auto-cut is enabled)
    escPosCommands.append(Data([0x1B, 0x64, 0x02])) // ESC d 2 (feed and cut)
    
    return escPosCommands
  }
}
