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
    
    WidgetsFlutterBinding.ensureInitialized();
    M3u8Downloader.initialize();

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
              child: Text("初始化配置"),
              onPressed: () async {
                _checkPermission().then((hasGranted) async {
                  if (hasGranted) {
                    String saveDir = await _findSavePath();
                    M3u8Downloader.config(debugMode: true, saveDir: saveDir);
                  }
                });
              },
            ),
            RaisedButton(
              child: Text("下载未加密m3u8"),
              onPressed: () {
                M3u8Downloader.download(
                  url: "https://videozmcdn.stz8.com:8091/20200229/HNDS-067/index.m3u8",
                  progressCallback: progressCallback,
                  successCallback: successCallback,
                  errorCallback: errorCallback
                );
              },
            ),
            RaisedButton(
              child: Text("下载已加密m3u8"),
              onPressed: () {
                M3u8Downloader.download(
                  url: "http://pl-ali.youku.com/playlist/m3u8?ts=1524205957&keyframe=0&m3u8Md5=a85842b9ca4e77db4aa57c314c8e61c7&t1=200&pid=1133275aa6ac0891&vid=XMzU1MDY0NjEyMA==&type=flv&oip=1779113856&sid=0524205957937209643a0&token=2124&did=ae8263a35f7eaca76f68bb61436e6dac&ev=1&ctype=20&ep=YlUi3d%2BWQ%2F5shnijRhmbvlc%2FYJ8QmCsaCWAJ1RRpNbA%3D&ymovie=1",
                  progressCallback: progressCallback,
                  successCallback: successCallback,
                  errorCallback: errorCallback
                );
              },
            )
          ],
        ),
      ),
    );
  }
}
