package com.vincent.m3u8Downloader.bean;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 17:18
 * @Desc: 下载任务状态
 */
public enum M3U8TaskState {
    /**
     * 默认状态
     */
    DEFAULT,
    /**
     * 下载排队中
     */
    PENDING,
    /**
     * 下载准备中
     */
    PREPARE,
    /**
     * 正在下载中
     */
    DOWNLOADING,
    /**
     * 下载成功
     */
    SUCCESS,
    /**
     * 下载失败
     */
    ERROR,
    /**
     * 暂停下载
     */
    PAUSE,
    /**
     * 存储空间不足
     */
    ENOSPC,
}
