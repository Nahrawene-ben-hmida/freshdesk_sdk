package com.snapnfix.freshdesk_sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.NonNull
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.freshworks.sdk.freshdesk.FreshdeskSDK
import com.freshworks.sdk.freshdesk.data.SDKConfig
import com.freshworks.sdk.freshdesk.events.SDKEventID
import com.freshworks.sdk.freshdesk.events.UserState

/** FreshdeskSdkPlugin */
class FreshdeskSdkPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null
    private var context: Context? = null
    
    private var unreadCountReceiver: BroadcastReceiver? = null
    private var userStateReceiver: BroadcastReceiver? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "freshdesk_sdk")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "freshdesk_sdk/unread_count")
        eventChannel.setStreamHandler(this)

        installFreshdeskExceptionGuard()
    }

    /**
     * Installs a global UncaughtExceptionHandler that intercepts Gson parsing errors originating
     * from within the Freshworks SDK's internal background threads.
     *
     * The Freshworks API changed the `meta` field in some responses from a String to an Array.
     * The SDK model still expects a String, so Gson throws JsonSyntaxException on a background
     * thread that is not caught by the SDK — crashing the app. This guard catches those exceptions
     * and logs them instead of letting them propagate.
     *
     * Fix for: https://expensya-ir.sentry.io/issues/7497089777/
     */
    private fun installFreshdeskExceptionGuard() {
        val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isFreshdeskGsonException(throwable)) {
                Log.e(
                    "FreshdeskSDK",
                    "⚠️ Freshdesk SDK internal Gson exception suppressed on thread '${thread.name}': ${throwable.message}",
                )
            } else {
                existingHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun isFreshdeskGsonException(throwable: Throwable): Boolean {
        val cause = throwable.cause ?: throwable
        val isGsonError = cause.javaClass.name.contains("JsonSyntaxException") ||
            cause.message?.contains("Expected a string but was") == true ||
            (cause.message?.contains("Expected a") == true && cause.message?.contains("but was") == true)
        if (!isGsonError) return false
        val trace = throwable.stackTraceToString()
        return trace.contains("com.freshworks") || trace.contains("freshdesk")
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "initialize" -> {
                initialize(call, result)
            }
            "showSupport" -> {
                showSupport(result)
            }
            "setUserProperties" -> {
                setUserProperties(call, result)
            }
            "setTicketProperties" -> {
                setTicketProperties(call, result)
            }
            "getUnreadCount" -> {
                getUnreadCount(result)
            }
            "trackEvent" -> {
                trackEvent(call, result)
            }
            "setPushRegistrationToken" -> {
                setPushRegistrationToken(call, result)
            }
            "handleRemoteNotification" -> {
                handleRemoteNotification(call, result)
            }
            "isFreshdeskNotification" -> {
                isFreshdeskNotification(call, result)
            }
            "clearUserData" -> {
                clearUserData(result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun initialize(call: MethodCall, result: Result) {
        try {
            val token = call.argument<String>("token")
            val host = call.argument<String>("host")
            val sdkId = call.argument<String>("sdkId")
            val jwt = call.argument<String?>("jwt")
            val locale = call.argument<String?>("locale") ?: "en"
            val debugMode = call.argument<Boolean>("debugMode") ?: false

            if (token == null || host == null || sdkId == null) {
                result.error("INVALID_ARGUMENTS", "Token, host, and sdkId are required", null)
                return
            }

            if (context == null) {
                result.error("NO_CONTEXT", "Context not available", null)
                return
            }

            // Initialize Freshdesk SDK with real implementation
            val sdkConfig = SDKConfig(
                token = token,
                host = host,
                sdkID = sdkId,
                locale = locale,
                debugMode = debugMode
            )

            FreshdeskSDK.initialize(context!!, sdkConfig) {
                Log.i("FreshdeskSDK", "✅ Freshdesk SDK Initialized successfully")
                
                // If JWT is provided, authenticate the user
                if (!jwt.isNullOrEmpty()) {
                    try {
                        FreshdeskSDK.authenticateAndUpdate(jwt)
                        Log.i("FreshdeskSDK", "🔐 User authenticated with JWT")
                    } catch (e: Exception) {
                        Log.e("FreshdeskSDK", "❌ Error authenticating user: ${e.message}")
                    }
                }
            }

            // Register user state receiver to monitor authentication
            registerUserStateReceiver()
            
            Log.i("FreshdeskSDK", "✅ Freshdesk SDK setup complete - Token: ${token.take(10)}... Host: $host")
            result.success(true)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error initializing SDK: ${e.message}")
            result.error("INIT_ERROR", e.message, null)
        }
    }

    private fun showSupport(result: Result) {
        try {
            if (context == null) {
                result.error("NO_CONTEXT", "Context not available", null)
                return
            }

            // Open Freshdesk support interface with real SDK
            FreshdeskSDK.openSupport(context!!)
            
            Log.i("FreshdeskSDK", "🚀 Opening Freshdesk support interface")
            result.success(null)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error showing support: ${e.message}")
            result.error("SHOW_SUPPORT_ERROR", e.message, null)
        }
    }

    private fun setUserProperties(call: MethodCall, result: Result) {
        try {
            val properties = call.arguments as? Map<String, Any>
            
            if (properties == null) {
                result.error("INVALID_ARGUMENTS", "Properties map is required", null)
                return
            }

            // Set user properties with real SDK
            FreshdeskSDK.setUserProperties(properties)
            
            Log.i("FreshdeskSDK", "👤 User properties set: $properties")

            result.success(null)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error setting user properties: ${e.message}")
            result.error("SET_USER_PROPERTIES_ERROR", "Failed to set user properties: ${e.message}", null)
        }
    }

    private fun setTicketProperties(call: MethodCall, result: Result) {
        try {
            val properties = call.arguments as? Map<String, Any>
            
            if (properties == null) {
                result.error("INVALID_ARGUMENTS", "Properties map is required", null)
                return
            }

            // Set ticket properties with real SDK
            FreshdeskSDK.setTicketProperties(properties)
            
            Log.i("FreshdeskSDK", "🎫 Ticket properties set: $properties")

            result.success(null)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error setting ticket properties: ${e.message}")
            result.error("SET_TICKET_PROPERTIES_ERROR", "Failed to set ticket properties: ${e.message}", null)
        }
    }

    private fun getUnreadCount(result: Result) {
        try {
            // Get unread count from real Freshdesk SDK - Uses BroadcastReceiver, no direct getter
            // Return 0 here, actual updates come via event stream
            val count = 0
            
            Log.i("FreshdeskSDK", "📬 Unread count requested (use event stream for updates)")
            result.success(count)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error getting unread count: ${e.message}")
            result.error("GET_UNREAD_COUNT_ERROR", "Failed to get unread count: ${e.message}", null)
        }
    }

    private fun trackEvent(call: MethodCall, result: Result) {
        try {
            val eventName = call.argument<String>("eventName")
            val eventData = call.argument<Map<String, Any>>("eventData")

            if (eventName == null) {
                result.error("INVALID_ARGUMENTS", "Event name is required", null)
                return
            }

            // Track event with real SDK
            FreshdeskSDK.trackEvent(eventName, eventData ?: emptyMap())
            
            Log.i("FreshdeskSDK", "📊 Event tracked: $eventName with data: $eventData")

            result.success(null)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error tracking event: ${e.message}")
            result.error("TRACK_EVENT_ERROR", "Failed to track event: ${e.message}", null)
        }
    }

    private fun setPushRegistrationToken(call: MethodCall, result: Result) {
        try {
            val token = call.argument<String>("token")

            if (token == null) {
                result.error("INVALID_ARGUMENTS", "Token is required", null)
                return
            }

            // Set push registration token with real SDK
            FreshdeskSDK.setPushRegistrationToken(token)
            
            Log.i("FreshdeskSDK", "🔔 Push registration token set")

            result.success(null)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error setting push token: ${e.message}")
            result.error("SET_PUSH_TOKEN_ERROR", "Failed to set push token: ${e.message}", null)
        }
    }

    private fun handleRemoteNotification(call: MethodCall, result: Result) {
        try {
            val data = call.arguments as? Map<String, Any>

            if (data == null) {
                result.error("INVALID_ARGUMENTS", "Notification data is required", null)
                return
            }

            if (context == null) {
                result.error("NO_CONTEXT", "Context not available", null)
                return
            }

            // Check if from Freshdesk and handle with real SDK
            @Suppress("UNCHECKED_CAST")
            val stringData = data.mapKeys { it.key.toString() }.mapValues { it.value.toString() } as Map<String, String>
            
            if (FreshdeskSDK.isFreshdeskSDKNotification(stringData)) {
                FreshdeskSDK.handleFCMNotification(stringData)
                Log.i("FreshdeskSDK", "🔔 Freshdesk notification handled")
                result.success(true)
            } else {
                result.success(false)
            }
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "\u274c Error handling remote notification: ${e.message}")
            result.error("HANDLE_NOTIFICATION_ERROR", "Failed to handle notification: ${e.message}", null)
        }
    }

    private fun isFreshdeskNotification(call: MethodCall, result: Result) {
        try {
            val data = call.arguments as? Map<String, Any>

            if (data == null) {
                result.error("INVALID_ARGUMENTS", "Notification data is required", null)
                return
            }

            // Check if notification is from Freshdesk with real SDK
            @Suppress("UNCHECKED_CAST")
            val stringData = data.mapKeys { it.key.toString() }.mapValues { it.value.toString() } as Map<String, String>
            val isFreshdesk = FreshdeskSDK.isFreshdeskSDKNotification(stringData)
            result.success(isFreshdesk)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "\u274c Error checking notification: ${e.message}")
            result.error("CHECK_NOTIF_ERROR", "Failed to check notification: ${e.message}", null)
        }
    }

    private fun clearUserData(result: Result) {
        try {
            // FreshdeskSDK.resetUser() is intentionally skipped.
            // It triggers an authenticated API call where the Freshworks server returns
            // `meta` as a JSON array but the SDK's Gson model expects a String, causing
            // an uncaught JsonSyntaxException on the SDK's background thread → app crash.
            // This only affects users with a Freshdesk JWT (authenticated sessions).
            // User isolation is preserved because the next login calls
            // FreshdeskSDK.authenticateAndUpdate(jwt) with the new user's JWT.
            // TODO: remove this workaround once Freshworks fixes their SDK.
            // Sentry issue: https://expensya-ir.sentry.io/issues/7497089777/
            Log.w("FreshdeskSDK", "⚠️ resetUser skipped — Freshworks SDK meta-array crash workaround")
            result.success(null)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error clearing user data: ${e.message}")
            result.error("CLEAR_USER_DATA_ERROR", "Failed to clear user data: ${e.message}", null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        unregisterUnreadCountReceiver()
        unregisterUserStateReceiver()
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        registerUnreadCountReceiver()
    }

    override fun onCancel(arguments: Any?) {
        unregisterUnreadCountReceiver()
        eventSink = null
    }

    private fun registerUnreadCountReceiver() {
        try {
            unreadCountReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == SDKEventID.UNREAD_COUNT) {
                        val unreadCount = intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0)
                        eventSink?.success(unreadCount)
                        Log.i("FreshdeskSDK", "📬 Unread count update: $unreadCount")
                    }
                }
            }

            val intentFilter = IntentFilter(SDKEventID.UNREAD_COUNT)
            LocalBroadcastManager.getInstance(context!!).registerReceiver(unreadCountReceiver!!, intentFilter)
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error registering unread count receiver: ${e.message}")
        }
    }

    private fun unregisterUnreadCountReceiver() {
        try {
            if (unreadCountReceiver != null && context != null) {
                LocalBroadcastManager.getInstance(context!!).unregisterReceiver(unreadCountReceiver!!)
                unreadCountReceiver = null
            }
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error unregistering unread count receiver: ${e.message}")
        }
    }

    private fun registerUserStateReceiver() {
        try {
            userStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == SDKEventID.USER_STATE_CHANGE) {
                        val userState = intent.getStringExtra(SDKEventID.USER_STATE_CHANGE)
                        when (userState) {
                            UserState.AUTHENTICATED -> {
                                Log.i("FreshdeskSDK", "✅ User authenticated")
                            }
                            UserState.NOT_AUTHENTICATED -> {
                                Log.w("FreshdeskSDK", "⚠️ User not authenticated")
                            }
                            UserState.AUTH_EXPIRED -> {
                                Log.w("FreshdeskSDK", "⚠️ Authentication expired")
                            }
                            UserState.JWT_ABSENT -> {
                                Log.w("FreshdeskSDK", "⚠️ JWT token missing")
                            }
                            else -> Log.d("FreshdeskSDK", "User state: $userState")
                        }
                    }
                }
            }

            val intentFilter = IntentFilter(SDKEventID.USER_STATE_CHANGE)
            context?.let {
                LocalBroadcastManager.getInstance(it).registerReceiver(userStateReceiver!!, intentFilter)
            }
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error registering user state receiver: ${e.message}")
        }
    }

    private fun unregisterUserStateReceiver() {
        try {
            if (userStateReceiver != null && context != null) {
                LocalBroadcastManager.getInstance(context!!).unregisterReceiver(userStateReceiver!!)
                userStateReceiver = null
            }
        } catch (e: Exception) {
            Log.e("FreshdeskSDK", "❌ Error unregistering user state receiver: ${e.message}")
        }
    }
}
