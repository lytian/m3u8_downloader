import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';

 import 'package:m3u8_downloader/m3u8_downloader.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await M3u8Downloader.initialize();

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {


  @override
  void initState() {
    super.initState();
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

  static void downloadCallback(dynamic args) {
    print(args);
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

                // M3u8Downloader.download("https://videomy.yongaomy.com/20191231/8P5tUoGm/index.m3u8");
                _checkPermission().then((hasGranted) async {
                  if (hasGranted) {
                    String saveDir = await _findSavePath();
                    print("================" + saveDir);
                    M3u8Downloader.config(debugMode: true, saveDir: saveDir);
                  }
                });
              },
            ),
            RaisedButton(
              child: Text("下载未加密m3u8"),
              onPressed: () {
                M3u8Downloader.download(url: "https://videomy.yongaomy.com/20191231/8P5tUoGm/index.m3u8", progressCallback: downloadCallback);
              },
            ),
            RaisedButton(
              child: Text("下载已加密m3u8"),
              onPressed: () {
                M3u8Downloader.download(url: "https://video.huishenghuo888888.com/putong/20200108/3VhB5pDe/index.m3u8", progressCallback: downloadCallback);
              },
            )
          ],
        ),
      ),
    );
  }
}
