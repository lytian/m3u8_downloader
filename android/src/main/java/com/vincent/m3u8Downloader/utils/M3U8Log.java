package com.vincent.m3u8Downloader.utils;

import android.util.Log;

import com.vincent.m3u8Downloader.downloader.M3U8DownloadConfig;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 17:58
 * @Desc: M3U8日志系统
 */
public class M3U8Log {
    private static final boolean isDebugMode = M3U8DownloadConfig.isDebugMode();
    private static final String TAG = "M3U8Log";
    private static final String PREFIX = "====== ";

    public static void d(String msg){
        if (isDebugMode) Log.d(TAG, PREFIX + msg);
    }

    public static void e(String msg){
        if (isDebugMode) Log.e(TAG, PREFIX + msg);
    }
}
