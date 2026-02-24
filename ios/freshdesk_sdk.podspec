Pod::Spec.new do |s|
  s.name             = 'freshdesk_sdk'
  s.version          = '1.0.0'
  s.summary          = 'A Flutter plugin for Freshdesk Mobile SDK integration.'
  s.description      = <<-DESC
A Flutter plugin for integrating Freshdesk Mobile SDK on iOS and Android.
                       DESC
  s.homepage         = 'https://github.com/snapnfix/freshdesk_sdk'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'SnapNFix' => 'support@snapnfix.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '17.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  # Vendored Freshdesk SDK binary
  # We include the FreshdeskSDK.xcframework directly so consumers
  # do not need to fiddle with Swift Package Manager. The framework
  # was copied from the official repository (https://github.com/freshworks/freshdesk-ios-sdk).
  s.vendored_frameworks = 'FreshdeskSDK.xcframework'

  # Optional: you may also add Freshdesk via Swift Package Manager in
  # the host application (Runner) if you prefer. Use this URL:
  #   https://github.com/freshworks/freshdesk-ios-sdk
  # and add package to your Runner target. This is not required when
  # the framework above is present.
  #
  # See IOS_SDK_SETUP.md for updated instructions.
end
