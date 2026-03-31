package com.ktv.common.util;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;

/**
 * 拼音工具类
 * 使用pinyin库进行拼音转换
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public class PinyinUtil {

    /**
     * 获取汉字的拼音全拼
     *
     * @param chinese 汉字字符串
     * @return 拼音全拼，如："中国" -> "zhongguo"
     */
    public static String getPinyin(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        // 转为小写，移除空格
        return PinyinHelper.toPinyin(chinese, PinyinStyleEnum.NORMAL).toLowerCase().replace(" ", "");
    }

    /**
     * 获取汉字的拼音首字母
     *
     * @param chinese 汉字字符串
     * @return 拼音首字母，如："中国" -> "zg"
     */
    public static String getPinyinInitial(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        // 转为首字母，转大写，移除空格
        return PinyinHelper.toPinyin(chinese, PinyinStyleEnum.FIRST_LETTER).toUpperCase().replace(" ", "");
    }

    /**
     * 获取汉字的拼音首字母（小写）
     *
     * @param chinese 汉字字符串
     * @return 拼音首字母（小写），如："中国" -> "zg"
     */
    public static String getPinyinInitialLower(String chinese) {
        String initial = getPinyinInitial(chinese);
        return initial.isEmpty() ? "" : initial.toLowerCase();
    }

    /**
     * 获取首个汉字的拼音首字母（用于索引分类）
     *
     * @param chinese 汉字字符串
     * @return 首个汉字的拼音首字母，如："中国" -> "Z"，"美国" -> "M"
     */
    public static String getFirstLetter(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "#";
        }
        String initial = getPinyinInitial(chinese);
        return initial.isEmpty() ? "#" : String.valueOf(initial.charAt(0));
    }

    /**
     * 判断是否为汉字
     *
     * @param c 字符
     * @return true-是汉字，false-不是汉字
     */
    public static boolean isChinese(char c) {
        return String.valueOf(c).matches("[\\u4e00-\\u9fa5]");
    }

    /**
     * 将汉字转换为带拼音的格式（如：中国(zhongguo)）
     *
     * @param chinese 汉字字符串
     * @return 带拼音的格式
     */
    public static String toPinyinWithOriginal(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        String pinyin = getPinyin(chinese);
        return pinyin.isEmpty() ? chinese : chinese + "(" + pinyin + ")";
    }

}
