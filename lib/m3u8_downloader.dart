import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/services.dart';

import 'callback_dispatcher.dart';

typedef CallbackHandle _GetCallbackHandle(Function callback);

class M3u8Downloader {
  static const MethodChannel _channel = const MethodChannel('vincent/m3u8_downloader', JSONMethodCodec());
  static _GetCallbackHandle _getCallbackHandle = (Function callback) => PluginUtilities.getCallbackHandle(callback);

  ///  初始化下载器
  /// 
  ///  在使用之前必须调用
  static Future<bool> initialize() async {
    final CallbackHandle handle = _getCallbackHandle(callbackDispatcher);
    if (handle == null) {
      return false;
    }
    final bool r = await _channel.invokeMethod<bool>('initialize', <dynamic>[handle.toRawHandle()]);
    return r ?? false;
  }

  /// 下载文件
  /// 
  /// - [url] 下载链接地址
  /// - [callback] 回调函数
  static void download({String url, Function progressCallback, Function successCallback, Function errorCallback}) async {
    assert(url != null && url != "");
    Map<String, dynamic> params = {
      "url": url
    };
    if (progressCallback != null) {
      final CallbackHandle handle = _getCallbackHandle(progressCallback);
      if (handle != null) {
        params["progressCallback"] = handle.toRawHandle();
      }
    }
    if (successCallback != null) {
      final CallbackHandle handle = _getCallbackHandle(successCallback);
      if (handle != null) {
        params["successCallback"] = handle.toRawHandle();
      }
    }
    if (errorCallback != null) {
      final CallbackHandle handle = _getCallbackHandle(errorCallback);
      if (handle != null) {
        params["errorCallback"] = handle.toRawHandle();
      }
    }

    await _channel.invokeMethod("download", params);
  }

  /// 配置方法
  ///
  /// - [saveDir] 文件保存位置
  /// - [connTimeout] 网络连接超时时间
  /// - [readTimeout] 文件读取超时时间
  /// - [debugMode] 调试模式
  static void config({ String saveDir, int connTimeout, int readTimeout, bool debugMode}) async {
    assert(Directory(saveDir).existsSync());

    await _channel.invokeMethod("config", {
      "saveDir": saveDir,
      "connTimeout": connTimeout,
      "readTimeout": readTimeout,
      "debugMode": debugMode
    });
  }

  /// 暂停下载
  /// 
  /// - [url] 暂停指定的链接地址
  static void pause(String url) async {
    assert(url != null && url != "");

    await _channel.invokeMethod("pause", { "url": url });
  }

  /// 取消下载
  /// 
  /// - [url] 下载链接地址
  /// - [isDelete] 取消时是否删除文件
  static void cancel(String url, { bool isDelete }) async {
    assert(url != null && url != "");

    await _channel.invokeMethod("cancel", { "url": url, "isDelete": isDelete });
  }

  /// 下载状态
  static Future<bool> isRunning() async {
    bool isRunning = await _channel.invokeMethod("isRunning");
    return isRunning;
  }

  /// 通过url获取保存的路径
  static Future<String> getM3U8Path(String url) async {
    String path = await _channel.invokeMethod("getM3U8Path", { "url": url });
    return path;
  }
}
