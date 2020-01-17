package vincent.m3u8_downloader;

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
import io.flutter.view.FlutterMain;

public class FlutterM3U8BackgroundExecutor implements MethodChannel.MethodCallHandler {
    private static final  String TAG = "M3u8Downloader background";
    private static PluginRegistry.PluginRegistrantCallback pluginRegistrantCallback;
    private MethodChannel backgroundChannel;
    private FlutterEngine backgroundFlutterEngine;
    private AtomicBoolean isCallbackDispatcherReady = new AtomicBoolean(false);

    public static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        pluginRegistrantCallback = callback;
    }

    public static void setCallbackDispatcher(Context context, long callbackHandle) {
        SharedPreferences prefs = context.getSharedPreferences(M3u8DownloaderPlugin.SHARED_PREFERENCES_KEY, 0);
        prefs.edit().putLong(M3u8DownloaderPlugin.CALLBACK_DISPATCHER_HANDLE_KEY, callbackHandle).apply();
    }

    public boolean isRunning() {
        return isCallbackDispatcherReady.get();
    }

    private void onInitialized() {
        // TODO
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        String method = call.method;
        Object arguments = call.arguments;
        try {
            if (method.equals("didInitializeDispatcher")) {
                onInitialized();
                isCallbackDispatcherReady.set(true);
            }
        } catch(Exception e) {
            result.error("error", "M3u8Download error: " + e.getMessage(), null);
        }
    }

    void startBackgroundIsolate(Context context) {
        if (!isRunning()) {
            SharedPreferences p = context.getSharedPreferences(M3u8DownloaderPlugin.SHARED_PREFERENCES_KEY, 0);
            long callbackHandle = p.getLong(M3u8DownloaderPlugin.CALLBACK_DISPATCHER_HANDLE_KEY, 0);
            startBackgroundIsolate(context, callbackHandle);
        }
    }

    public void startBackgroundIsolate(Context context, long callbackHandle) {
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started");
            return;
        }
        Log.i(TAG, "Starting Background isolate...");
        String appBundlePath = FlutterMain.findAppBundlePath(context);
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

        backgroundChannel.invokeMethod("", new Object[] {callbackHandle, args});
    }

}
