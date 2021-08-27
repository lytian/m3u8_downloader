package com.vincent.m3u8Downloader.bean;

import com.vincent.m3u8Downloader.utils.M3U8Util;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 17:20
 * @Desc: M3U8下载任务
 */
public class M3U8Task {

    private String url;
    private M3U8TaskState state = M3U8TaskState.DEFAULT;
    private long speed;
    private float progress;
    private M3U8 m3U8;

    private M3U8Task() {}

    public M3U8Task(String url){
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public M3U8TaskState getState() {
        return state;
    }

    public void setState(M3U8TaskState state) {
        this.state = state;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public M3U8 getM3U8() {
        return m3U8;
    }

    public void setM3U8(M3U8 m3U8) {
        this.m3U8 = m3U8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        M3U8Task m3U8Task = (M3U8Task) o;
        return url.equals(m3U8Task.url);
    }

    public String getFormatSpeed() {
        if (speed == 0) return "";
        return M3U8Util.formatFileSize(speed) + "/s";
    }

    public long getTotalSize() {
        if (m3U8 == null) return 0;
        return m3U8.getTotalFileSize();
    }

    public String getFormatTotalSize() {
        if (m3U8 == null) return "";
        long fileSize = getTotalSize();
        if (fileSize == 0) return "";
        return M3U8Util.formatFileSize(fileSize);
    }

    public String getFormatCurrentSize() {
        if (m3U8 == null)return "";
        return M3U8Util.formatFileSize((long)(progress * m3U8.getTotalFileSize()));
    }
}
