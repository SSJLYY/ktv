package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ktv.common.exception.BusinessException;
import com.ktv.entity.Song;
import com.ktv.mapper.SongMapper;
import com.ktv.service.MediaService;
import com.ktv.util.MediaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 媒体文件Service实现类
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final SongMapper songMapper;

    /**
     * 媒体文件基础路径（从配置文件读取）
     * C8修复：使用 ${media.base-path} 让 yml 默认值生效，避免 Java 代码覆盖
     */
    @Value("${media.base-path}")
    private String mediaBasePath;

    /**
     * 获取媒体流
     */
    @Override
    public Resource getMediaStream(Long songId) {
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("媒体文件不存在：{}", filePath);
            return null;
        }
        return new FileSystemResource(file);
    }

    /**
     * 获取媒体文件
     */
    @Override
    public Resource getMediaFile(Long songId) {
        return getMediaStream(songId);
    }

    /**
     * S10修复：获取媒体类型统一使用 MediaUtils
     */
    @Override
    public String getMediaType(Long songId) {
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return "application/octet-stream";
        }
        return MediaUtils.getMediaType(MediaUtils.getFileExtension(filePath));
    }

    /**
     * 获取媒体文件大小
     */
    @Override
    public long getMediaSize(Long songId) {
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return 0;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        return file.length();
    }

    /**
     * M4修复：编译正则表达式为静态常量，避免每次调用时重新编译
     */
    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^[A-Za-z]:.*");

    /**
     * 获取歌曲封面图
     * C3修复：添加路径穿越检查
     */
    @Override
    public Resource getCoverImage(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            log.warn("歌曲不存在：{}", songId);
            return null;
        }

        String coverUrl = song.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            // 如果没有封面图，返回默认封面
            return getDefaultCover();
        }

        // 如果是URL，直接返回
        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
            // 外部URL，需要下载或代理
            // 这里简化处理，返回null让前端使用默认图
            return null;
        }

        // C3修复：检查路径穿越字符
        if (coverUrl.contains("..")) {
            log.warn("封面图路径包含路径穿越字符：{}", coverUrl);
            return getDefaultCover();
        }

        // 本地文件路径（可能是相对路径，如 /covers/1.jpg 或 covers/1.jpg）
        String absolutePath;
        if (coverUrl.startsWith("/") || WINDOWS_PATH_PATTERN.matcher(coverUrl).matches()) {
            // 以/开头的相对路径（如 /covers/1.jpg）或绝对路径，都拼上 mediaBasePath（去掉开头的 /）
            if (coverUrl.startsWith("/")) {
                absolutePath = mediaBasePath + coverUrl;
            } else {
                absolutePath = coverUrl;
            }
        } else {
            absolutePath = mediaBasePath + "/" + coverUrl;
        }

        // C3修复：使用Path.normalize()验证最终路径仍在mediaBasePath下
        Path normalizedPath = Paths.get(absolutePath).normalize().toAbsolutePath();
        Path basePath = Paths.get(mediaBasePath).normalize().toAbsolutePath();
        if (!normalizedPath.startsWith(basePath)) {
            log.warn("封面图路径超出mediaBasePath范围：{}", absolutePath);
            return getDefaultCover();
        }

        File coverFile = normalizedPath.toFile();
        if (!coverFile.exists()) {
            // 按 songId 扫描 covers 目录查找任意扩展名的封面
            File coversDir = new File(mediaBasePath + "/covers");
            if (coversDir.exists() && coversDir.isDirectory()) {
                File[] candidates = coversDir.listFiles((dir, name) ->
                        name.startsWith(songId + "."));
                if (candidates != null && candidates.length > 0) {
                    return new FileSystemResource(candidates[0]);
                }
            }
            return getDefaultCover();
        }

        return new FileSystemResource(coverFile);
    }

    /**
     * 检查媒体文件是否存在
     */
    @Override
    public boolean mediaExists(Long songId) {
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return false;
        }
        return new File(filePath).exists();
    }

    /**
     * 获取歌曲文件路径
     * C4修复：添加路径穿越检查
     */
    private String getFilePath(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            log.warn("歌曲不存在：{}", songId);
            return null;
        }

        String filePath = song.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            log.warn("歌曲文件路径为空：{}", songId);
            return null;
        }

        // C4修复：检查路径穿越字符
        if (filePath.contains("..")) {
            log.warn("文件路径包含路径穿越字符：{}", filePath);
            return null;
        }

        String finalPath;
        // 如果是绝对路径，直接使用但需要验证
        if (filePath.startsWith("/") || WINDOWS_PATH_PATTERN.matcher(filePath).matches()) {
            finalPath = filePath;
        } else {
            // 相对路径，拼接媒体基础路径
            finalPath = mediaBasePath + "/" + filePath;
        }

        // C4修复：验证最终路径是否在mediaBasePath范围内
        Path normalizedPath = Paths.get(finalPath).normalize().toAbsolutePath();
        Path basePath = Paths.get(mediaBasePath).normalize().toAbsolutePath();
        if (!normalizedPath.startsWith(basePath)) {
            log.warn("文件路径超出mediaBasePath范围：{}", finalPath);
            return null;
        }

        return normalizedPath.toString();
    }

    /**
     * S10修复：获取文件扩展名统一使用 MediaUtils
     */
    private String getFileExtension(String filePath) {
        return MediaUtils.getFileExtension(filePath);
    }

    /**
     * 获取默认封面图
     */
    private Resource getDefaultCover() {
        // 返回默认封面图路径
        String defaultCover = mediaBasePath + "/covers/default.jpg";
        File file = new File(defaultCover);
        if (file.exists()) {
            return new FileSystemResource(file);
        }
        return null;
    }
}
