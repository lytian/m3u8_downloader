# m3u8_downloader

Flutter m3u8下载器。后台任务下载，支持加密下载。

只实现了Android端，并且只支持单m3u8视频下载(m3u8文件包含了多个ts文件，本质是多个ts同时下载)。

## 安装

pubspec.yaml
```yaml
dependencies:
  m3u8_downloader:
    git:
      url: https://github.com/lytian/m3u8_downloader.git
```

## Android权限配置

```xml
  <!--网络权限-->
  <uses-permission android:name="android.permission.INTERNET" />

  <!--存储-->
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 使用

1. 页面初始化时，初始化下载器
```dart

WidgetsFlutterBinding.ensureInitialized();
M3u8Downloader.initialize();


/// initialize方法参数说明：
/// - [saveDir] 文件保存位置
/// - [showNotification] 是否显示通知
/// - [connTimeout] 网络连接超时时间
/// - [readTimeout] 文件读取超时时间
/// - [threadCount] 同时下载的线程数
/// - [debugMode] 调试模式
/// - [onSelect] 点击通知的回调
```



2. 必须初始化完成后才能使用M3u8Downloader的其他方法
```dart

// 下载方法
M3u8Downloader.download(url, name, callback);

// 暂停下载
M3u8Downloader.pause(url);

// 取消下载
M3u8Downloader.cancel(url, true);

// 获取下载状态
M3u8Downloader.isRunning();

// 通过url获取保存的路径
M3u8Downloader.getM3U8Path();

/// download参数说明：
/// - [url] 下载链接地址
/// - [name] 下载文件名。(通知标题)
/// - [progressCallback] 下载进度回调
/// - [successCallback] 下载成功回调
/// - [errorCallback] 下载失败回调
```

3. 下载器本身是后台进程下载，看情况暂停和取消下载

4. download函数中的回调函数，必须是顶层的静态函数

5. 不支持m3u8多任务下载。因为每一个m3u8都包含了多个ts片段，本身是多任务下载ts片段

## 示例代码

具体代码请看[Example/lib/main.dart](https://github.com/lytian/m3u8_downloader/blob/master/example/lib/main.dart)
