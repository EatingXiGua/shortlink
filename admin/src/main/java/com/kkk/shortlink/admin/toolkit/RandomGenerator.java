package com.kkk.shortlink.admin.toolkit;

import java.security.SecureRandom;

/**
 *
 */
public final class RandomGenerator {

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机分组id
     * @return
     */
    public static String generateRandom(){
        return generateRandom(6);
    }

    /**
     * 生成随机分组id
     * @param length 生成多少位
     * @return 分组ID
     */
    public static String generateRandom(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 从字符集中随机选择一个字符
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}
