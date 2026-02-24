# Changelog

## 1.0.0 - 2026-02-24

### Initial Release

#### Features
* ✅ **Cross-platform Support**: Works on Android (API 26+) and iOS (17.0+)
* ✅ **Native SDK Integration**: Uses official Freshdesk Mobile SDKs
  * Android: Freshdesk Android SDK v1.0.2
  * iOS: Freshdesk iOS SDK v1.1.0 (vendored xcframework)
* ✅ **Complete API Coverage**:
  * Initialize SDK with custom configuration
  * Show support/chat interface
  * Set user properties
  * Set ticket properties  
  * Get unread message count
  * Real-time unread count stream
  * Track custom events
  * JWT authentication
  * Push notification handling
  * User session management

#### Android Implementation
* Native Kotlin plugin using Freshdesk Android SDK v1.0.2
* Maven Central dependency integration
* Firebase messaging support
* Background service handling
* Method channel and event channel communication

#### iOS Implementation
* Native Swift plugin using Freshdesk iOS SDK v1.1.0
* Vendored xcframework (no SPM setup required)
* CocoaPods integration
* Notification center observers
* Proper type conversion for SDK APIs

#### Developer Experience
* Simple initialization with `FreshdeskConfig`
* Streaming updates via `unreadCountStream`
* Comprehensive error handling
* Debug mode support
* Locale configuration

#### Documentation
* Detailed README with setup instructions
* API reference and usage examples
* Platform-specific configuration guides
* JWT authentication documentation
* Push notification integration guide

### Breaking Changes
None - initial release

### Migration Guide
None - initial release

### Known Issues
* iOS minimum deployment target is 17.0 (SDK requirement)
* Android minSdkVersion is 26 (SDK requirement)

### Dependencies
* Flutter SDK: >=3.0.0
* Dart SDK: >=3.0.0 <4.0.0
* Android: Freshdesk SDK 1.0.2
* iOS: Freshdesk SDK 1.1.0 (bundled)
