package com.ktv.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 媒体文件工具类，统一处理文件扩展名和 MIME 类型判断。
 */
@Slf4j
public final class MediaUtils {

    private MediaUtils() {
        // 工具类禁止实例化
    }

    /**
     * 音视频文件扩展名白名单。
     */
    public static final Set<String> ALLOWED_MEDIA_EXTENSIONS = Set.of(
            "mp3", "flac", "wav", "ogg", "m4a", "mp4", "avi", "mkv", "webm"
    );

    /**
     * 音视频 MIME 类型白名单。
     */
    public static final Set<String> ALLOWED_MEDIA_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/flac",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/mp4",
            "video/mp4",
            "video/x-msvideo",
            "video/x-matroska",
            "video/webm",
            "video/x-ms-wmv"
    );

    /**
     * 图片文件扩展名白名单。
     */
    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    /**
     * 图片 MIME 类型白名单。
     */
    public static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * 扩展名到 MIME 类型的映射。
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
                    {"webp", "image/webp"}
            }
    ).collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));

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

    public static String getMediaType(String extension) {
        if (extension == null) {
            return "application/octet-stream";
        }
        return EXTENSION_TO_MEDIA_TYPE.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    public static boolean isMediaFile(String extension) {
        if (extension == null) {
            return false;
        }
        return ALLOWED_MEDIA_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static boolean isImageFile(String extension) {
        if (extension == null) {
            return false;
        }
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static boolean isVideoFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        String ext = getFileExtension(filePath);
        return Set.of("mp4", "avi", "mkv", "webm").contains(ext);
    }

    public static String getImageMediaType(String extension) {
        if (extension == null) {
            return "image/jpeg";
        }
        return EXTENSION_TO_MEDIA_TYPE.getOrDefault(extension.toLowerCase(), "image/jpeg");
    }
}
