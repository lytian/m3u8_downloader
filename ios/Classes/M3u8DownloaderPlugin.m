#import "M3U8DownloaderPlugin.h"
#if __has_include(<m3u8_downloader/m3u8_downloader-Swift.h>)
#import <m3u8_downloader/m3u8_downloader-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "m3u8_downloader-Swift.h"
#endif

@implementation M3U8DownloaderPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftM3u8DownloaderPlugin registerWithRegistrar:registrar];
}
@end
