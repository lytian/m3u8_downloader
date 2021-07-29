import 'dart:io';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:m3u8_downloader/m3u8_downloader.dart';
import 'package:open_file/open_file.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  ReceivePort _port = ReceivePort();

  // 未加密的url地址
  String url1 = "https://videomy.yongaomy.com/20210721/xxxxxxx/index.m3u8";
  // 加密的url地址
  String url2 = "https://video.huishenghuo888888.com/douyin/20210722/xxxxxxx/index.m3u8";

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
        onSelect: () async {
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
    var status = await Permission.storage.status;
    if (!status.isGranted) {
      status = await Permission.storage.request();
    }
    return status.isGranted;
  }

  Future<String> _findSavePath() async {
    final directory = Platform.isAndroid
        ? await getExternalStorageDirectory()
        : await getApplicationDocumentsDirectory();
    String saveDir = directory!.path + '/vPlayDownload';
    Directory root = Directory(saveDir);
    if (!root.existsSync()) {
      await root.create();
    }
    print(saveDir);
    return saveDir;
  }

  static progressCallback(dynamic args) {
    final SendPort? send = IsolateNameServer.lookupPortByName('downloader_send_port');
    if (send != null) {
      args["status"] = 1;
      send.send(args);
    }
  }
  static successCallback(dynamic args) {
    final SendPort? send = IsolateNameServer.lookupPortByName('downloader_send_port');
    if (send != null) {
      send.send({
        "status": 2,
        "url": args["url"],
        "filePath": args["filePath"],
        "dir": args["dir"]
      });
    }
  }
  static errorCallback(dynamic args) {
    final SendPort? send = IsolateNameServer.lookupPortByName('downloader_send_port');
    if (send != null) {
      send.send({"status": 3, "url": args["url"]});
    }
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
            ElevatedButton(
              child: Text("下载未加密m3u8"),
              onPressed: () {
              _checkPermission().then((hasGranted) async {
                if (hasGranted) {
                  M3u8Downloader.download(
                      url: url1,
                      name: "下载未加密m3u8",
                      progressCallback: progressCallback,
                      successCallback: successCallback,
                      errorCallback: errorCallback
                  );
                }
              });
            }),
            ElevatedButton(
              child: Text("下载已加密m3u8"),
              onPressed: () {
                _checkPermission().then((hasGranted) async {
                  if (hasGranted) {
                    M3u8Downloader.download(
                        url: url2,
                        name: "下载已加密m3u8",
                        progressCallback: progressCallback,
                        successCallback: successCallback,
                        errorCallback: errorCallback
                    );
                  }
                });
              },
            ),
            ElevatedButton(
              child: Text("打开已下载的未加密的文件"),
              onPressed: () async {
                  var res = await M3u8Downloader.getSavePath(url1);
                  print(res);
                  File mp4 = File(res['mp4']);
                  if (mp4.existsSync()) {
                    OpenFile.open(res['mp4']);
                  }
              },
            ),
            ElevatedButton(
              child: Text("打开已下载的已加密的文件"),
              onPressed: () async {
                var res = await M3u8Downloader.getSavePath(url2);
                print(res);
                File mp4 = File(res['mp4']);
                if (mp4.existsSync()) {
                  OpenFile.open(res['mp4']);
                }
              },
            ),
          ],
        ),
      ),
    );
  }
}
