package com.vincent.m3u8Downloader.downloader;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.vincent.m3u8Downloader.bean.M3U8;
import com.vincent.m3u8Downloader.bean.M3U8Ts;
import com.vincent.m3u8Downloader.listener.OnInfoCallback;
import com.vincent.m3u8Downloader.listener.OnTaskDownloadListener;
import com.vincent.m3u8Downloader.utils.EncryptUtil;
import com.vincent.m3u8Downloader.utils.M3U8Log;
import com.vincent.m3u8Downloader.utils.M3U8Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 22:26
 * @Desc: M3U8下载任务
 */
public class M3U8DownloadTask {

    public static final String LOCAL_FILE_NAME = "local.m3u8";
    public static final String M3U8_KEY_NAME = "key.key";

    private static final int WHAT_ON_ERROR = 1001;
    private static final int WHAT_ON_PROGRESS = 1002;
    private static final int WHAT_ON_SUCCESS = 1003;
    private static final int WHAT_ON_START_DOWNLOAD = 1004;
    private static final int WHAT_ON_CONVERT = 1005;

    // 文件保存地址
    private String saveDir;
    // 当前M3U8
    private M3U8 currentM3U8;
    // 线程池
    private ExecutorService executor;
    // 网速定时任务
    private Timer netSpeedTimer;
    // 任务是否正在运行
    private boolean isRunning = false;
    // 当前已经在下完成的大小
    private long curLength = 0;
    // 当前下载完成的文件个数
    private final AtomicInteger curTs = new AtomicInteger(0);
    // 总文件个数
    private volatile int totalTs = 0;
    // 单个文件的大小
    private volatile long itemFileSize = 0;
    // 下载任务监听器
    private OnTaskDownloadListener onTaskDownloadListener;
    int connTimeout;
    int readTimeout;
    int threadCount;

    private final WeakHandler mHandler = new WeakHandler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_ON_START_DOWNLOAD:
                    onTaskDownloadListener.onStartDownload(totalTs, curTs.get());
                    M3U8Log.d("start download, save dir: " + saveDir);
                    break;
                case WHAT_ON_PROGRESS:
                    onTaskDownloadListener.onDownloadItem(itemFileSize, totalTs, curTs.get());
                    break;
                case WHAT_ON_CONVERT:
                    onTaskDownloadListener.onConvert();
                    break;
                case WHAT_ON_SUCCESS:
                    if (netSpeedTimer != null) {
                        netSpeedTimer.cancel();
                    }
                    onTaskDownloadListener.onSuccess(currentM3U8);
                    break;
                case WHAT_ON_ERROR:
                    onTaskDownloadListener.onError((Throwable) msg.obj);
                    break;
            }
            return true;
        }
    });

    public M3U8DownloadTask() {
        connTimeout = M3U8DownloadConfig.getConnTimeout();
        readTimeout = M3U8DownloadConfig.getReadTimeout();
        threadCount = M3U8DownloadConfig.getThreadCount();
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 开始下载
     * @param url m3u8下载地址
     * @param onTaskDownloadListener 任务下载监听器
     */
    public void download(final String url, final OnTaskDownloadListener onTaskDownloadListener) {
        this.saveDir = M3U8Util.getSaveFileDir(url);
        this.onTaskDownloadListener = onTaskDownloadListener;
        onTaskDownloadListener.onStart();
        if (M3U8DownloadConfig.isConvertMp4()) {
            File file = new File(saveDir + ".mp4");
            // 已存在MP4文件，则已完成
            if (file.exists()) {
                mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
                return;
            }
        }
        if (!isRunning()) {
            // 获取m3u8
            getM3U8Info(url, new OnInfoCallback() {
                @Override
                public void success(M3U8 m3u8) {
                    start(m3u8);
                }
            });
        } else {
            handlerError(new Throwable("Task running"));
        }
    }

    /**
     * 获取m3u8信息
     * @param url m3u8地址
     * @param callback 回调函数
     */
    private synchronized void getM3U8Info(final String url, final OnInfoCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    M3U8 m3u8 = M3U8Util.parseIndex(url);
                    callback.success(m3u8);
                } catch (Exception e) {
                    handlerError(e);
                }
            }
        }).start();
    }

    /**
     * 开始下载
     */
    private void start(final M3U8 m3u8) {
        currentM3U8 = m3u8;
        mHandler.sendEmptyMessage(WHAT_ON_START_DOWNLOAD);
        new Thread() {
            @Override
            public void run() {
                try {
                    final CountDownLatch latch = new CountDownLatch(m3u8.getTsList().size());
                    // 开始下载
                    batchDownloadTs(m3u8, latch);

                    // 等待线程执行完毕
                    latch.await();

                    // 关闭线程池
                    if (executor != null) {
                        executor.shutdown();
                    }
                    if (isRunning) {
                        currentM3U8.setDirPath(saveDir);
                        if (M3U8DownloadConfig.isConvertMp4()) {
                            // 转成mp4
                            convertMP4();
                        } else {
                            // 否则生成local.m3u8文件
                            File m3u8File;
                            if (TextUtils.isEmpty(currentM3U8.getKey())) {
                                m3u8File = M3U8Util.createLocalM3U8(new File(saveDir), LOCAL_FILE_NAME, currentM3U8);
                            } else {
                                m3u8File = M3U8Util.createLocalM3U8(new File(saveDir), LOCAL_FILE_NAME, currentM3U8, M3U8_KEY_NAME);
                            }
                            currentM3U8.setLocalPath(m3u8File.getPath());
                        }

                        mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
                        isRunning = false;
                    }
                } catch (InterruptedIOException e) {
                    // 被中断了，使用stop时会抛出这个，不需要处理
                } catch (IOException e) {
                    handlerError(e);
                } catch (InterruptedException e) {
                    handlerError(e);
                } catch (Exception e) {
                    handlerError(e);
                }
            }
        }.start();
    }

    /**
     * 批量下载ts切片
     * @param m3u8 M3U8对象
     * @param latch 锁存器
     */
    private void batchDownloadTs(final M3U8 m3u8, final CountDownLatch latch) {
        final File dir = new File(saveDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!TextUtils.isEmpty(m3u8.getKey())) {
            // 保存key文件
            try {
                M3U8Util.saveFile(m3u8.getKey(), saveDir + File.separator + "key.key");
            } catch (IOException e) {
                handlerError(e);
            }
        }
        totalTs = m3u8.getTsList().size();
        // 重置线程池
        if (executor != null) {
            executor.shutdownNow();
            M3U8Log.d("executor is shutDown !");
        }
        M3U8Log.d("Downloading !");
        executor = Executors.newFixedThreadPool(threadCount);

        curTs.set(0);
        isRunning = true;
        // 重置网速定时器
        if (netSpeedTimer != null) {
            netSpeedTimer.cancel();
        }
        netSpeedTimer = new Timer();
        netSpeedTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            onTaskDownloadListener.onProgress(curLength);
            }
        }, 0, 1500);

        final String basePath = m3u8.getBaseUrl();
        for (final M3U8Ts m3u8Ts : m3u8.getTsList()) {
            // 每个TS文件下载单独一个线程
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    File file;
                    try {
                        file = new File(dir + File.separator + m3u8Ts.obtainEncodeTsFileName());
                    } catch (Exception e) {
                        file = new File(dir + File.separator + m3u8Ts.getUrl());
                    }

                    if (!file.exists()) {
                        FileOutputStream fos = null;
                        InputStream inputStream = null;
                        boolean readFinished = false;
                        try {
                            URL url = m3u8Ts.obtainFullUrl(basePath);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            // conn.addRequestProperty("Referer", "http://xxxxxxxx.com/");
                            conn.setConnectTimeout(connTimeout);
                            conn.setReadTimeout(readTimeout);
                            if (conn.getResponseCode() == 200) {
                                inputStream = conn.getInputStream();
                                fos = new FileOutputStream(file);//会自动创建文件
                                int len;
                                byte[] buf = new byte[1024];
                                while ((len = inputStream.read(buf)) != -1) {
                                    curLength += len;
                                    fos.write(buf, 0, len);//写入流中
                                }
                            } else {
                                handlerError(new Throwable(String.valueOf(conn.getResponseCode())));
                            }
                            readFinished = true;
                        } catch (MalformedURLException e) {
                            handlerError(e);
                        } catch (IOException e) {
                            handlerError(e);
                        } finally {
                            // 如果没有读取完，则删除
                            if (!readFinished && file.exists()) {
                                file.delete();
                            }
                            // 关流
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        itemFileSize = file.length();
                        m3u8Ts.setFileSize(itemFileSize);
                        mHandler.sendEmptyMessage(WHAT_ON_PROGRESS);
                    } else {
                        itemFileSize = file.length();
                        m3u8Ts.setFileSize(itemFileSize);
                    }
                    curTs.incrementAndGet();
                    latch.countDown();
                }
            });
        }
    }

    /**
     * M3U8转MP4
     */
    private void convertMP4() {
        mHandler.sendEmptyMessage(WHAT_ON_CONVERT);
        final File dir = new File(saveDir);

        FileOutputStream fos = null;
        InputStream inputStream = null;
        String mp4FilePath = saveDir + ".mp4";
        File mp4File = null;
        int len;
        try {
            mp4File = new File(mp4FilePath);
            if (mp4File.exists()) {
                mp4File.delete();
            }
            fos = new FileOutputStream(mp4File);
            byte[] bytes = new byte[1024];
            for (final M3U8Ts m3U8Ts : currentM3U8.getTsList()) {
                File file;
                try {
                    file = new File(dir + File.separator + m3U8Ts.obtainEncodeTsFileName());
                } catch (Exception e) {
                    file = new File(dir + File.separator + m3U8Ts.getUrl());
                }
                // ts片段不存在，直接跳过
                if(!file.exists())
                    continue;
                inputStream = new FileInputStream(file);
                if (!TextUtils.isEmpty(currentM3U8.getKey())) {
                    int available = inputStream.available();
                    if (bytes.length < available)
                        bytes = new byte[available];
                    inputStream.read(bytes);
                    // 解密，追加到mp4文件中
                    fos.write(EncryptUtil.decryptTs(bytes, currentM3U8.getKey(), currentM3U8.getIv()));
                } else {
                    // 追加到mp4文件中
                    while ((len = inputStream.read(bytes)) != -1) {
                        fos.write(bytes, 0, len);
                    }
                }
                // 关闭流
                inputStream.close();
            }
            // 设置文件路径
            currentM3U8.setLocalPath(mp4FilePath);
            // 合并成功，删除m3u8和ts文件
            M3U8Util.clearDir(dir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            handlerError(e);
        } catch (IOException e) {
            e.printStackTrace();
            handlerError(e);
        } catch (Exception e) {
            e.printStackTrace();
            handlerError(e);
        } finally {
            // 关流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mp4File != null && mp4File.exists() && mp4File.length() == 0) {
                mp4File.delete();
            }
        }
    }

    /**
     * 停止任务
     */
    public void stop() {
        // 停止网速定时器
        if (netSpeedTimer != null) {
            netSpeedTimer.cancel();
            netSpeedTimer = null;
        }
        isRunning = false;
        // 关闭线程池
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 处理异常
     * @param e 异常信息
     */
    private void handlerError(Throwable e) {
        if (!"Task running".equals(e.getMessage())) {
            stop();
        }
        // 不提示被中断的情况
        if ("thread interrupted".equals(e.getMessage())) {
            return;
        }
        e.printStackTrace();
        Message msg = Message.obtain();
        msg.obj = e;
        msg.what = WHAT_ON_ERROR;
        mHandler.sendMessage(msg);
    }

    /**
     * 获取m3u8本地路径
     * @param url m3u8地址
     * @return 文件路径
     */
    public static String getM3U8Path(String url) {
        return M3U8Util.getSaveFileDir(url) + File.separator + LOCAL_FILE_NAME;
    }

    /**
     * 获取m3u8本地路径
     * @param url m3u8地址
     * @return 文件路径
     */
    public static String getMp4Path(String url) {
        return M3U8Util.getSaveFileDir(url) + ".mp4";
    }

    /**
     * 获取m3u8本地文件
     * @param url m3u8网络地址
     * @return m3u8本地文件
     */
    public static File getM3U8File(String url) {
        try {
            return new File(M3U8Util.getSaveFileDir(url), LOCAL_FILE_NAME);
        } catch (Exception e){
            M3U8Log.e(e.getMessage());
        }
        return null;
    }
}
