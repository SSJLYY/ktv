package com.ktv.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PinyinUtil 单元测试
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
class PinyinUtilTest {

    @Test
    @DisplayName("getPinyin - 正常中文转拼音")
    void testGetPinyin_normal() {
        assertEquals("zhongguo", PinyinUtil.getPinyin("中国"));
        assertEquals("beijing", PinyinUtil.getPinyin("北京"));
    }

    @Test
    @DisplayName("getPinyin - 空值和空字符串")
    void testGetPinyin_empty() {
        assertEquals("", PinyinUtil.getPinyin(null));
        assertEquals("", PinyinUtil.getPinyin(""));
    }

    @Test
    @DisplayName("getPinyin - 混合中英文和数字")
    void testGetPinyin_mixed() {
        assertTrue(PinyinUtil.getPinyin("张3").startsWith("zhang"));
        assertTrue(PinyinUtil.getPinyin("Hello世界").contains("hello"));
    }

    @Test
    @DisplayName("getPinyinInitial - 汉字首字母")
    void testGetPinyinInitial_chinese() {
        assertEquals("ZG", PinyinUtil.getPinyinInitial("中国"));
        assertEquals("BJ", PinyinUtil.getPinyinInitial("北京"));
    }

    @Test
    @DisplayName("getPinyinInitial - 空值")
    void testGetPinyinInitial_empty() {
        assertEquals("", PinyinUtil.getPinyinInitial(null));
        assertEquals("", PinyinUtil.getPinyinInitial(""));
    }

    @Test
    @DisplayName("getFirstLetter - 首字母提取")
    void testGetFirstLetter() {
        assertEquals("Z", PinyinUtil.getFirstLetter("中国"));
        assertEquals("B", PinyinUtil.getFirstLetter("北京"));
        assertEquals("A", PinyinUtil.getFirstLetter("Apple"));
        assertEquals("#", PinyinUtil.getFirstLetter("123"));
        assertEquals("#", PinyinUtil.getFirstLetter(null));
    }

    @Test
    @DisplayName("isChinese - 汉字判断")
    void testIsChinese() {
        assertTrue(PinyinUtil.isChinese('中'));
        assertTrue(PinyinUtil.isChinese('国'));
        assertFalse(PinyinUtil.isChinese('A'));
        assertFalse(PinyinUtil.isChinese('1'));
        assertFalse(PinyinUtil.isChinese(' '));
    }

    @Test
    @DisplayName("toPinyinWithOriginal - 带原文的拼音格式")
    void testToPinyinWithOriginal() {
        assertEquals("中国(zhongguo)", PinyinUtil.toPinyinWithOriginal("中国"));
        assertEquals("", PinyinUtil.toPinyinWithOriginal(""));
        assertEquals("", PinyinUtil.toPinyinWithOriginal(null));
    }
}
