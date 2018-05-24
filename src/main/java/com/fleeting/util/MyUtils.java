package com.fleeting.util;

import java.util.Random;

/**
 * @author cxx
 * @date 2018/5/24
 */
public class MyUtils {

    public static final String SOURCES =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890_";

    public static String getAbsolueUrl(String fileName){
        String classpath = MyUtils.class.getResource("/").getPath();
        classpath = classpath.replace("test-classes","classes");
        String filePath = classpath + fileName;
        return filePath;
    }

    public static String generateString(int length) {
        Random random = new Random();
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = SOURCES.charAt(random.nextInt(SOURCES.length()));
        }
        return new String(text);
    }
}
