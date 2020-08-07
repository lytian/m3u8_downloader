package vincent.m3u8_downloader;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
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
import io.flutter.plugin.common.PluginRegistry.Registrar;
import vincent.m3u8_downloader.bean.M3U8Task;
import vincent.m3u8_downloader.utils.MD5Utils;
import vincent.m3u8_downloader.utils.MUtils;

/** FlutterM3U8DownloaderPlugin */
public class M3u8DownloaderPlugin implements FlutterPlugin, PluginRegistry.NewIntentListener, MethodCallHandler, ActivityAware {
  private static final  String TAG = "M3u8Downloader";
  private static final String CHANNEL_NAME = "vincent/m3u8_downloader";
  public static final String SHARED_PREFERENCES_KEY = "vincent.m3u8.downloader.pref";
  public static final String CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatcher_handle_key";
  private static final String CHANNEL_ID = "M3U8_DOWNLOADER_NOTIFICATION";
  private static final int NOTIFICATION_ID = 9527;
  private static final String SELECT_NOTIFICATION = "SELECT_NOTIFICATION";


  private static M3u8DownloaderPlugin instance;
  private MethodChannel channel;
  private Context context;
  private Handler handler;
  private Object initializationLock = new Object();
  FlutterM3U8BackgroundExecutor flutterM3U8BackgroundExecutor = new FlutterM3U8BackgroundExecutor();

  /**
   * 通知栏
   */
  private boolean showNotification;
  private NotificationCompat.Builder builder;
  private NotificationManager notificationManager;
  private String fileName;
  private int notificationProgress = -100;
  private boolean isNotificationError = false;
  private Activity mainActivity;

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
        long callbackHandle = call.argument("handle");

        M3U8DownloaderConfig config = M3U8DownloaderConfig.build(context);
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
        if (call.hasArgument("isConvert") && call.argument("isConvert") != JSONObject.NULL) {
          boolean isConvert = call.argument("isConvert");
          config.setIsConvert(isConvert);
        }

        flutterM3U8BackgroundExecutor.setCallbackDispatcher(context, callbackHandle);
        flutterM3U8BackgroundExecutor.startBackgroundIsolate(context);

        if (showNotification) {
          buildNotification();
        }
        result.success(true);
      } else if (call.method.equals("download")) {
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        boolean showNotification = M3U8DownloaderConfig.isShowNotification();
        String name = "";
        if (showNotification) {
          if (!call.hasArgument("name")) {
            result.error("1", "name必传", "");
            return;
          }
          name = call.argument("name");
          this.fileName = name;
        }
        String url = call.argument("url");

        final long progressCallbackHandle = call.hasArgument("progressCallback") && call.argument("progressCallback") != JSONObject.NULL ? (long)call.argument("progressCallback") : -1;
        final long successCallbackHandle = call.hasArgument("successCallback") && call.argument("successCallback") != JSONObject.NULL ? (long)call.argument("successCallback") : -1;
        final long errorCallbackHandle = call.hasArgument("errorCallback") && call.argument("errorCallback") != JSONObject.NULL ? (long)call.argument("errorCallback") : -1;

        M3U8Downloader.getInstance().download(url, name);
        updateNotification(0, 0);
        M3U8Downloader.getInstance().setOnM3U8DownloadListener(new OnM3U8DownloadListener() {
          @Override
          public void onDownloadProgress(final M3U8Task task) {
            super.onDownloadProgress(task);

            updateNotification(1, (int)(task.getProgress() * 100));
            if (progressCallbackHandle != -1) {
              //下载进度，非UI线程
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
          public void onConverting() {
            super.onConverting();
            updateNotification(5, 100);
          }

          @Override
          public void onDownloadSuccess(M3U8Task task) {
            super.onDownloadSuccess(task);
            updateNotification(2, 100);
            String saveDir = MUtils.getSaveFileDir(task.getUrl());
            final Map<String, Object> args = new HashMap<>();
            args.put("url", task.getUrl());
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

            updateNotification(3, 0);
            //下载错误，非UI线程
            if (errorCallbackHandle != -1) {
              final Map<String, Object> args = new HashMap<>();
              args.put("url", task.getUrl());
              handler.post(new Runnable() {
                @Override
                public void run() {
                  flutterM3U8BackgroundExecutor.executeDartCallbackInBackgroundIsolate(errorCallbackHandle, args);
                }
              });
            }
          }
        });
        result.success(null);
      } else if (call.method.equals("pause")) {
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String url = call.argument("url");
        M3U8Downloader.getInstance().pause(url);
        updateNotification(4, 0);
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
        if (notificationManager != null) {
          notificationManager.cancel(NOTIFICATION_ID);
        }
        result.success(null);
      } else if (call.method.equals("isRunning")) {
        result.success(M3U8Downloader.getInstance().isRunning());
      }  else if (call.method.equals("getM3U8Path")) {
        if (!call.hasArgument("url")) {
          result.error("1", "url必传", "");
          return;
        }
        String url = call.argument("url");
        String baseDir = MUtils.getSaveFileDir(url);
        Map<String, String> res = new HashMap<>();
        res.put("baseDir", baseDir);
        res.put("m3u8", baseDir + File.separator + "local.m3u8");
        res.put("mp4", M3U8DownloaderConfig.getSaveDir() + File.separator + MD5Utils.encode(url) + ".mp4");
        result.success(res);
      }  else {
        result.notImplemented();
      }
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

  /**
   *  初始化通知
   */
  private void buildNotification() {
    // Make a channel if necessary
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Create the NotificationChannel, but only on API 26+ because
      // the NotificationChannel class is new and not in the support library

      CharSequence name = context.getApplicationInfo().loadLabel(context.getPackageManager());
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      channel.setSound(null, null);

      // Add the channel
      notificationManager = context.getSystemService(NotificationManager.class);

      if (notificationManager != null) {
        notificationManager.createNotificationChannel(channel);
      }
    }

    if (mainActivity != null) {
      sendNotificationPayloadMessage(mainActivity.getIntent());
    }

    // Create the notification
    builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_download)  // 通知图标
            .setOnlyAlertOnce(true) //
            .setAutoCancel(true) // 默认不自动取消
            .setPriority(NotificationCompat.PRIORITY_DEFAULT); // 默认优先级

  }

  /**
   * 更新通知
   * @param status 下载状态    0-准备下载   1-正在下载   2-下载成功   3-下载失败   4-已暂停   5-正在转成MP4
   * @param progress 下载进度
   */
  private void updateNotification(int status, int progress) {
    if (!showNotification) return;
    builder.setContentTitle(fileName == null || fileName.equals("") ? "下载M3U8文件" : fileName);

    if (status == 0) {
      isNotificationError = false;
      notificationProgress = -100;
      builder.setContentText("等待下载...").setProgress(0, 0, true);
      builder.setOngoing(true)
              .setSmallIcon(android.R.drawable.stat_sys_download_done);
    } else if (status == 1) {
      if (isNotificationError) return;
      // 控制刷新Notification频率
      if (progress < 100 && (progress - notificationProgress < 2)) {
        return;
      }
      notificationProgress = progress;
      builder.setContentText("正在下载...")
              .setProgress(100, progress, false);
      builder.setOngoing(true)
              .setSmallIcon(android.R.drawable.stat_sys_download);
    } else if (status == 2) {
      // 添加点击回调
      Intent intent = new Intent(context, getMainActivityClass(context));
      intent.setAction(SELECT_NOTIFICATION);
      PendingIntent pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      builder.setContentIntent(pendingIntent);

      builder.setContentText("下载完成").setProgress(0, 0, false);
      builder.setOngoing(false)
              .setSmallIcon(android.R.drawable.stat_sys_download_done);
    } else if (status == 3) {
      isNotificationError = true;
      builder.setContentText("下载失败").setProgress(0, 0, false);
      builder.setOngoing(false)
              .setSmallIcon(android.R.drawable.stat_sys_download_done);
    } else if (status == 4) {
      builder.setContentText("暂停下载").setProgress(0, 0, false);
      builder.setOngoing(false)
              .setSmallIcon(android.R.drawable.stat_sys_download);
    } else if (status == 5) {
      builder.setContentText("正在转成MP4").setProgress(100, 100, true);
      builder.setOngoing(true)
              .setSmallIcon(android.R.drawable.stat_sys_download);
    }

    // Show the notification
    if (showNotification) {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
  }

  private Class getMainActivityClass(Context context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    String className = launchIntent.getComponent().getClassName();
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  private Boolean sendNotificationPayloadMessage(Intent intent) {
    if (SELECT_NOTIFICATION.equals(intent.getAction())) {
      channel.invokeMethod("selectNotification", null);
      return true;
    }
    return false;
  }

  @Override
  public boolean onNewIntent(Intent intent) {
    boolean res = sendNotificationPayloadMessage(intent);
    if (res && mainActivity != null) {
      mainActivity.setIntent(intent);
    }
    return res;
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
    binding.addOnNewIntentListener(this);
    mainActivity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    this.mainActivity = null;
  }
}
