package com.ktv.common.util;

import io.github.biezhi.tinypinyin.Pinyin;

/**
 * 拼音工具类
 * 使用 TinyPinyin（io.github.biezhi:TinyPinyin）进行拼音转换
 * Maven Central 可用，无需 JitPack 仓库
 *
 * TinyPinyin API 说明：
 *   - Pinyin.toPinyin(str, separator)  → 字符串转全拼，结果大写，如 "中国" → "ZHONGGUO"
 *   - Pinyin.toPinyin(char)            → 单字符转全拼，结果大写，如 '中' → "ZHONG"
 *   - Pinyin.isChinese(char)           → 是否为汉字
 *
 * @author shaun.sheng
 * @since 2026-04-01
 */
public class PinyinUtil {

    private PinyinUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 获取汉字的拼音全拼（小写，无空格）
     *
     * @param chinese 汉字字符串（可含英文/数字，非汉字原样保留）
     * @return 拼音全拼，如："中国" → "zhongguo"；"张3" → "zhang3"
     */
    public static String getPinyin(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        // TinyPinyin.toPinyin(str, "") 返回大写无分隔，如 "ZHONGGUO"
        return Pinyin.toPinyin(chinese, "").toLowerCase();
    }

    /**
     * 获取汉字的拼音首字母（大写，无空格）
     *
     * @param chinese 汉字字符串
     * @return 拼音首字母，如："中国" → "ZG"；非汉字字符原样保留
     */
    public static String getPinyinInitial(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (Pinyin.isChinese(c)) {
                // 取该汉字拼音的首字母，toUpperCase 保证大写
                sb.append(Pinyin.toPinyin(c).charAt(0));
            } else {
                // 非汉字（英文、数字等）原样保留并转大写
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * 获取汉字的拼音首字母（小写，无空格）
     *
     * @param chinese 汉字字符串
     * @return 拼音首字母（小写），如："中国" → "zg"
     */
    public static String getPinyinInitialLower(String chinese) {
        return getPinyinInitial(chinese).toLowerCase();
    }

    /**
     * 获取首个汉字的拼音首字母（用于索引分类，如按字母分组）
     *
     * @param chinese 汉字字符串
     * @return 首个汉字的拼音首字母（大写），如："中国" → "Z"；非汉字 → "#"
     */
    public static String getFirstLetter(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "#";
        }
        char first = chinese.charAt(0);
        if (Pinyin.isChinese(first)) {
            return String.valueOf(Pinyin.toPinyin(first).charAt(0));
        }
        // 英文字母直接转大写，其他字符返回 "#"
        if (Character.isLetter(first)) {
            return String.valueOf(Character.toUpperCase(first));
        }
        return "#";
    }

    /**
     * 判断是否为汉字
     *
     * @param c 字符
     * @return true-是汉字，false-不是汉字
     */
    public static boolean isChinese(char c) {
        return Pinyin.isChinese(c);
    }

    /**
     * 将汉字转换为带拼音的格式（如：中国(zhongguo)）
     *
     * @param chinese 汉字字符串
     * @return 带拼音的格式，如："中国" → "中国(zhongguo)"
     */
    public static String toPinyinWithOriginal(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        String pinyin = getPinyin(chinese);
        return pinyin.isEmpty() ? chinese : chinese + "(" + pinyin + ")";
    }

}
