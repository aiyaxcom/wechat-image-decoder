package com.aiyax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description:
 * Author: wangtao
 * Date: 2023/5/2 22:54
 */
public class WechatImage {

    public static final String outputDirectory = "C:\\Users\\ZCY007\\Documents\\DecodedWechatImage";
    public static final String inputDirectory = "C:\\Users\\ZCY007\\Documents\\WeChat Files\\wxid_0g8vu6dbi0bt21\\FileStorage\\MsgAttach";
    public static final String SPECIFIC_PERSON_OR_GROUP = ""; // 个人或群聊的id，如果为空，则解码所有图片
    public static final String EMPTY = "";
    public static final Map<String, Integer> IMAGE_STATISTICS_GROUP_BY_ID = new HashMap<>();

    public static int decodedFileCount = 0;
    public static int readFileCount = 0;

    public static void main(String[] args) throws IOException {
        File directory = new File(inputDirectory);
        if (!directory.isDirectory()) {
            System.out.println(inputDirectory + " is not a directory!");
            return;
        }
        traverseDirectory(directory);
        System.out.println("Decoded " + decodedFileCount + " files from " + readFileCount + " files.");
        System.out.println("Image statistics group by id:");
        IMAGE_STATISTICS_GROUP_BY_ID.forEach((k, v) -> System.out.println(k + ": " + v));
    }

    public static void traverseDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    traverseDirectory(file);
                } else if (file.getName().endsWith(".dat")) {
//                    System.out.println(file.getAbsolutePath());
                    decodeFile(file);
                }
            }
        }
    }

    public static void decodeFile(File file) throws IOException {
        byte[] fileBytes = readFile(file);
        Byte offset = getOffset(Arrays.copyOfRange(fileBytes, 0, 2));
        if (offset != null) {
            byte[] newFileBytes = xorByteArrays(fileBytes, offset);
            String outputFile = outputDirectory + refactorOutputPath(file.getAbsolutePath().replace(inputDirectory, EMPTY).replace(file.getName(), EMPTY)) + "\\" + file.getName().substring(0, file.getName().length() - 4) + ".jpg";
            writeFile(newFileBytes, outputFile);
        }
    }

    private static String refactorOutputPath(String originContext) {
        try {
            String[] split = originContext.split("\\\\");
            // 去除split数组中的blank值
            List<String> paths = Arrays.stream(split).filter(s -> !s.equals("")).collect(Collectors.toList());
            if (paths.size() == 3) {
                String path0 = paths.get(0);
                if (path0.length() != 32) {
                    throw new RuntimeException("路径不合法");
                } else {
                    // 若path0在IMAGE_STATISTICS_GROUP_BY_ID中不存在，则将path0作为key，value为1存入IMAGE_STATISTICS_GROUP_BY_ID
                    if (!IMAGE_STATISTICS_GROUP_BY_ID.containsKey(path0)) {
                        IMAGE_STATISTICS_GROUP_BY_ID.put(path0, 1);
                    } else {
                        // 若path0在IMAGE_STATISTICS_GROUP_BY_ID中存在，则将value+1
                        IMAGE_STATISTICS_GROUP_BY_ID.put(path0, IMAGE_STATISTICS_GROUP_BY_ID.get(path0) + 1);
                    }
                }
                String path1 = paths.get(1);
                if (!path1.equals("Image") && !path1.equals("Thumb")) {
                    throw new RuntimeException("路径不合法");
                }
                String path2 = paths.get(2);
                // 如果path2不为 yyyy-MM 年月格式，则路径不合法
                if (path2.length() != 7) {
                    throw new RuntimeException("路径不合法");
                }
                return "\\" + path2 + "\\" + path1 + "\\" + path0;
            }
        } catch (Exception e) {
            System.out.println("路径不合法" + originContext);
        }

        return originContext;
    }

    public static Byte getOffset(byte[] twoBytes) {
        byte[] jpgBytes = new byte[] {(byte) 0xFF, (byte) 0xD8}; // jpg文件的前两位
        byte[] pngBytes = new byte[] {(byte) 0x89, (byte) 0x50}; // png文件的前两位
        byte[] jpgXorResult = xorByteArrays(twoBytes, jpgBytes);
        String offset = convertToHexString(jpgXorResult);
        // 如果offset的前两位和后两位相等，取前两位字符给新变量offsetString
        if (offset.substring(2).equals(offset.substring(0, 2))) {
            String offsetString = offset.substring(0, 2);
            // 将offsetString转换为byte
            return hexStringToByte(offsetString);
        }

        byte[] pngXorResult = xorByteArrays(twoBytes, pngBytes);
        offset = convertToHexString(pngXorResult);
        // 如果offset的前两位和后两位相等，取前两位字符给新变量offsetString
        if (offset.substring(2).equals(offset.substring(0, 2))) {
            String offsetString = offset.substring(0, 2);
            // 将offsetString转换为byte
            return hexStringToByte(offsetString);
        }

        System.out.println("Unhandled prefix: " + convertToHexString(twoBytes));
        System.out.println("Unhandled offset: " + offset);

        return null;
    }

    private static byte[] readFile(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[fileInputStream.available()];
        fileInputStream.read(fileBytes);
        fileInputStream.close();
        readFileCount++;
        return fileBytes;
    }

    private static String convertToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02X", b));
        }
        return stringBuilder.toString();
    }

    private static byte hexStringToByte(String hexString) {
        if (hexString.length() != 2) {
            throw new IllegalArgumentException("hexString must be 2 characters in length");
        }
        return (byte) ((Character.digit(hexString.charAt(0), 16) << 4)
                + Character.digit(hexString.charAt(1), 16));
    }

    private static byte[] xorByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i % b.length]);
        }
        return result;
    }

    private static byte[] xorByteArrays(byte[] a, byte b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b);
        }
        return result;
    }

    private static void writeFile(byte[] fileBytes, String outputPath) throws IOException {
        File outputDir = new File(outputPath).getParentFile();
        if (!SPECIFIC_PERSON_OR_GROUP.equals(EMPTY) && !outputPath.contains(SPECIFIC_PERSON_OR_GROUP)) {
            return;
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
        fileOutputStream.write(fileBytes);
        fileOutputStream.close();
        decodedFileCount++;
    }

}
