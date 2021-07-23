import 'dart:io';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

void callbackDispatcher() {

  // Initialize state necessary for MethodChannels.
  WidgetsFlutterBinding.ensureInitialized();
  const MethodChannel backgroundChannel = MethodChannel('vincent/m3u8_downloader_background', JSONMethodCodec());

  backgroundChannel.setMethodCallHandler((MethodCall call) async {
    final dynamic args = call.arguments;
    final CallbackHandle handle = CallbackHandle.fromRawHandle(args[0]);

    final Function? closure = PluginUtilities.getCallbackFromHandle(handle);

    if (closure == null) {
      print('Fatal: could not find callback');
      exit(-1);
    }

    closure(args[1]);
  });

  backgroundChannel.invokeMethod('didInitializeDispatcher');
}