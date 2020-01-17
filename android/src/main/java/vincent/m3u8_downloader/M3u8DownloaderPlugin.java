package vincent.m3u8_downloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import vincent.m3u8_downloader.bean.M3U8Task;
import vincent.m3u8_downloader.utils.MUtils;

/** FlutterM3U8DownloaderPlugin */
public class M3u8DownloaderPlugin implements FlutterPlugin, MethodCallHandler {
  private static final  String TAG = "M3u8Downloader";
  private static final String CHANNEL_NAME = "vincent/m3u8_downloader";
  public static final String SHARED_PREFERENCES_KEY = "vincent.m3u8.downloader.pref";
  public static final String CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatcher_handle_key";


  private static M3u8DownloaderPlugin instance;
  private MethodChannel channel;
  private Context context;
  private Handler handler;
  private Object initializationLock = new Object();
  FlutterM3U8BackgroundExecutor flutterM3U8BackgroundExecutor = new FlutterM3U8BackgroundExecutor();

  public static void registerWith(Registrar registrar) {
    Log.e(TAG, "registerWith");
    if (instance == null) {
      instance = new M3u8DownloaderPlugin();
    }
    instance.onAttachedToEngine(registrar.context(), registrar.messenger());
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    onAttachedToEngine( flutterPluginBinding.getApplicationContext(), flutterPluginBinding.getFlutterEngine().getDartExecutor());
  }

  private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
    synchronized (initializationLock) {
      if (channel != null) {
        return;
      }
      this.context = applicationContext;
      handler = new Handler(Looper.getMainLooper());

      channel = new MethodChannel( messenger, CHANNEL_NAME, JSONMethodCodec.INSTANCE);
      channel.setMethodCallHandler(this);
    }
  }


  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      if (call.method.equals("initialize")) {
        long callbackHandle = ((JSONArray) call.arguments).getLong(0);

        flutterM3U8BackgroundExecutor.setCallbackDispatcher(context, callbackHandle);
        flutterM3U8BackgroundExecutor.startBackgroundIsolate(context);
        result.success(true);
      } else if (call.method.equals("config")) {
        if (!call.hasArgument("saveDir")) {
          result.error("1", "saveDir必传", "");
          return;
        }
        M3U8DownloaderConfig config = M3U8DownloaderConfig.build(context);
        String saveDir = call.argument("saveDir");
        config.setSaveDir(saveDir);
        if (call.hasArgument("connTimeout") && call.argument("connTimeout") != JSONObject.NULL) {
          int connTimeout = call.argument("connTimeout");
          config.setConnTimeout(connTimeout);
        }
        if (call.hasArgument("readTimeout") && call.argument("readTimeout") != JSONObject.NULL) {
          int readTimeout = call.argument("readTimeout");
          config.setReadTimeout(readTimeout);
        }
        if (call.hasArgument("debugMode") && call.argument("debugMode") != JSONObject.NULL) {
          boolean debugMode = call.argument("debugMode");
          config.setDebugMode(debugMode);
        }
        result.success(true);
      } else if (call.method.equals("download")) {
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String url = call.argument("url");

        final long progressCallbackHandle = call.hasArgument("progressCallback") && call.argument("progressCallback") != JSONObject.NULL ? (long)call.argument("progressCallback") : -1;
        final long successCallbackHandle = call.hasArgument("successCallback") && call.argument("successCallback") != JSONObject.NULL ? (long)call.argument("successCallback") : -1;
        final long errorCallbackHandle = call.hasArgument("errorCallback") && call.argument("errorCallback") != JSONObject.NULL ? (long)call.argument("errorCallback") : -1;

        M3U8Downloader.getInstance().download(url);
        M3U8Downloader.getInstance().setOnM3U8DownloadListener(new OnM3U8DownloadListener() {
          @Override
          public void onDownloadProgress(final M3U8Task task) {
            super.onDownloadProgress(task);
            if (progressCallbackHandle != -1) {
              //下载进度，非UI线程
              final Map<String, Object> args = new HashMap<>();
              args.put("url", task.getUrl());
              args.put("state", task.getState());
              args.put("progress", task.getProgress());
              args.put("speed", task.getSpeed());
              args.put("formatSpeed", task.getFormatSpeed());
              args.put("totalSize", task.getFormatTotalSize());
              handler.post(new Runnable() {
                @Override
                public void run() {
                  flutterM3U8BackgroundExecutor.executeDartCallbackInBackgroundIsolate(progressCallbackHandle, args);
                }
              });
            }
          }

          @Override
          public void onDownloadItem(M3U8Task task, long itemFileSize, int totalTs, int curTs) {
            super.onDownloadItem(task, itemFileSize, totalTs, curTs);
            //下载切片监听，非UI线程
//          channel.invokeMethod();
          }

          @Override
          public void onDownloadSuccess(M3U8Task task) {
            super.onDownloadSuccess(task);
            String saveDir = MUtils.getSaveFileDir(task.getUrl());
            final Map<String, Object> args = new HashMap<>();
            args.put("dir", saveDir);
            args.put("fileName", saveDir + File.separator + "local.m3u8");

            //下载成功
            if (successCallbackHandle != -1) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  flutterM3U8BackgroundExecutor.executeDartCallbackInBackgroundIsolate(successCallbackHandle, args);
                }
              });
            }
          }

          @Override
          public void onDownloadPending(M3U8Task task) {
            super.onDownloadPending(task);
            //加入队列，任务挂起
          }

          @Override
          public void onDownloadPause(M3U8Task task) {
            super.onDownloadPause(task);
            //任务暂停
          }

          @Override
          public void onDownloadPrepare(final M3U8Task task) {
            super.onDownloadPrepare(task);
            //准备下载
          }

          @Override
          public void onDownloadError(final M3U8Task task, Throwable errorMsg) {
            super.onDownloadError(task, errorMsg);

            //下载错误，非UI线程
            if (errorCallbackHandle != -1) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  flutterM3U8BackgroundExecutor.executeDartCallbackInBackgroundIsolate(errorCallbackHandle, null);
                }
              });
            }
          }
        });
        result.success(null);
      } else if (call.method.equals("isRunning")) {
        result.success(M3U8Downloader.getInstance().isRunning());
      } else if (call.method.equals("pause")) {
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String url = call.argument("url");
        M3U8Downloader.getInstance().pause(url);
        result.success(null);
      } else if (call.method.equals("cancel")) {
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String url = call.argument("url");
        boolean isDelete = false;
        if (call.hasArgument("isDelete")) {
          isDelete = call.argument("isDelete");
        }
        if (isDelete) {
          M3U8Downloader.getInstance().cancelAndDelete(url, null);
        } else {
          M3U8Downloader.getInstance().pause(url);
        }
        result.success(null);
      } else {
        result.notImplemented();
      }
    } catch (JSONException e) {
      result.error("error", "JSON error: " + e.getMessage(), null);
    } catch (Exception e) {
      result.error("error", "M3u8Downloader error: " + e.getMessage(), null);
    }
  }
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    context = null;
    channel.setMethodCallHandler(null);
    channel = null;
  }
}
