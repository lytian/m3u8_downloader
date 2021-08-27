package com.vincent.m3u8Downloader.listener;

import com.vincent.m3u8Downloader.bean.M3U8Task;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 22:40
 * @Desc: M3U8Downloader 监听器
 */
public interface OnM3U8DownloadListener {

    /**
     * 下载准备
     * @param task 当前准备任务
     */
    void onDownloadPrepare(M3U8Task task);

    /**
     * 等待下载
     * @param task 等待的任务
     */
    void onDownloadPending(M3U8Task task);

    /**
     * 下载进度
     * 异步回调，不可以直接在UI线程调用
     * @param task 当前下载任务
     */
    void onDownloadProgress(M3U8Task task);

    /**
     * 完成一次下载任务
     * @param task 下载任务
     * @param itemFileSize 此任务的文件大小
     * @param totalTs 总切片数
     * @param curTs 已下载切片数
     */
    void onDownloadItem(M3U8Task task, long itemFileSize, int totalTs, int curTs);

    /**
     * 下载成功
     */
    void onDownloadSuccess(M3U8Task task);

    /**
     * 暂停下载
     * @param task 暂停的任务
     */
    void onDownloadPause(M3U8Task task);

    /**
     * 准备转成MP4
     */
    void onConvert();

    /**
     * 下载失败
     * @param task 失败的任务
     * @param error 错误信息
     */
    void onDownloadError(M3U8Task task, Throwable error);

    /**
     * 停止下载
     * @param task 停止的任务
     */
    void onStop(M3U8Task task);
}
