package com.oss.file.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件哈希工具类
 * 用于计算文件的SHA-256或MD5值
 */
public class FileHashUtil {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 计算文件的SHA-256哈希值
     * @param file 文件对象
     * @return SHA-256哈希值（十六进制字符串）
     */
    public static String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        return calculateHash(file, "SHA-256");
    }

    /**
     * 计算文件的MD5值
     * @param file 文件对象
     * @return MD5值（十六进制字符串）
     */
    public static String calculateMD5(File file) throws IOException, NoSuchAlgorithmException {
        return calculateHash(file, "MD5");
    }

    /**
     * 计算文件哈希值（通用方法）
     * @param file 文件对象
     * @param algorithm 算法名称（MD5, SHA-1, SHA-256等）
     * @return 哈希值（十六进制字符串）
     */
    private static String calculateHash(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = digest.digest();
        return bytesToHex(hashBytes);
    }

    /**
     * 计算输入流的SHA-256哈希值
     * @param data 字节数组
     * @return SHA-256哈希值（十六进制字符串）
     */
    public static String calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        return bytesToHex(hashBytes);
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 比较两个哈希值是否相等
     * @param hash1 哈希值1
     * @param hash2 哈希值2
     * @return 是否相等
     */
    public static boolean compareHash(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return false;
        }
        return hash1.equalsIgnoreCase(hash2);
    }
}
