package com.vincent.m3u8Downloader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vincent.m3u8Downloader.bean.M3U8Task;
import com.vincent.m3u8Downloader.bean.M3U8TaskState;
import com.vincent.m3u8Downloader.downloader.M3U8DownloadConfig;
import com.vincent.m3u8Downloader.downloader.M3U8DownloadTask;
import com.vincent.m3u8Downloader.downloader.M3U8Downloader;
import com.vincent.m3u8Downloader.listener.OnM3U8DownloadListener;
import com.vincent.m3u8Downloader.utils.M3U8Log;
import com.vincent.m3u8Downloader.utils.M3U8Util;
import com.vincent.m3u8Downloader.utils.NotificationUtil;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/** M3U8DownloaderPlugin */
public class M3U8DownloaderPlugin implements FlutterPlugin, MethodCallHandler, PluginRegistry.NewIntentListener, ActivityAware {
  private static final String CHANNEL_NAME = "vincent/m3u8_downloader";

  private MethodChannel channel;
  private Context context;
  private Activity mainActivity;
  private Handler handler;
  private final Object initializationLock = new Object();
  private boolean showNotification = true;
  private final FlutterBackgroundExecutor backgroundExecutor = new FlutterBackgroundExecutor();

  private String fileName = "";
  private long progressCallbackHandle = -1;
  private long successCallbackHandle = -1;
  private long errorCallbackHandle = -1;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
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
  public void onMethodCall(@NonNull MethodCall call, @NonNull final Result result) {
    String method = call.method;
    switch (method) {
      case "initialize":
        long callbackHandle = call.argument("handle");
        FlutterBackgroundExecutor.setCallbackDispatcher(context, callbackHandle);
        backgroundExecutor.startBackgroundIsolate(context);

        result.success(true);
        break;
      case "config":
        M3U8DownloadConfig config = M3U8DownloadConfig.build(context);
        if (call.hasArgument("saveDir") && call.argument("saveDir") != JSONObject.NULL) {
          String saveDir = call.argument("saveDir");
          config.setSaveDir(saveDir);
        }
        if (call.hasArgument("showNotification") && call.argument("showNotification") != JSONObject.NULL) {
          boolean show = call.argument("showNotification");
          showNotification = show;
          config.setShowNotification(show);
        }
        if (call.hasArgument("connTimeout") && call.argument("connTimeout") != JSONObject.NULL) {
          int connTimeout = call.argument("connTimeout");
          config.setConnTimeout(connTimeout);
        }
        if (call.hasArgument("readTimeout") && call.argument("readTimeout") != JSONObject.NULL) {
          int readTimeout = call.argument("readTimeout");
          config.setReadTimeout(readTimeout);
        }
        if (call.hasArgument("threadCount") && call.argument("threadCount") != JSONObject.NULL) {
          int threadCount = call.argument("threadCount");
          config.setThreadCount(threadCount);
        }
        if (call.hasArgument("debugMode") && call.argument("debugMode") != JSONObject.NULL) {
          boolean debugMode = call.argument("debugMode");
          config.setDebugMode(debugMode);
        }
        if (call.hasArgument("convertMp4") && call.argument("convertMp4") != JSONObject.NULL) {
          boolean convertMp4 = call.argument("convertMp4");
          config.setConvertMp4(convertMp4);
        }
        result.success(true);
        break;
      case "download":
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        if (!call.hasArgument("name")) {
          result.error("1", "name必传", "");
          return;
        }
        showNotification = M3U8DownloadConfig.isShowNotification();
        String url = call.argument("url");
        fileName = call.argument("name");
        NotificationUtil.getInstance().cancel();
        if (showNotification) {
          NotificationUtil.getInstance().build(context);
        }
        progressCallbackHandle = call.hasArgument("progressCallback") && call.argument("progressCallback") != JSONObject.NULL ? (long)call.argument("progressCallback") : -1;
        successCallbackHandle = call.hasArgument("successCallback") && call.argument("successCallback") != JSONObject.NULL ? (long)call.argument("successCallback") : -1;
        errorCallbackHandle = call.hasArgument("errorCallback") && call.argument("errorCallback") != JSONObject.NULL ? (long)call.argument("errorCallback") : -1;

        M3U8Downloader.getInstance().download(url);
        M3U8Downloader.getInstance().setOnM3U8DownloadListener(mDownloadListener);
        result.success(null);
        break;
      case "pause":
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String pauseUrl = call.argument("url");

        M3U8Downloader.getInstance().pause(pauseUrl);
        result.success(null);
        break;
      case "delete":
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        final String deleteUrl = call.argument("url");
        handler.post(new Runnable() {
          @Override
          public void run() {
            boolean flag = M3U8Downloader.getInstance().delete(deleteUrl);
            result.success(flag);
          }
        });
        break;
      case "isRunning":
        result.success(M3U8Downloader.getInstance().isRunning());
        break;
      case "getSavePath":
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String saveUrl = call.argument("url");
        Map<String, String> res = new HashMap<>();
        res.put("baseDir", M3U8Util.getSaveFileDir(saveUrl));
        res.put("m3u8", M3U8DownloadTask.getM3U8Path(saveUrl));
        res.put("mp4", M3U8DownloadTask.getMp4Path(saveUrl));
        result.success(res);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  final OnM3U8DownloadListener mDownloadListener = new OnM3U8DownloadListener() {
    @Override
    public void onDownloadPrepare(M3U8Task task) {
      if (showNotification) {
        NotificationUtil.getInstance().updateNotification(fileName, M3U8TaskState.PREPARE, 0);
      }
    }

    @Override
    public void onDownloadPending(M3U8Task task) {
      if (showNotification) {
        NotificationUtil.getInstance().updateNotification(fileName, M3U8TaskState.PENDING, Math.round(task.getProgress() * 100));
      }
    }

    @Override
    public void onDownloadProgress(M3U8Task task) {
      if (showNotification) {
        NotificationUtil.getInstance().updateNotification(fileName, M3U8TaskState.DOWNLOADING, Math.round(task.getProgress() * 100));
      }
      if (progressCallbackHandle != -1) {
        final Map<String, Object> args = new HashMap<>();
        args.put("url", task.getUrl());
        args.put("state", task.getState());
        args.put("progress", task.getProgress());
        args.put("speed", task.getSpeed());
        args.put("formatSpeed", task.getFormatSpeed());
        args.put("totalSize", task.getTotalSize());
        args.put("currentFormatSize", task.getFormatCurrentSize());
        args.put("totalFormatSize", task.getFormatTotalSize());
        handler.post(new Runnable() {
          @Override
          public void run() {
            backgroundExecutor.executeDartCallbackInBackgroundIsolate(progressCallbackHandle, args);
          }
        });
      }
    }

    @Override
    public void onDownloadItem(M3U8Task task, long itemFileSize, int totalTs, int curTs) {
    }

    @Override
    public void onDownloadSuccess(M3U8Task task) {
      if (showNotification) {
        NotificationUtil.getInstance().updateNotification(fileName, M3U8TaskState.SUCCESS, 100);
      }
      String saveDir = M3U8Util.getSaveFileDir(task.getUrl());
      String filePath;
      if (task.getM3U8() != null) {
        filePath = task.getM3U8().getLocalPath();
      } else {
        File mp4File = new File(M3U8DownloadTask.getMp4Path(task.getUrl()));
        if (mp4File.exists()) {
          filePath = mp4File.getPath();
        } else {
          filePath = M3U8DownloadTask.getMp4Path(task.getUrl());
        }
      }
      final Map<String, Object> args = new HashMap<>();
      args.put("url", task.getUrl());
      args.put("dir", saveDir);
      args.put("filePath", filePath);

      //下载成功
      if (successCallbackHandle != -1) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            backgroundExecutor.executeDartCallbackInBackgroundIsolate(successCallbackHandle, args);
          }
        });
      }
    }

    @Override
    public void onDownloadPause(M3U8Task task) {
      if (showNotification) {
        NotificationUtil.getInstance().updateNotification(fileName, M3U8TaskState.PAUSE, Math.round(task.getProgress() * 100));
      }
    }

    @Override
    public void onConvert() {
      if (showNotification) {
        NotificationCompat.Builder builder = NotificationUtil.getInstance().getBuilder();
        if (builder == null) return;

        builder.setContentText("正在转成MP4")
                .setProgress(100, 100, true)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_download);
        NotificationManagerCompat.from(context).notify(NotificationUtil.NOTIFICATION_ID, builder.build());
      }
    }

    @Override
    public void onDownloadError(M3U8Task task, Throwable error) {
      if (showNotification) {
        NotificationUtil.getInstance().updateNotification(fileName, task.getState(), Math.round(task.getProgress() * 100));
      }
      if (errorCallbackHandle != -1) {
        final Map<String, Object> args = new HashMap<>();
        args.put("url", task.getUrl());
        handler.post(new Runnable() {
          @Override
          public void run() {
            backgroundExecutor.executeDartCallbackInBackgroundIsolate(errorCallbackHandle, args);
          }
        });
      }
    }

    @Override
    public void onStop(M3U8Task task) {
      if (showNotification) {
        NotificationUtil.getInstance().cancel();
      }
    }
  };

  @Override
  public boolean onNewIntent(Intent intent) {
    if (NotificationUtil.ACTION_SELECT_NOTIFICATION.equals(intent.getAction())) {
      M3U8Log.d("selectNotification");
      channel.invokeMethod("selectNotification", null);
      if (mainActivity != null) {
        mainActivity.setIntent(intent);
      }
      return true;
    }
    return false;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    binding.addOnNewIntentListener(this);
    mainActivity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.mainActivity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    this.mainActivity = null;
  }
}
