package com.snapnfix.freshdesk_sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.freshworks.sdk.freshdesk.FreshdeskSDK;
import com.freshworks.sdk.freshdesk.data.SDKConfig;
import com.freshworks.sdk.freshdesk.events.SDKEventID;
import com.freshworks.sdk.freshdesk.events.UserState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class FreshdeskSdkPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink eventSink;
    private Context context;

    private BroadcastReceiver unreadCountReceiver;
    private BroadcastReceiver userStateReceiver;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "freshdesk_sdk");
        channel.setMethodCallHandler(this);

        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "freshdesk_sdk/unread_count");
        eventChannel.setStreamHandler(this);

        installFreshdeskExceptionGuard();
    }

    private void installFreshdeskExceptionGuard() {
        final Thread.UncaughtExceptionHandler existingHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (isFreshdeskGsonException(throwable)) {
                Log.e(
                    "FreshdeskSDK",
                    "Freshdesk SDK internal Gson exception suppressed on thread '" + thread.getName() + "': " + throwable.getMessage()
                );
            } else if (existingHandler != null) {
                existingHandler.uncaughtException(thread, throwable);
            }
        });
    }

    private boolean isFreshdeskGsonException(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String className = cause.getClass().getName();
        String message = cause.getMessage();
        boolean isGsonError =
            className.contains("JsonSyntaxException") ||
            (message != null && message.contains("Expected a string but was")) ||
            (message != null && message.contains("Expected a") && message.contains("but was"));

        if (!isGsonError) {
            return false;
        }

        String trace = Log.getStackTraceString(throwable);
        return trace.contains("com.freshworks") || trace.contains("freshdesk");
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "initialize":
                initialize(call, result);
                break;
            case "showSupport":
                showSupport(result);
                break;
            case "setUserProperties":
                setUserProperties(call, result);
                break;
            case "setTicketProperties":
                setTicketProperties(call, result);
                break;
            case "getUnreadCount":
                getUnreadCount(result);
                break;
            case "trackEvent":
                trackEvent(call, result);
                break;
            case "setPushRegistrationToken":
                setPushRegistrationToken(call, result);
                break;
            case "handleRemoteNotification":
                handleRemoteNotification(call, result);
                break;
            case "isFreshdeskNotification":
                isFreshdeskNotification(call, result);
                break;
            case "clearUserData":
                clearUserData(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void initialize(MethodCall call, MethodChannel.Result result) {
        try {
            String token = call.argument("token");
            String host = call.argument("host");
            String sdkId = call.argument("sdkId");
            String jwt = call.argument("jwt");
            String locale = call.argument("locale");
            Boolean debugModeValue = call.argument("debugMode");
            boolean debugMode = debugModeValue != null && debugModeValue;

            if (locale == null) {
                locale = "en";
            }

            if (token == null || host == null || sdkId == null) {
                result.error("INVALID_ARGUMENTS", "Token, host, and sdkId are required", null);
                return;
            }

            if (context == null) {
                result.error("NO_CONTEXT", "Context not available", null);
                return;
            }

            SDKConfig sdkConfig = new SDKConfig(
                token,
                host,
                sdkId,
                locale,
                null,
                Collections.emptyMap(),
                "",
                "",
                debugMode
            );

            final String freshdeskJwt = jwt;
            FreshdeskSDK.INSTANCE.initialize(context, sdkConfig, new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    Log.i("FreshdeskSDK", "Freshdesk SDK Initialized successfully");
                    if (freshdeskJwt != null && !freshdeskJwt.isEmpty()) {
                        try {
                            FreshdeskSDK.INSTANCE.authenticateAndUpdate(freshdeskJwt);
                            Log.i("FreshdeskSDK", "User authenticated with JWT");
                        } catch (Exception e) {
                            Log.e("FreshdeskSDK", "Error authenticating user: " + e.getMessage());
                        }
                    }
                    return Unit.INSTANCE;
                }
            });

            registerUserStateReceiver();

            Log.i("FreshdeskSDK", "Freshdesk SDK setup complete - Token: " + token.substring(0, Math.min(10, token.length())) + "... Host: " + host);
            result.success(true);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error initializing SDK: " + e.getMessage());
            result.error("INIT_ERROR", e.getMessage(), null);
        }
    }

    private void showSupport(MethodChannel.Result result) {
        try {
            if (context == null) {
                result.error("NO_CONTEXT", "Context not available", null);
                return;
            }

            FreshdeskSDK.INSTANCE.openSupport(context);
            Log.i("FreshdeskSDK", "Opening Freshdesk support interface");
            result.success(null);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error showing support: " + e.getMessage());
            result.error("SHOW_SUPPORT_ERROR", e.getMessage(), null);
        }
    }

    private void setUserProperties(MethodCall call, MethodChannel.Result result) {
        try {
            Map<String, Object> properties = getMapArguments(call);
            if (properties == null) {
                result.error("INVALID_ARGUMENTS", "Properties map is required", null);
                return;
            }

            FreshdeskSDK.INSTANCE.setUserProperties(properties);
            Log.i("FreshdeskSDK", "User properties set: " + properties);
            result.success(null);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error setting user properties: " + e.getMessage());
            result.error("SET_USER_PROPERTIES_ERROR", "Failed to set user properties: " + e.getMessage(), null);
        }
    }

    private void setTicketProperties(MethodCall call, MethodChannel.Result result) {
        try {
            Map<String, Object> properties = getMapArguments(call);
            if (properties == null) {
                result.error("INVALID_ARGUMENTS", "Properties map is required", null);
                return;
            }

            FreshdeskSDK.INSTANCE.setTicketProperties(properties);
            Log.i("FreshdeskSDK", "Ticket properties set: " + properties);
            result.success(null);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error setting ticket properties: " + e.getMessage());
            result.error("SET_TICKET_PROPERTIES_ERROR", "Failed to set ticket properties: " + e.getMessage(), null);
        }
    }

    private void getUnreadCount(MethodChannel.Result result) {
        try {
            Log.i("FreshdeskSDK", "Unread count requested (use event stream for updates)");
            result.success(0);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error getting unread count: " + e.getMessage());
            result.error("GET_UNREAD_COUNT_ERROR", "Failed to get unread count: " + e.getMessage(), null);
        }
    }

    private void trackEvent(MethodCall call, MethodChannel.Result result) {
        try {
            String eventName = call.argument("eventName");
            Map<String, Object> eventData = call.argument("eventData");

            if (eventName == null) {
                result.error("INVALID_ARGUMENTS", "Event name is required", null);
                return;
            }

            FreshdeskSDK.INSTANCE.trackEvent(eventName, eventData != null ? eventData : Collections.emptyMap());
            Log.i("FreshdeskSDK", "Event tracked: " + eventName + " with data: " + eventData);
            result.success(null);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error tracking event: " + e.getMessage());
            result.error("TRACK_EVENT_ERROR", "Failed to track event: " + e.getMessage(), null);
        }
    }

    private void setPushRegistrationToken(MethodCall call, MethodChannel.Result result) {
        try {
            String token = call.argument("token");
            if (token == null) {
                result.error("INVALID_ARGUMENTS", "Token is required", null);
                return;
            }

            FreshdeskSDK.INSTANCE.setPushRegistrationToken(token);
            Log.i("FreshdeskSDK", "Push registration token set");
            result.success(null);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error setting push token: " + e.getMessage());
            result.error("SET_PUSH_TOKEN_ERROR", "Failed to set push token: " + e.getMessage(), null);
        }
    }

    private void handleRemoteNotification(MethodCall call, MethodChannel.Result result) {
        try {
            Map<String, Object> data = getMapArguments(call);
            if (data == null) {
                result.error("INVALID_ARGUMENTS", "Notification data is required", null);
                return;
            }

            if (context == null) {
                result.error("NO_CONTEXT", "Context not available", null);
                return;
            }

            Map<String, String> stringData = toStringMap(data);
            if (FreshdeskSDK.INSTANCE.isFreshdeskSDKNotification(stringData)) {
                FreshdeskSDK.INSTANCE.handleFCMNotification(stringData);
                Log.i("FreshdeskSDK", "Freshdesk notification handled");
                result.success(true);
            } else {
                result.success(false);
            }
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error handling remote notification: " + e.getMessage());
            result.error("HANDLE_NOTIFICATION_ERROR", "Failed to handle notification: " + e.getMessage(), null);
        }
    }

    private void isFreshdeskNotification(MethodCall call, MethodChannel.Result result) {
        try {
            Map<String, Object> data = getMapArguments(call);
            if (data == null) {
                result.error("INVALID_ARGUMENTS", "Notification data is required", null);
                return;
            }

            result.success(FreshdeskSDK.INSTANCE.isFreshdeskSDKNotification(toStringMap(data)));
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error checking notification: " + e.getMessage());
            result.error("CHECK_NOTIF_ERROR", "Failed to check notification: " + e.getMessage(), null);
        }
    }

    private void clearUserData(MethodChannel.Result result) {
        try {
            Log.w("FreshdeskSDK", "resetUser skipped - Freshworks SDK meta-array crash workaround");
            result.success(null);
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error clearing user data: " + e.getMessage());
            result.error("CLEAR_USER_DATA_ERROR", "Failed to clear user data: " + e.getMessage(), null);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
        }
        unregisterUnreadCountReceiver();
        unregisterUserStateReceiver();
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
        registerUnreadCountReceiver();
    }

    @Override
    public void onCancel(Object arguments) {
        unregisterUnreadCountReceiver();
        eventSink = null;
    }

    private void registerUnreadCountReceiver() {
        try {
            if (context == null) {
                return;
            }

            unreadCountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && SDKEventID.UNREAD_COUNT.equals(intent.getAction())) {
                        int unreadCount = intent.getIntExtra(SDKEventID.UNREAD_COUNT, 0);
                        if (eventSink != null) {
                            eventSink.success(unreadCount);
                        }
                        Log.i("FreshdeskSDK", "Unread count update: " + unreadCount);
                    }
                }
            };

            LocalBroadcastManager.getInstance(context).registerReceiver(unreadCountReceiver, new IntentFilter(SDKEventID.UNREAD_COUNT));
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error registering unread count receiver: " + e.getMessage());
        }
    }

    private void unregisterUnreadCountReceiver() {
        try {
            if (unreadCountReceiver != null && context != null) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(unreadCountReceiver);
                unreadCountReceiver = null;
            }
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error unregistering unread count receiver: " + e.getMessage());
        }
    }

    private void registerUserStateReceiver() {
        try {
            if (context == null) {
                return;
            }

            userStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && SDKEventID.USER_STATE_CHANGE.equals(intent.getAction())) {
                        String userState = intent.getStringExtra(SDKEventID.USER_STATE_CHANGE);
                        if (UserState.AUTHENTICATED.equals(userState)) {
                            Log.i("FreshdeskSDK", "User authenticated");
                        } else if (UserState.NOT_AUTHENTICATED.equals(userState)) {
                            Log.w("FreshdeskSDK", "User not authenticated");
                        } else if (UserState.AUTH_EXPIRED.equals(userState)) {
                            Log.w("FreshdeskSDK", "Authentication expired");
                        } else if (UserState.JWT_ABSENT.equals(userState)) {
                            Log.w("FreshdeskSDK", "JWT token missing");
                        } else {
                            Log.d("FreshdeskSDK", "User state: " + userState);
                        }
                    }
                }
            };

            LocalBroadcastManager.getInstance(context).registerReceiver(userStateReceiver, new IntentFilter(SDKEventID.USER_STATE_CHANGE));
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error registering user state receiver: " + e.getMessage());
        }
    }

    private void unregisterUserStateReceiver() {
        try {
            if (userStateReceiver != null && context != null) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(userStateReceiver);
                userStateReceiver = null;
            }
        } catch (Exception e) {
            Log.e("FreshdeskSDK", "Error unregistering user state receiver: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapArguments(MethodCall call) {
        if (call.arguments instanceof Map) {
            return (Map<String, Object>) call.arguments;
        }
        return null;
    }

    private Map<String, String> toStringMap(Map<String, Object> data) {
        Map<String, String> stringData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            stringData.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return stringData;
    }
}
