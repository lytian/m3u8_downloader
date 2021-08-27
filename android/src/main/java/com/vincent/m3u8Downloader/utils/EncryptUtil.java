package com.vincent.m3u8Downloader.utils;

import android.text.TextUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @Author: Vincent
 * @CreateAt: 2021/08/25 16:42
 * @Desc: 加解密工具
 */
public class EncryptUtil {

    private final static String ENCODING = "UTF-8";

    /**
     * md5加密字符串
     * @param str 待加密字符串
     * @return 加密后的字符串
     */
    public static String md5Encode(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * 生成密钥
     * 自动生成base64 编码后的AES128位密钥
     */
    public static String getAESKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        SecretKey sk = kg.generateKey();
        byte[] b = sk.getEncoded();
        return parseByte2HexStr(b);
    }

    /**
     * AES 加密
     * @param base64Key   base64编码后的 AES key
     * @param text  待加密的字符串
     * @return 加密后的byte[]
     * @throws Exception 异常
     */
    public static byte[] getAESEncode(String base64Key, String text) throws Exception{
        return getAESEncode(base64Key, text.getBytes());
    }

    /**
     * AES 加密
     * @param base64Key   base64编码后的 AES key
     * @param bytes  待加密的bytes
     * @return 加密后的byte[]
     * @throws Exception 异常
     */
    public static byte[] getAESEncode(String base64Key, byte[] bytes) throws Exception{
        if (base64Key == null)return bytes;
        byte[] key = parseHexStr2Byte(base64Key);
        SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
        return cipher.doFinal(bytes);
    }

    /**
     * AES解密
     * @param base64Key   base64编码后的 AES key
     * @param text  待解密的字符串
     * @return 解密后的byte[]
     * @throws Exception 异常
     */
    public static byte[] getAESDecode(String base64Key, String text) throws Exception{
        return getAESDecode(base64Key, text.getBytes());
    }

    /**
     * AES解密
     * @param base64Key   base64编码后的 AES key
     * @param bytes  待解密的字符串
     * @return 解密后的byte[] 数组
     * @throws Exception 异常
     */
    public static byte[] getAESDecode(String base64Key, byte[] bytes) throws Exception{
        if (base64Key == null)return bytes;
        byte[] key = parseHexStr2Byte(base64Key);
        SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec);
        return cipher.doFinal(bytes);
    }

    /**
     * 将二进制转换成16进制
     * @param buf byte数组
     * @return 16进制字符串
     */
    public static String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     * @param hexStr 16进制字符串
     * @return byte[]
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length()/2];
        for (int i = 0; i< hexStr.length()/2; i++) {
            int high = Integer.parseInt(hexStr.substring(i*2, i*2+1), 16);
            int low = Integer.parseInt(hexStr.substring(i*2+1, i*2+2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 解密ts文件
     * @param bytes 文件字节流
     * @param key   m3u8的key
     * @param iv    m3u8的iv
     * @return  解密后的byte[]
     * @throws Exception 异常
     */
    public static byte[] decryptTs(byte[] bytes, String key, String iv) throws  Exception {
        if (TextUtils.isEmpty(key)) {
            return bytes;
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        byte[] ivByte = new byte[16];
        if (!TextUtils.isEmpty(iv)) {
            if (iv.startsWith("0x"))
                ivByte = parseHexStr2Byte(iv.substring(2));
            else
                ivByte = iv.getBytes();

            if (ivByte == null || ivByte.length != 16)
                ivByte = new byte[16];
        }
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(ENCODING), "AES");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        return cipher.doFinal(bytes);
    }
}
