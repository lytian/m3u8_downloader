package com.vincent.m3u8Downloader.utils;

import android.annotation.SuppressLint;

import com.vincent.m3u8Downloader.downloader.M3U8DownloadConfig;
import com.vincent.m3u8Downloader.bean.M3U8;
import com.vincent.m3u8Downloader.bean.M3U8Ts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 21:43
 * @Desc: M3U8工具类
 */
public class M3U8Util {

    /**
     * 将Url转换为M3U8对象
     * @param url url地址
     * @return M3U8对象
     * @throws IOException IO异常
     */
    public static M3U8 parseIndex(String url) throws IOException {
        URL baseUrl = new URL(url);
        BufferedReader reader = new BufferedReader(new InputStreamReader(baseUrl.openStream()));

        M3U8 ret = new M3U8();
        ret.setBaseUrl(url);

        String line;
        float seconds = 0;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                if (line.startsWith("#EXTINF:")) {
                    line = line.substring(8);
                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    seconds = Float.parseFloat(line);
                } else if (line.startsWith("#EXT-X-KEY:")) {
                    line = line.split("#EXT-X-KEY:")[1];
                    String[] arr = line.split(",");
                    for (String s : arr) {
                        if (s.contains("=")) {
                            String k = s.split("=")[0];
                            String v = s.split("=")[1];
                            if (k.equals("URI")) {
                                // 获取key
                                v = v.replaceAll("\"", "");
                                v = v.replaceAll("'", "");
                                BufferedReader keyReader = new BufferedReader(new InputStreamReader(new URL(baseUrl, v).openStream()));
                                ret.setKey(keyReader.readLine());
                                M3U8Log.d("m3u8 key: " + ret.getKey());
                            } else if (k.equals("IV")) {
                                // 获取IV
                                ret.setIv(v);
                                M3U8Log.d("m3u8 IV: " + v);
                            }
                        }
                    }
                }
                continue;
            }
            if (line.endsWith("m3u8")) {
                return parseIndex(new URL(baseUrl, line).toString());
            }
            ret.addTs(new M3U8Ts(line, seconds));
            seconds = 0;
        }
        reader.close();

        return ret;
    }


    /**
     * 清空文件夹
     * @param dir 文件夹/文件地址
     * @return  删除状态
     */
    public static boolean clearDir(File dir) {
        if (dir.exists()) {
            if (dir.isFile()) {
                return dir.delete();
            } else if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        clearDir(file);
                    }
                }
                return dir.delete();
            }
        }
        return true;
    }


    private static final float KB = 1024;
    private static final float MB = 1024 * KB;
    private static final float GB = 1024 * MB;

    /**
     * 格式化文件大小
     * @param size 文件大小
     * @return 格式化字符串
     */
    @SuppressLint("DefaultLocale")
    public static String formatFileSize(long size){
        if (size >= GB) {
            return String.format("%.1f GB", size / GB);
        } else if (size >= MB) {
            float value = size / MB;
            return String.format(value > 100 ? "%.0f MB" : "%.1f MB", value);
        } else if (size >= KB) {
            float value =  size / KB;
            return String.format(value > 100 ? "%.0f KB" : "%.1f KB", value);
        } else {
            return String.format("%d B", size);
        }
    }

    /**
     * 生成本地m3u8索引文件，ts切片和m3u8文件放在相同目录下即可
     * @param m3U8 m3u8文件
     */
    public static void createLocalM3U8(String fileName, M3U8 m3U8) throws IOException{
        createLocalM3U8(fileName, m3U8, null);
    }

    /**
     * 生成AES-128加密本地m3u8索引文件，ts切片和m3u8文件放在相同目录下即可
     * @param m3U8 m3u8文件
     * @param keyPath 加密key
     */
    public static void createLocalM3U8(String fileName, M3U8 m3U8, String keyPath) throws IOException{
        M3U8Log.d("createLocalM3U8: " + fileName);
        BufferedWriter bfw = new BufferedWriter(new FileWriter(fileName, false));
        bfw.write("#EXTM3U\n");
        bfw.write("#EXT-X-VERSION:3\n");
        bfw.write("#EXT-X-MEDIA-SEQUENCE:0\n");
        bfw.write("#EXT-X-TARGETDURATION:13\n");
        if (keyPath != null) bfw.write("#EXT-X-KEY:METHOD=AES-128,URI=\""+keyPath+"\"\n");
        for (M3U8Ts m3U8Ts : m3U8.getTsList()) {
            bfw.write("#EXTINF:" + m3U8Ts.getSeconds()+",\n");
            bfw.write(m3U8Ts.obtainEncodeTsFileName());
            bfw.newLine();
        }
        bfw.write("#EXT-X-ENDLIST");
        bfw.flush();
        bfw.close();
    }

    /**
     * 获取文件流
     * @param fileName 文件名
     * @return 文件字节数组
     * @throws IOException IO异常
     */
    public static byte[] readFile(String fileName) throws IOException{
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        int length = fis.available();
        byte[] buffer = new byte[length];
        fis.read(buffer);
        fis.close();
        return buffer;
    }

    /**
     * 保存文件
     * @param bytes 字节数组
     * @param fileName 文件名
     * @throws IOException IO异常
     */
    public static void saveFile(byte[] bytes, String fileName) throws IOException{
        File file = new File(fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 保存文件
     * @param text 文件内容
     * @param fileName 文件名
     * @throws IOException IO异常
     */
    public static void saveFile(String text, String fileName) throws IOException{
        File file = new File(fileName);
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(text);
        out.flush();
        out.close();
    }

    /**
     * 获取url保存的地址
     * @param url 请求地址
     * @return 地址
     */
    public static String getSaveFileDir(String url){
        return M3U8DownloadConfig.getSaveDir() + File.separator + EncryptUtil.md5Encode(url);
    }
}
