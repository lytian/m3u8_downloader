package com.vincent.m3u8Downloader.downloader;

import android.text.TextUtils;

import com.vincent.m3u8Downloader.bean.M3U8;
import com.vincent.m3u8Downloader.bean.M3U8Task;
import com.vincent.m3u8Downloader.bean.M3U8TaskState;
import com.vincent.m3u8Downloader.listener.OnM3U8DownloadListener;
import com.vincent.m3u8Downloader.listener.OnTaskDownloadListener;
import com.vincent.m3u8Downloader.utils.M3U8Log;
import com.vincent.m3u8Downloader.utils.M3U8Util;

import java.io.File;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 22:17
 * @Desc: M3U8下载器
 */
public class M3U8Downloader {
    private static M3U8Downloader instance;

    private final M3U8DownloadTask m3U8DownLoadTask;
    private M3U8Task currentM3U8Task;
    private OnM3U8DownloadListener onM3U8DownloadListener;
    private long currentTime;

    private M3U8Downloader() {
        m3U8DownLoadTask = new M3U8DownloadTask();
    }

    public static M3U8Downloader getInstance(){
        if (null == instance) {
            instance = new M3U8Downloader();
        }
        return instance;
    }

    public void setOnM3U8DownloadListener(OnM3U8DownloadListener onM3U8DownloadListener) {
        this.onM3U8DownloadListener = onM3U8DownloadListener;
    }

    /**
     * 防止快速点击引起ThreadPoolExecutor频繁创建销毁引起crash
     * @return 是否快速点击
     */
    private boolean isQuicklyClick(){
        boolean result = false;
        if (System.currentTimeMillis() - currentTime <= 100){
            result = true;
            M3U8Log.d("is too quickly click!");
        }
        currentTime = System.currentTimeMillis();
        return result;
    }

    /**
     * 下载m3u8
     * @param url m3u8下载地址
     */
    public void download(String url) {
        if (TextUtils.isEmpty(url) || isQuicklyClick()) return;

        // 暂停之前的
        if (currentM3U8Task != null && !currentM3U8Task.getUrl().equals(url)) {
            pauseCurrent();
        }

        // 开启新的下载
        M3U8Task task = new M3U8Task(url);
        // 准备任务
        pendingTask(task);
        try {
            currentM3U8Task = task;
            M3U8Log.d("start downloading: " + task.getUrl());
            m3U8DownLoadTask.download(task.getUrl(), onTaskDownloadListener);
        } catch (Exception e){
            e.printStackTrace();
            M3U8Log.e("startDownloadTask Error:"+e.getMessage());
        }
    }

    /**
     * 挂起任务
     * @param task 下载任务
     */
    private void pendingTask(M3U8Task task){
        task.setState(M3U8TaskState.PENDING);
        if (onM3U8DownloadListener != null){
            onM3U8DownloadListener.onDownloadPending(task);
        }
    }

    /**
     * 暂停任务（非当前任务）
     */
    public void pause(String url){
        M3U8Log.d("pause download: " + url);
        if (currentM3U8Task == null || url == null) return;
        if (currentM3U8Task.getUrl().equals(url)) {
            pauseCurrent();
        }

    }

    /**
     * 暂停当前任务
     */
    private void pauseCurrent() {
        if (currentM3U8Task == null || currentM3U8Task.getState() != M3U8TaskState.DOWNLOADING) return;

        currentM3U8Task.setState(M3U8TaskState.PAUSE);
        if (onM3U8DownloadListener != null) {
            onM3U8DownloadListener.onDownloadPause(currentM3U8Task);
        }
        m3U8DownLoadTask.stop();
    }

    /**
     * 删除下载文件。非线程安全
     * @param url 下载地址
     * @return 删除状态
     */
    public boolean delete(final String url){
        if (currentM3U8Task != null && currentM3U8Task.getUrl().equals(url)) {
            currentM3U8Task.setState(M3U8TaskState.DEFAULT);
            m3U8DownLoadTask.stop();
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onStop(currentM3U8Task);
            }
        }
        String saveDir = M3U8Util.getSaveFileDir(url);
        // 删除文件夹
        boolean isDelete = M3U8Util.clearDir(new File(saveDir));
        // 删除mp4文件
        if (isDelete) {
            isDelete = M3U8Util.clearDir(new File(saveDir + ".mp4"));
        }
        return isDelete;
    }

    /**
     * 是否正在下载
     * @return 运行状态
     */
    public boolean isRunning(){
        return m3U8DownLoadTask.isRunning();
    }

    /**
     * 下载任务监听器
     */
    private final OnTaskDownloadListener onTaskDownloadListener = new OnTaskDownloadListener() {
        private long lastLength;
        private float downloadProgress;

        @Override
        public void onStart() {
            currentM3U8Task.setState(M3U8TaskState.PREPARE);
            if (onM3U8DownloadListener != null){
                onM3U8DownloadListener.onDownloadPrepare(currentM3U8Task);
            }
            M3U8Log.d("onDownloadPrepare: "+ currentM3U8Task.getUrl());
        }

        @Override
        public void onStartDownload(int totalTs, int curTs) {
            M3U8Log.d("onStartDownload: "+totalTs+"|"+curTs);

            currentM3U8Task.setState(M3U8TaskState.DOWNLOADING);
            if (totalTs > 0) {
                downloadProgress = 1.0f * curTs / totalTs;
            }
        }

        @Override
        public void onDownloadItem(long itemFileSize, int totalTs, int curTs) {
            if (!m3U8DownLoadTask.isRunning())return;
            M3U8Log.d("onDownloadItem: "+currentM3U8Task.getTotalSize()+"|"+itemFileSize+"|"+totalTs+"|"+curTs);

            if (totalTs > 0) {
                downloadProgress = 1.0f * curTs / totalTs;
            }
            if (onM3U8DownloadListener != null){
                onM3U8DownloadListener.onDownloadItem(currentM3U8Task, itemFileSize, totalTs, curTs);
            }
        }

        @Override
        public void onProgress(long curLength) {
            if (curLength - lastLength > 0) {
                currentM3U8Task.setProgress(downloadProgress);
                currentM3U8Task.setSpeed(curLength - lastLength);
                if (onM3U8DownloadListener != null ){
                    onM3U8DownloadListener.onDownloadProgress(currentM3U8Task);
                }
                lastLength = curLength;
            }
        }

        @Override
        public void onConvert() {
            M3U8Log.d("onConvert!");
            if (onM3U8DownloadListener != null){
                onM3U8DownloadListener.onConvert();
            }
        }

        @Override
        public void onSuccess(M3U8 m3U8) {
            M3U8Log.d("m3u8 Downloader onSuccess: "+ m3U8);
            m3U8DownLoadTask.stop();
            currentM3U8Task.setM3U8(m3U8);
            currentM3U8Task.setState(M3U8TaskState.SUCCESS);
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadSuccess(currentM3U8Task);
            }
        }

        @Override
        public void onError(Throwable error) {
            if (error.getMessage() != null && error.getMessage().contains("ENOSPC")){
                currentM3U8Task.setState(M3U8TaskState.ENOSPC);
            }else {
                currentM3U8Task.setState(M3U8TaskState.ERROR);
            }
            if (onM3U8DownloadListener != null) {
                onM3U8DownloadListener.onDownloadError(currentM3U8Task, error);
            }
            M3U8Log.e("onError: " + error.getMessage());
        }
    };
}
