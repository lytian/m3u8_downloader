import 'dart:io';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:m3u8_downloader/m3u8_downloader.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  ReceivePort _port = ReceivePort();
  
  @override
  void initState() {
    super.initState();
    initAsync();
  }

  void initAsync() async {
    WidgetsFlutterBinding.ensureInitialized();

    String saveDir = await _findSavePath();
    M3u8Downloader.initialize(
        saveDir: saveDir,
        debugMode: false,
        onSelect: () {
          print('下载成功点击');
          return null;
        }
    );
    // 注册监听器
    IsolateNameServer.registerPortWithName(_port.sendPort, 'downloader_send_port');
    _port.listen((dynamic data) {
      // 监听数据请求
      print(data);
    });
  }

  Future<bool> _checkPermission() async {
    if (Platform.isAndroid) {
      PermissionStatus permission = await PermissionHandler().checkPermissionStatus(PermissionGroup.storage);
      if (permission != PermissionStatus.granted) {
        Map<PermissionGroup, PermissionStatus> permissions = await PermissionHandler().requestPermissions([PermissionGroup.storage]);
        if (permissions[PermissionGroup.storage] == PermissionStatus.granted) {
          return true;
        }
      } else {
        return true;
      }
    } else {
      return true;
    }
    return false;
  }

  Future<String> _findSavePath() async {
    final directory = Platform.isAndroid
        ? await getExternalStorageDirectory()
        : await getApplicationDocumentsDirectory();
    String saveDir = directory.path + '/vPlayDownload';
    Directory root = Directory(saveDir);
    if (!root.existsSync()) {
      await root.create();
    }
    print(saveDir);
    return saveDir;
  }

  static progressCallback(dynamic args) {
    final SendPort send = IsolateNameServer.lookupPortByName('downloader_send_port');
    args["status"] = 1;
	  send.send(args);
  }
  static successCallback(dynamic args) {
    final SendPort send = IsolateNameServer.lookupPortByName('downloader_send_port');
	  send.send({"status": 2, "url": args["url"]});
  }
  static errorCallback(dynamic args) {
    final SendPort send = IsolateNameServer.lookupPortByName('downloader_send_port');
	  send.send({"status": 3, "url": args["url"]});
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: <Widget>[
            RaisedButton(
              child: Text("下载未加密m3u8"),
              onPressed: () {
              _checkPermission().then((hasGranted) async {
                if (hasGranted) {
                  M3u8Downloader.download(
                      url: "https://iqiyi.cdn9-okzy.com/20200711/12300_030c6e1d/index.m3u8",
                      name: "下载未加密m3u8",
                      progressCallback: progressCallback,
                      successCallback: successCallback,
                      errorCallback: errorCallback
                  );
                }
              });
            }),
            RaisedButton(
              child: Text("下载已加密m3u8"),
              onPressed: () {
                _checkPermission().then((hasGranted) async {
                  if (hasGranted) {
                    M3u8Downloader.download(
                      url: "http://video.huishenghuo888888.com:8091/jingpin/20200801/D4Jm7KDD/index.m3u8",
                      name: "下载已加密m3u8",
                      progressCallback: progressCallback,
                      successCallback: successCallback,
                      errorCallback: errorCallback
                    );
                  }
                });
              },
            )
          ],
        ),
      ),
    );
  }
}
