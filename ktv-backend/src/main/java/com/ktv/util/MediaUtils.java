package com.ktv.util;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 媒体文件工具类
 * S10修复：将散落在 SongController、MediaServiceImpl 等处的媒体类型判断逻辑统一提取
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
public final class MediaUtils {

    private MediaUtils() {
        // 工具类禁止实例化
    }

    /**
     * 音视频文件扩展名白名单
     */
    public static final Set<String> ALLOWED_MEDIA_EXTENSIONS = Set.of(
            "mp3", "flac", "wav", "ogg", "m4a", "mp4", "avi", "mkv", "webm"
    );

    /**
     * 音视频 MIME 类型白名单
     */
    public static final Set<String> ALLOWED_MEDIA_CONTENT_TYPES = Set.of(
            "audio/mpeg",       // mp3
            "audio/flac",       // flac
            "audio/wav",        // wav
            "audio/x-wav",
            "audio/ogg",        // ogg
            "audio/mp4",        // m4a
            "video/mp4",        // mp4
            "video/x-msvideo",  // avi
            "video/x-matroska", // mkv
            "video/webm",       // webm
            "video/x-ms-wmv"    // wmv
    );

    /**
     * 图片文件扩展名白名单
     */
    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    /**
     * 图片 MIME 类型白名单
     */
    public static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * 扩展名 → MIME 类型映射表
     */
    private static final Map<String, String> EXTENSION_TO_MEDIA_TYPE = Stream.of(
            new String[][]{
                    {"mp3", "audio/mpeg"},
                    {"flac", "audio/flac"},
                    {"wav", "audio/wav"},
                    {"ogg", "audio/ogg"},
                    {"m4a", "audio/mp4"},
                    {"mp4", "video/mp4"},
                    {"avi", "video/x-msvideo"},
                    {"mkv", "video/x-matroska"},
                    {"webm", "video/webm"},
                    {"jpg", "image/jpeg"},
                    {"jpeg", "image/jpeg"},
                    {"png", "image/png"},
                    {"gif", "image/gif"},
                    {"webp", "image/webp"},
            }
    ).collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));

    /**
     * 根据文件路径获取文件扩展名
     *
     * @param filePath 文件路径
     * @return 扩展名（小写），无扩展名则返回空字符串
     */
    public static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 根据扩展名获取 MIME 类型
     *
     * @param extension 文件扩展名（小写）
     * @return MIME 类型字符串，未知扩展名返回 application/octet-stream
     */
    public static String getMediaType(String extension) {
        if (extension == null) {
            return "application/octet-stream";
        }
        return EXTENSION_TO_MEDIA_TYPE.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    /**
     * 判断是否为音视频文件
     * L3修复：显式处理null情况，避免Set.contains(null)意图不清晰
     *
     * @param extension 文件扩展名
     * @return true 是音视频文件
     */
    public static boolean isMediaFile(String extension) {
        if (extension == null) {
            return false;
        }
        return ALLOWED_MEDIA_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 判断是否为图片文件
     * L3修复：显式处理null情况
     *
     * @param extension 文件扩展名
     * @return true 是图片文件
     */
    public static boolean isImageFile(String extension) {
        if (extension == null) {
            return false;
        }
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 判断是否为视频文件（基于文件路径）
     *
     * @param filePath 文件路径
     * @return true 是视频文件
     */
    public static boolean isVideoFile(String filePath) {
        if (filePath == null) return false;
        String ext = getFileExtension(filePath);
        return Set.of("mp4", "avi", "mkv", "webm").contains(ext);
    }

    /**
     * 根据图片扩展名获取MIME类型
     * M22修复：提供专门的图片MIME类型获取方法
     *
     * @param extension 图片扩展名（小写）
     * @return MIME类型字符串，未知扩展名返回 image/jpeg
     */
    public static String getImageMediaType(String extension) {
        if (extension == null) {
            return "image/jpeg";
        }
        return EXTENSION_TO_MEDIA_TYPE.getOrDefault(extension.toLowerCase(), "image/jpeg");
    }
}
