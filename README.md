# freshdesk_sdk

[![pub package](https://img.shields.io/pub/v/freshdesk_sdk.svg)](https://pub.dev/packages/freshdesk_sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A Flutter plugin for integrating **Freshdesk Mobile SDK** on Android and iOS. Provide seamless customer support with native chat, ticketing, and help desk features.

## ✨ Features

- 🚀 **Native Performance**: Uses official Freshdesk Android & iOS SDKs
- 💬 **Real-time Chat**: Live customer support conversations
- 🎫 **Ticketing System**: Create and track support tickets
- 👤 **User Management**: Set custom user properties and identification
- 📊 **Event Tracking**: Track user actions and behaviors
- 🔔 **Push Notifications**: Handle Freshdesk notifications
- 🔐 **JWT Authentication**: Secure user verification
- 🌍 **Localization**: Multi-language support
- 📱 **Unread Badge**: Real-time unread message count

## 📦 Installation

Add to your `pubspec.yaml`:

```yaml
dependencies:
  freshdesk_sdk: ^1.0.0
```

Run:

```bash
flutter pub get
```

## 🔧 Platform Setup

### Android (Automatic ✓)

The plugin automatically includes the Freshdesk Android SDK (v1.0.2). No additional setup required!

**Requirements:**
- minSdkVersion: 26 (Android 8.0)
- compileSdkVersion: 34

### iOS (Simple Setup)

The plugin includes a vendored xcframework. Just update your deployment target:

1. Open `ios/Podfile` and set:
```ruby
platform :ios, '17.0'
```

2. Run:
```bash
cd ios && pod install && cd ..
```

**Requirements:**
- iOS 17.0+
- Swift 5.0+

## 🚀 Quick Start

### 1. Get Freshdesk Credentials

1. Log in to [Freshdesk](https://www.freshdesk.com/) as admin
2. Go to **Admin → Channels → Mobile Chat SDK**
3. Click **Create new** or select existing widget
4. Copy: `token`, `host`, and `sdkId`

### 2. Initialize SDK

```dart
import 'package:freshdesk_sdk/freshdesk_sdk.dart';

// Initialize once in your app
await FreshdeskSDK.initialize(
  FreshdeskConfig(
    token: 'YOUR_TOKEN',
    host: 'https://yourcompany.freshdesk.com',
    sdkId: 'YOUR_SDK_ID',
    jwt: 'your-jwt-token',  // Optional: for verified users
    locale: 'en',            // Optional: language code
    debugMode: true,         // Optional: enable logs
  ),
);
```

### 3. Show Support Chat

```dart
// Open Freshdesk support interface
await FreshdeskSDK.showSupport();
```

That's it! Your users can now chat with support.

## 📚 Advanced Usage

### Set User Properties

```dart
await FreshdeskSDK.setUserProperties({
  'name': 'John Doe',
  'email': 'john@example.com',
  'phone': '+1234567890',
  'custom_field': 'value',
});
```

### Set Ticket Properties

```dart
await FreshdeskSDK.setTicketProperties({
  'subject': 'Payment Issue',
  'priority': 3,
  'type': 'Bug',
  'status': 2,
});
```

### Get Unread Count

```dart
// Get current unread count
final count = await FreshdeskSDK.getUnreadCount();
print('Unread messages: $count');

// Listen to real-time updates
FreshdeskSDK.unreadCountStream.listen((count) {
  setState(() {
    _unreadCount = count;
  });
});
```

### Track Events

```dart
await FreshdeskSDK.trackEvent('purchase_completed', {
  'product_id': 'PRD123',
  'amount': 99.99,
  'currency': 'USD',
});
```

### User Logout

```dart
// Clear user data when user logs out
await FreshdeskSDK.clearUserData();
```

## 🔐 JWT Authentication

For secure user identification:

### 1. Enable JWT in Freshdesk
- Go to **Admin → Channels → Mobile Chat SDK**
- Select your widget → Enable **User authentication**
- Copy the **Secret Key**

### 2. Generate JWT on Your Backend

```javascript
// Node.js example
const jwt = require('jsonwebtoken');

const payload = {
  sub: 'user@example.com',
  name: 'John Doe',
  email: 'user@example.com',
  // Add custom fields
  phone: '+1234567890',
};

const token = jwt.sign(payload, 'YOUR_SECRET_KEY', {
  algorithm: 'HS256',
  expiresIn: '7d',
});
```

### 3. Pass JWT to Plugin

```dart
await FreshdeskSDK.initialize(
  FreshdeskConfig(
    token: 'YOUR_TOKEN',
    host: 'https://yourcompany.freshdesk.com',
    sdkId: 'YOUR_SDK_ID',
    jwt: jwtFromBackend,  // ← Generated JWT
  ),
);
```

## 🔔 Push Notifications (Optional)

### Android

1. Add Firebase to your project
2. The plugin handles notifications automatically when initialized

### iOS

Configure in `AppDelegate.swift`:

```swift
override func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
) {
    Freshdesk.setPushRegistrationToken(deviceToken)
}
```

### Flutter

```dart
// Forward FCM token
await FreshdeskSDK.setPushRegistrationToken(fcmToken);

// Handle notifications
final isFreshdesk = await FreshdeskSDK.isFreshdeskNotification(data);
if (isFreshdesk) {
  await FreshdeskSDK.handleRemoteNotification(data);
}
```

## 📖 API Reference

### Configuration

```dart
class FreshdeskConfig {
  final String token;      // Required: Widget token
  final String host;       // Required: Freshdesk URL
  final String sdkId;      // Required: SDK identifier
  final String? jwt;       // Optional: JWT for authentication
  final String? locale;    // Optional: Language (default: 'en')
  final bool? debugMode;   // Optional: Enable debug logs
}
```

### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `initialize(FreshdeskConfig)` | Initialize SDK | `Future<void>` |
| `showSupport()` | Open support chat | `Future<void>` |
| `setUserProperties(Map)` | Set user details | `Future<void>` |
| `setTicketProperties(Map)` | Set ticket metadata | `Future<void>` |
| `getUnreadCount()` | Get unread count | `Future<int>` |
| `trackEvent(String, Map)` | Track custom event | `Future<void>` |
| `clearUserData()` | Logout user | `Future<void>` |
| `setPushRegistrationToken(String)` | Register FCM token | `Future<void>` |
| `handleRemoteNotification(Map)` | Handle notification | `Future<void>` |
| `isFreshdeskNotification(Map)` | Check if Freshdesk notification | `Future<bool>` |

### Streams

| Stream | Type | Description |
|--------|------|-------------|
| `unreadCountStream` | `Stream<int>` | Real-time unread count updates |

## 🎯 Example App

Check out the [example](./example) directory for a complete working app.

## 🐛 Troubleshooting

### Android: SDK not initializing
- Ensure minSdkVersion is 26+
- Check Freshdesk credentials
- Enable debug mode to see logs

### iOS: Import errors
- Run `cd ios && pod install`
- Ensure platform is set to iOS 17.0
- Clean build: `flutter clean && flutter pub get`

### Push notifications not working
- Verify Firebase setup (Android)
- Check APNS configuration (iOS)
- Ensure FCM token is registered with Freshdesk

## 📋 Requirements

| Platform | Minimum Version |
|----------|----------------|
| Flutter | 3.0.0+ |
| Dart | 3.0.0+ |
| Android | API 26 (Android 8.0) |
| iOS | 17.0+ |

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [Freshdesk Developer Documentation](https://developers.freshdesk.com/)
- [Android SDK Documentation](https://support.freshdesk.com/support/solutions/articles/5000011672)
- [iOS SDK Documentation](https://support.freshdesk.com/support/solutions/articles/50000011673)

## ⭐ Support

For issues and feature requests, please file them on the [issue tracker](https://github.com/ashwindots/freshdesk_sdk/issues).
