package com.vincent.m3u8Downloader.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 16:31
 * @Desc: m3u8实体类
 */
public class M3U8 {
    private String baseUrl;
    private String dirPath;
    private String localPath;
    private String key;
    private String iv;

    private List<M3U8Ts> tsList = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public List<M3U8Ts> getTsList() {
        return tsList;
    }

    public void setTsList(List<M3U8Ts> tsList) {
        this.tsList = tsList;
    }

    public void addTs(M3U8Ts ts) {
        this.tsList.add(ts);
    }

    public long getTotalFileSize() {
        long fileSize = 0;
        for (M3U8Ts m3U8Ts : tsList){
            fileSize = fileSize + m3U8Ts.getFileSize();
        }
        return fileSize;
    }

    public long getTotalTime() {
        long totalTime = 0;
        for (M3U8Ts m3U8Ts : tsList){
            totalTime = totalTime + (int)(m3U8Ts.getSeconds() * 1000);
        }
        return totalTime;
    }

    @Override
    public String toString() {
        return "M3U8{" +
                "basePath='" + baseUrl + '\'' +
                ", dirPath='" + dirPath + '\'' +
                ", localPath='" + localPath + '\'' +
                ", key='" + key + '\'' +
                ", iv='" + iv + '\'' +
                ", totalFileSize=" + getTotalFileSize() +
                ", totalTime=" + getTotalTime() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        M3U8 m3U8 = (M3U8) o;
        return baseUrl.equals(m3U8.baseUrl);
    }
}
