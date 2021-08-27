import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';

import 'callback_dispatcher.dart';

typedef CallbackHandle? _GetCallbackHandle(Function callback);
typedef SelectNotificationCallback = Future<dynamic> Function();


class M3u8Downloader {
  static const MethodChannel _channel = const MethodChannel('vincent/m3u8_downloader', JSONMethodCodec());
  static _GetCallbackHandle _getCallbackHandle = (Function callback) => PluginUtilities.getCallbackHandle(callback);
  static SelectNotificationCallback? _onSelectNotification;
  static bool _initialized = false;


  ///  初始化下载器
  ///  在使用之前必须调用
  ///
  /// - [onSelect] 点击通知的回调
  static Future<bool> initialize({
    SelectNotificationCallback? onSelect
  }) async {
    assert(!_initialized, 'M3u8Downloader.initialize() must be called only once!');

    final CallbackHandle? handle = _getCallbackHandle(callbackDispatcher);
    if (handle == null) {
      return false;
    }
    if (onSelect != null) {
      _onSelectNotification = onSelect;
    }
    _channel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
        case 'selectNotification':
          if (_onSelectNotification == null) {
            return Future.value(false);
          }
          return _onSelectNotification!();
        default:
          return Future.error('method not defined');
      }
    });

    final bool? r = await _channel.invokeMethod<bool>('initialize',{
      "handle": handle.toRawHandle(),
    });
    _initialized = r ?? false;
    return _initialized;
  }

  ///  下载配置
  ///
  /// - [saveDir] 文件保存位置
  /// - [showNotification] 是否显示通知
  /// - [convertMp4] 是否转成mp4
  /// - [connTimeout] 网络连接超时时间
  /// - [readTimeout] 文件读取超时时间
  /// - [threadCount] 同时下载的线程数
  /// - [debugMode] 调试模式
  static Future<bool> config({
    String? saveDir,
    bool showNotification = true,
    bool convertMp4 = false,
    int? connTimeout,
    int? readTimeout,
    int? threadCount,
    bool? debugMode,
  }) async {
    final bool? r = await _channel.invokeMethod<bool>('config',{
      "saveDir": saveDir,
      "showNotification": showNotification,
      "convertMp4": convertMp4,
      "connTimeout": connTimeout,
      "readTimeout": readTimeout,
      "threadCount": threadCount,
      "debugMode": debugMode,
    });
    return r ?? false;
  }

  /// 下载文件
  /// 
  /// - [url] 下载链接地址
  /// - [name] 下载文件名(通知标题)
  /// - [progressCallback] 下载进度回调
  /// - [successCallback] 下载成功回调
  /// - [errorCallback] 下载失败回调
  static void download({
    required String url,
    required String name,
    Function? progressCallback,
    Function? successCallback,
    Function? errorCallback
  }) async {
    assert(url.isNotEmpty && name.isNotEmpty);
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');

    Map<String, dynamic> params = {
      "url": url,
      "name": name,
    };
    if (progressCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(progressCallback);
      if (handle != null) {
        params["progressCallback"] = handle.toRawHandle();
      }
    }
    if (successCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(successCallback);
      if (handle != null) {
        params["successCallback"] = handle.toRawHandle();
      }
    }
    if (errorCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(errorCallback);
      if (handle != null) {
        params["errorCallback"] = handle.toRawHandle();
      }
    }

    await _channel.invokeMethod("download", params);
  }

  /// 暂停下载
  /// 
  /// - [url] 暂停指定的链接地址
  static void pause(String url) async {
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');
    await _channel.invokeMethod("pause", {
      "url": url
    });
  }

  /// 删除下载
  /// 
  /// - [url] 下载链接地址
  static Future<bool> delete(String url) async {
    assert(url.isNotEmpty);
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');

    return await _channel.invokeMethod("delete", {
      "url": url
    }) ?? false;
  }

  /// 下载状态
  static Future<bool> isRunning() async {
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');
    bool isRunning = await _channel.invokeMethod("isRunning");
    return isRunning;
  }

  /// 通过URL获取保存的路径
  /// - [url] 请求的URL
  /// baseDir - 基础文件保存路径
  /// m3u8 - m3u8文件地址
  /// mp4 - mp4存储位置
  static Future<dynamic> getSavePath(String url) async {
    return await _channel.invokeMethod("getSavePath", { "url": url });
  }
}
