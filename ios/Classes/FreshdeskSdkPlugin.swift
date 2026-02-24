import Flutter
import UIKit
import Foundation
import FreshdeskSDK

public class FreshdeskSdkPlugin: NSObject, FlutterPlugin, FlutterStreamHandler {
    private var eventSink: FlutterEventSink?
    private var unreadCountObserver: NSObjectProtocol?
    private static var sdkInitialized = false
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "freshdesk_sdk", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "freshdesk_sdk/unread_count", binaryMessenger: registrar.messenger())
        
        let instance = FreshdeskSdkPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        eventChannel.setStreamHandler(instance)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initialize":
            initialize(call: call, result: result)
        case "showSupport":
            showSupport(result: result)
        case "setUserProperties":
            setUserProperties(call: call, result: result)
        case "setTicketProperties":
            setTicketProperties(call: call, result: result)
        case "getUnreadCount":
            getUnreadCount(result: result)
        case "trackEvent":
            trackEvent(call: call, result: result)
        case "setPushRegistrationToken":
            setPushRegistrationToken(call: call, result: result)
        case "handleRemoteNotification":
            handleRemoteNotification(call: call, result: result)
        case "isFreshdeskNotification":
            isFreshdeskNotification(call: call, result: result)
        case "clearUserData":
            clearUserData(result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func initialize(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let token = args["token"] as? String,
              let host = args["host"] as? String,
              let sdkId = args["sdkId"] as? String else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Token, host, and sdkId are required", details: nil))
            return
        }
        
        let jwt = args["jwt"] as? String
        let locale = args["locale"] as? String ?? "en"
        
        // initialize Freshdesk SDK
        let sdkConfig = FreshdeskSDKConfig(
            token: token,
            host: host,
            sdkId: sdkId,
            jwtToken: jwt,
            locale: locale
        )
        Freshdesk.initialize(with: sdkConfig)
        FreshdeskSdkPlugin.sdkInitialized = true
        
        NSLog("✅ Freshdesk SDK Initialized - Token: %@... Host: %@", String(token.prefix(10)), host)
        if jwt != nil {
            NSLog("🔐 User authenticated with JWT")
        }
        
        result(true)
    }
    
    private func showSupport(result: @escaping FlutterResult) {
        guard FreshdeskSdkPlugin.sdkInitialized else {
            result(FlutterError(code: "SDK_NOT_INITIALIZED", message: "Freshdesk SDK not initialized", details: nil))
            return
        }
        
        // Get root view controller
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            result(FlutterError(code: "NO_VIEW_CONTROLLER", message: "No root view controller found", details: nil))
            return
        }
        
        // Open Freshdesk support interface with real SDK
        DispatchQueue.main.async {
            Freshdesk.openSupport(rootViewController)
            NSLog("🚀 Freshdesk support opened")
        }
        
        result(nil)
    }
    
    private func setUserProperties(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let properties = call.arguments as? [String: Any] else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Properties map is required", details: nil))
            return
        }
        
        // Convert values to strings (SDK expects [String: String])
        let stringProps = properties.reduce(into: [String:String]()) { acc, pair in
            acc[pair.key] = "\(pair.value)"
        }
        Freshdesk.setUserDetails(with: stringProps)
        NSLog("👤 User properties set: %@", stringProps)
        
        result(nil)
    }
    
    private func setTicketProperties(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let properties = call.arguments as? [String: Any] else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Properties map is required", details: nil))
            return
        }
        
        // Convert values to strings (SDK expects [String: String])
        let stringProps = properties.reduce(into: [String:String]()) { acc, pair in
            acc[pair.key] = "\(pair.value)"
        }
        Freshdesk.setTicketProperties(with: stringProps)
        NSLog("🎫 Ticket properties set: %@", stringProps)
        
        result(nil)
    }
    
    private func getUnreadCount(result: @escaping FlutterResult) {
        // Get unread count from real SDK
        let count = Freshdesk.getUnreadCount()
        NSLog("📬 Unread count: %d", count)
        result(count)
    }
    
    private func trackEvent(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let eventName = args["eventName"] as? String else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Event name is required", details: nil))
            return
        }
        
        let eventDataAny = args["eventData"] as? [String: Any] ?? [:]
        // convert to string dictionary
        let eventData = eventDataAny.reduce(into: [String:String]()) { acc, pair in
            acc[pair.key] = "\(pair.value)"
        }
        
        // Track event with real SDK
        Freshdesk.trackUserEvents(name: eventName, payload: eventData)
        NSLog("📊 Event tracked: %@ with data: %@", eventName, eventData)
        
        result(nil)
    }
    
    private func setPushRegistrationToken(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let token = args["token"] as? String else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Token is required", details: nil))
            return
        }
        
        // Set push registration token with real SDK
        if let tokenData = token.data(using: .utf8) {
            Freshdesk.setPushRegistrationToken(tokenData)
            NSLog("🔔 Push registration token set")
        }
        
        result(nil)
    }
    
    private func handleRemoteNotification(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let data = call.arguments as? [String: Any] else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Notification data is required", details: nil))
            return
        }
        
        // Handle notification with real SDK
        let appState = UIApplication.shared.applicationState
        if Freshdesk.isFreshdeskNotification(data) {
            Freshdesk.handleRemoteNotification(data, appState: appState)
            NSLog("🔔 Freshdesk notification handled")
            result(true)
        } else {
            result(false)
        }
    }
    
    private func isFreshdeskNotification(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let data = call.arguments as? [String: Any] else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Notification data is required", details: nil))
            return
        }
        
        // Check if notification is from Freshdesk
        let isFreshdesk = Freshdesk.isFreshdeskNotification(data)
        result(isFreshdesk)
    }
    
    private func clearUserData(result: @escaping FlutterResult) {
        // Clear user / session data with SDK
        // the iOS API uses `resetUser` rather than clearUserData
        Freshdesk.resetUser()
        NSLog("🗑️ User data cleared (reset)")
        
        result(nil)
    }
    
    // MARK: - FlutterStreamHandler
    
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        eventSink = events
        
        // Listen for unread count updates from Freshdesk SDK
        unreadCountObserver = NotificationCenter.default.addObserver(
            forName: Notification.Name("FDUnreadCountChanged"),
            object: nil,
            queue: .main
        ) { [weak self] notification in
            if let unreadCount = notification.userInfo?["unreadCount"] as? Int {
                self?.eventSink?(unreadCount)
                NSLog("📬 Unread count update: %d", unreadCount)
            }
        }
        
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        if let observer = unreadCountObserver {
            NotificationCenter.default.removeObserver(observer)
            unreadCountObserver = nil
        }
        eventSink = nil
        return nil
    }
}
