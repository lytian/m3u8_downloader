package com.vincent.m3u8Downloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.Log;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterCallbackInformation;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/26 16:19
 * @Desc: 初始化运行回调调度程序的后台隔离，用于在后台启动时调用Dart回调。
 */
public class FlutterBackgroundExecutor implements MethodChannel.MethodCallHandler {
    public static final String SHARED_PREFERENCES_KEY = "vincent.m3u8.downloader.pref";
    public static final String CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatcher_handle_key";
    private static final String TAG = "M3u8Downloader background";
    @SuppressWarnings("deprecation")
    private static PluginRegistry.PluginRegistrantCallback pluginRegistrantCallback;
    private MethodChannel backgroundChannel;
    private FlutterEngine backgroundFlutterEngine;
    private final AtomicBoolean isCallbackDispatcherReady = new AtomicBoolean(false);

    public static void setCallbackDispatcher(Context context, long callbackHandle) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0);
        prefs.edit().putLong(CALLBACK_DISPATCHER_HANDLE_KEY, callbackHandle).apply();
    }

    public boolean isRunning() {
        return isCallbackDispatcherReady.get();
    }

    private void onInitialized() {
        isCallbackDispatcherReady.set(true);
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        String method = call.method;
        if (method.equals("didInitializeDispatcher")) {
            onInitialized();
            result.success(true);
        } else {
            result.notImplemented();
        }
    }

    void startBackgroundIsolate(Context context) {
        if (!isRunning()) {
            SharedPreferences p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0);
            long callbackHandle = p.getLong(CALLBACK_DISPATCHER_HANDLE_KEY, 0);
            startBackgroundIsolate(context, callbackHandle);

        }
    }

    public void startBackgroundIsolate(Context context, long callbackHandle) {
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started");
            return;
        }
        Log.i(TAG, "Starting Background isolate...");
        @SuppressWarnings("deprecation")
        String appBundlePath = io.flutter.view.FlutterMain.findAppBundlePath(context);
        AssetManager assets = context.getAssets();
        if (appBundlePath != null && !isRunning()) {
            backgroundFlutterEngine = new FlutterEngine(context);
            FlutterCallbackInformation flutterCallback = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle);
            if (flutterCallback == null) {
                Log.e(TAG, "Fatal: failed to find callback");
                return;
            }
            DartExecutor executor = backgroundFlutterEngine.getDartExecutor();
            initializeMethodChannel(executor);
            DartExecutor.DartCallback dartCallback = new DartExecutor.DartCallback(assets, appBundlePath, flutterCallback);

            executor.executeDartCallback(dartCallback);

            if (pluginRegistrantCallback != null) {
                pluginRegistrantCallback.registerWith(new ShimPluginRegistry(backgroundFlutterEngine));
            }
        }
    }

    private void initializeMethodChannel(BinaryMessenger isolate) {
        backgroundChannel = new MethodChannel(isolate, "vincent/m3u8_downloader_background", JSONMethodCodec.INSTANCE);
        backgroundChannel.setMethodCallHandler(this);
    }

    public void executeDartCallbackInBackgroundIsolate(long callbackHandle, Object args) {
        backgroundChannel.invokeMethod("", new Object[]{callbackHandle, args});
    }
}
