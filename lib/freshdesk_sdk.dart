import 'dart:async';
import 'package:flutter/services.dart';

/// Freshdesk SDK configuration
class FreshdeskConfig {
  final String token;
  final String host;
  final String sdkId;
  final String? jwt;
  final String? locale;
  final bool debugMode;

  FreshdeskConfig({
    required this.token,
    required this.host,
    required this.sdkId,
    this.jwt,
    this.locale,
    this.debugMode = false,
  });

  Map<String, dynamic> toMap() {
    return {
      'token': token,
      'host': host,
      'sdkId': sdkId,
      'jwt': jwt,
      'locale': locale,
      'debugMode': debugMode,
    };
  }
}

/// Main Freshdesk SDK class
class FreshdeskSDK {
  static const MethodChannel _channel = MethodChannel('freshdesk_sdk');
  static const EventChannel _unreadCountChannel = EventChannel('freshdesk_sdk/unread_count');
  
  static Stream<int>? _unreadCountStream;

  /// Initialize the Freshdesk SDK
  static Future<bool> initialize(FreshdeskConfig config) async {
    try {
      final result = await _channel.invokeMethod('initialize', config.toMap());
      return result == true;
    } catch (e) {
      print('Error initializing Freshdesk SDK: $e');
      return false;
    }
  }

  /// Show the Freshdesk chat interface
  static Future<void> showSupport() async {
    try {
      await _channel.invokeMethod('showSupport');
    } catch (e) {
      print('Error showing support: $e');
    }
  }

  /// Set user properties
  static Future<void> setUserProperties(Map<String, dynamic> properties) async {
    try {
      await _channel.invokeMethod('setUserProperties', properties);
    } catch (e) {
      print('Error setting user properties: $e');
    }
  }

  /// Set ticket properties
  static Future<void> setTicketProperties(Map<String, dynamic> properties) async {
    try {
      await _channel.invokeMethod('setTicketProperties', properties);
    } catch (e) {
      print('Error setting ticket properties: $e');
    }
  }

  /// Get unread count
  static Future<int> getUnreadCount() async {
    try {
      final count = await _channel.invokeMethod('getUnreadCount');
      return count ?? 0;
    } catch (e) {
      print('Error getting unread count: $e');
      return 0;
    }
  }

  /// Listen to unread count updates
  static Stream<int> get unreadCountStream {
    _unreadCountStream ??= _unreadCountChannel.receiveBroadcastStream().map((count) => count as int? ?? 0);
    return _unreadCountStream!;
  }

  /// Track user events
  static Future<void> trackEvent(String eventName, Map<String, dynamic> eventData) async {
    try {
      await _channel.invokeMethod('trackEvent', {
        'eventName': eventName,
        'eventData': eventData,
      });
    } catch (e) {
      print('Error tracking event: $e');
    }
  }

  /// Set push registration token (for FCM/APNs)
  static Future<void> setPushRegistrationToken(String token) async {
    try {
      await _channel.invokeMethod('setPushRegistrationToken', {'token': token});
    } catch (e) {
      print('Error setting push token: $e');
    }
  }

  /// Handle remote notification (call this when a push notification is received)
  static Future<void> handleRemoteNotification(Map<String, dynamic> data) async {
    try {
      await _channel.invokeMethod('handleRemoteNotification', data);
    } catch (e) {
      print('Error handling remote notification: $e');
    }
  }

  /// Check if a notification is from Freshdesk
  static Future<bool> isFreshdeskNotification(Map<String, dynamic> data) async {
    try {
      final result = await _channel.invokeMethod('isFreshdeskNotification', data);
      return result == true;
    } catch (e) {
      print('Error checking Freshdesk notification: $e');
      return false;
    }
  }

  /// Clear user data and logout
  static Future<void> clearUserData() async {
    try {
      await _channel.invokeMethod('clearUserData');
    } catch (e) {
      print('Error clearing user data: $e');
    }
  }
}
