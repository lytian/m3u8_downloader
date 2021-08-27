package com.vincent.m3u8Downloader.bean;

import com.vincent.m3u8Downloader.utils.EncryptUtil;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 16:36
 * @Desc: m3u8切片
 */
public class M3U8Ts implements Comparable<M3U8Ts>  {
    /**
     * ts网络请求地址（完整的网络请求地址请使用obtainFullUrl）
     */
    private String url;
    /**
     * 文件大小
     */
    private long fileSize;
    /**
     * ts秒数
     */
    private float seconds;

    public M3U8Ts(String url, float seconds) {
        this.url = url;
        this.seconds = seconds;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public float getSeconds() {
        return seconds;
    }

    public void setSeconds(float seconds) {
        this.seconds = seconds;
    }

    @Override
    public int compareTo(M3U8Ts m3U8Ts) {
        return url.compareTo(m3U8Ts.url);
    }

    @Override
    public String toString() {
        return "M3U8Ts{" +
                "url='" + url + '\'' +
                ", fileSize=" + fileSize +
                ", seconds=" + seconds +
                '}';
    }

    /**
     * 获取加密后的文件名
     * @return ts文件名
     */
    public String obtainEncodeTsFileName(){
        if (url == null) return "error.ts";

        return EncryptUtil.md5Encode(url).concat(".ts");
    }

    /**
     * 获取完整的URL地址
     * @param hostUrl host地址
     * @return URL地址
     */
    public URL obtainFullUrl(String hostUrl) throws MalformedURLException {
        if (url == null || hostUrl == null) {
            return null;
        }
        URL host = new URL(hostUrl);
        return new URL(host, url);
    }
}
