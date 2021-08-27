package com.vincent.m3u8Downloader.downloader;

import android.content.Context;
import android.os.Environment;

import com.vincent.m3u8Downloader.utils.SpHelper;

import java.io.File;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 21:37
 * @Desc: 配置类
 */
public class M3U8DownloadConfig {
    private static final String TAG_SAVE_DIR = "TAG_SAVE_DIR_M3U8";
    private static final String TAG_THREAD_COUNT = "TAG_THREAD_COUNT_M3U8";
    private static final String TAG_CONN_TIMEOUT = "TAG_CONN_TIMEOUT_M3U8";
    private static final String TAG_READ_TIMEOUT = "TAG_READ_TIMEOUT_M3U8";
    private static final String TAG_DEBUG = "TAG_DEBUG_M3U8";
    private static final String TAG_SHOW_NOTIFICATION = "TAG_SHOW_NOTIFICATION_M3U8";
    private static final String TAG_CONVERT_MP4 = "TAG_CONVERT_MP4";

    public static M3U8DownloadConfig build(Context context){
        SpHelper.init(context);
        return new M3U8DownloadConfig();
    }

    public M3U8DownloadConfig setSaveDir(String saveDir){
        SpHelper.putString(TAG_SAVE_DIR, saveDir);
        return this;
    }

    @SuppressWarnings("deprecation")
    public static String getSaveDir(){
        return SpHelper.getString(TAG_SAVE_DIR, Environment.getExternalStorageDirectory().getPath() + File.separator + "M3u8Downloader");
    }

    public M3U8DownloadConfig setThreadCount(int threadCount){
        if (threadCount > 5) threadCount = 5;
        if (threadCount <= 0) threadCount = 1;
        SpHelper.putInt(TAG_THREAD_COUNT, threadCount);
        return this;
    }

    public static int getThreadCount(){
        return SpHelper.getInt(TAG_THREAD_COUNT, 3);
    }

    public M3U8DownloadConfig setConnTimeout(int connTimeout){
        SpHelper.putInt(TAG_CONN_TIMEOUT, connTimeout);
        return this;
    }

    public static int getConnTimeout(){
        return SpHelper.getInt(TAG_CONN_TIMEOUT, 10 * 1000);
    }

    public M3U8DownloadConfig setReadTimeout(int readTimeout){
        SpHelper.putInt(TAG_READ_TIMEOUT, readTimeout);
        return this;
    }

    public static int getReadTimeout(){
        return SpHelper.getInt(TAG_READ_TIMEOUT, 30 * 60 * 1000);
    }


    public M3U8DownloadConfig setDebugMode(boolean debug){
        SpHelper.putBoolean(TAG_DEBUG, debug);
        return this;
    }

    public static boolean isDebugMode(){
        return SpHelper.getBoolean(TAG_DEBUG, false);
    }

    public M3U8DownloadConfig setShowNotification(boolean show){
        SpHelper.putBoolean(TAG_SHOW_NOTIFICATION, show);
        return this;
    }

    public static boolean isShowNotification(){
        return SpHelper.getBoolean(TAG_SHOW_NOTIFICATION, true);
    }

    public M3U8DownloadConfig setConvertMp4(boolean convertMp4){
        SpHelper.putBoolean(TAG_CONVERT_MP4, convertMp4);
        return this;
    }

    public static boolean isConvertMp4(){
        return SpHelper.getBoolean(TAG_CONVERT_MP4, false);
    }
}
