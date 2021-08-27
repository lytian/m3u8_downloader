package com.vincent.m3u8Downloader.listener;

import com.vincent.m3u8Downloader.bean.M3U8;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 22:37
 * @Desc: 任务下载监听器
 */
public interface OnTaskDownloadListener {

    /**
     * 开始任务
     */
    void onStart();

    /**
     * 开始下载
     * @param totalTs ts总数
     * @param curTs 当前下载完成的ts个数
     */
    void onStartDownload(int totalTs, int curTs);

    /**
     * ts文件下载完成
     * 注意：这个方法是异步的（子线程中执行），所以不能在此方法中回调，其他方法为主线程中回调
     * @param itemFileSize 单个文件的大小
     * @param totalTs      ts总数
     * @param curTs        当前下载完成的ts个数
     */
    void onDownloadItem(long itemFileSize, int totalTs, int curTs);

    /**
     * 定时进度
     * @param curLength 下载大小
     */
    void onProgress(long curLength);

    /**
     * 正在转成MP4格式
     */
    void onConvert();

    /**
     * 下载成功
     */
    void onSuccess(M3U8 m3U8);

    /**
     * 错误的时候回调
     * 线程环境无法保证，不可以直接在UI线程调用
     * @param error 错误信息
     */
    void onError(Throwable error);
}
