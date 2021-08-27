package com.vincent.m3u8Downloader.listener;

import com.vincent.m3u8Downloader.bean.M3U8;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/26 9:58
 * @Desc: 获取M3U8文件信息的回调函数
 */
public interface OnInfoCallback {

    void success(M3U8 m3u8);
}
