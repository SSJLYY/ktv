package com.ktv.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MediaUtils 单元测试
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
class MediaUtilsTest {

    @Test
    @DisplayName("getFileExtension - 正常提取扩展名")
    void testGetFileExtension_normal() {
        assertEquals("mp3", MediaUtils.getFileExtension("song.mp3"));
        assertEquals("mp4", MediaUtils.getFileExtension("video.mp4"));
        assertEquals("jpg", MediaUtils.getFileExtension("cover.jpg"));
    }

    @Test
    @DisplayName("getFileExtension - 大写扩展名转小写")
    void testGetFileExtension_uppercase() {
        assertEquals("mp3", MediaUtils.getFileExtension("song.MP3"));
        assertEquals("png", MediaUtils.getFileExtension("image.PNG"));
    }

    @Test
    @DisplayName("getFileExtension - 边界情况")
    void testGetFileExtension_edge() {
        assertEquals("", MediaUtils.getFileExtension(null));
        assertEquals("", MediaUtils.getFileExtension(""));
        assertEquals("", MediaUtils.getFileExtension("noextension"));
        assertEquals("", MediaUtils.getFileExtension("trailingdot."));
    }

    @Test
    @DisplayName("isMediaFile - 音视频判断")
    void testIsMediaFile() {
        assertTrue(MediaUtils.isMediaFile("mp3"));
        assertTrue(MediaUtils.isMediaFile("flac"));
        assertTrue(MediaUtils.isMediaFile("mp4"));
        assertTrue(MediaUtils.isMediaFile("mkv"));
        assertFalse(MediaUtils.isMediaFile("jpg"));
        assertFalse(MediaUtils.isMediaFile("exe"));
        assertFalse(MediaUtils.isMediaFile(null));
    }

    @Test
    @DisplayName("isImageFile - 图片判断")
    void testIsImageFile() {
        assertTrue(MediaUtils.isImageFile("jpg"));
        assertTrue(MediaUtils.isImageFile("png"));
        assertTrue(MediaUtils.isImageFile("gif"));
        assertTrue(MediaUtils.isImageFile("webp"));
        assertFalse(MediaUtils.isImageFile("mp3"));
        assertFalse(MediaUtils.isImageFile("mp4"));
        assertFalse(MediaUtils.isImageFile(null));
    }

    @Test
    @DisplayName("isVideoFile - 视频文件判断")
    void testIsVideoFile() {
        assertTrue(MediaUtils.isVideoFile("video.mp4"));
        assertTrue(MediaUtils.isVideoFile("movie.mkv"));
        assertTrue(MediaUtils.isVideoFile("clip.webm"));
        assertFalse(MediaUtils.isVideoFile("audio.mp3"));
        assertFalse(MediaUtils.isVideoFile(null));
    }

    @Test
    @DisplayName("getMediaType - MIME类型获取")
    void testGetMediaType() {
        assertEquals("audio/mpeg", MediaUtils.getMediaType("mp3"));
        assertEquals("video/mp4", MediaUtils.getMediaType("mp4"));
        assertEquals("application/octet-stream", MediaUtils.getMediaType("unknown"));
        assertEquals("application/octet-stream", MediaUtils.getMediaType(null));
    }

    @Test
    @DisplayName("getImageMediaType - 图片MIME类型")
    void testGetImageMediaType() {
        assertEquals("image/jpeg", MediaUtils.getImageMediaType("jpg"));
        assertEquals("image/jpeg", MediaUtils.getImageMediaType("jpeg"));
        assertEquals("image/png", MediaUtils.getImageMediaType("png"));
        assertEquals("image/jpeg", MediaUtils.getImageMediaType(null)); // 默认值
    }
}
