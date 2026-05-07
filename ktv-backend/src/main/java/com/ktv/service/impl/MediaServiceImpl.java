package com.ktv.service.impl;

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
 * 媒体文件服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^[A-Za-z]:.*");

    private final SongMapper songMapper;

    @Value("${media.base-path}")
    private String mediaBasePath;

    @Override
    public Resource getMediaStream(Long songId) {
        validatePositiveId(songId);
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return null;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("媒体文件不存在: {}", filePath);
            return null;
        }
        return new FileSystemResource(file);
    }

    @Override
    public Resource getMediaFile(Long songId) {
        return getMediaStream(songId);
    }

    @Override
    public String getMediaType(Long songId) {
        validatePositiveId(songId);
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return "application/octet-stream";
        }
        return MediaUtils.getMediaType(MediaUtils.getFileExtension(filePath));
    }

    @Override
    public long getMediaSize(Long songId) {
        validatePositiveId(songId);
        String filePath = getFilePath(songId);
        if (filePath == null) {
            return 0;
        }

        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    @Override
    public Resource getCoverImage(Long songId) {
        validatePositiveId(songId);
        Song song = songMapper.selectById(songId);
        if (song == null) {
            log.warn("歌曲不存在: {}", songId);
            return null;
        }

        String coverUrl = song.getCoverUrl();
        if (coverUrl == null || coverUrl.isBlank()) {
            return getDefaultCover();
        }

        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
            return null;
        }

        Path normalizedPath = resolveMediaPath(coverUrl);
        if (normalizedPath == null) {
            log.warn("封面路径非法: {}", coverUrl);
            return getDefaultCover();
        }

        File coverFile = normalizedPath.toFile();
        if (!coverFile.exists()) {
            Path coversPath = resolveMediaPath("covers");
            File coversDir = coversPath != null ? coversPath.toFile() : null;
            if (coversDir != null && coversDir.exists() && coversDir.isDirectory()) {
                File[] candidates = coversDir.listFiles((dir, name) -> name.startsWith(songId + "."));
                if (candidates != null && candidates.length > 0) {
                    return new FileSystemResource(candidates[0]);
                }
            }
            return getDefaultCover();
        }

        return new FileSystemResource(coverFile);
    }

    @Override
    public boolean mediaExists(Long songId) {
        validatePositiveId(songId);
        String filePath = getFilePath(songId);
        return filePath != null && new File(filePath).exists();
    }

    private void validatePositiveId(Long songId) {
        if (songId == null || songId <= 0) {
            throw new BusinessException("歌曲 ID 必须为正整数");
        }
    }

    private String getFilePath(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            log.warn("歌曲不存在: {}", songId);
            return null;
        }

        String filePath = song.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            log.warn("歌曲文件路径为空: songId={}", songId);
            return null;
        }

        Path normalizedPath = resolveMediaPath(filePath);
        if (normalizedPath == null) {
            log.warn("文件路径非法: {}", filePath);
            return null;
        }
        return normalizedPath.toString();
    }

    private Path resolveMediaPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        String normalizedInput = rawPath.trim().replace('\\', '/');
        if (normalizedInput.contains("..")) {
            return null;
        }

        Path basePath = Paths.get(mediaBasePath).normalize().toAbsolutePath();
        Path candidatePath;

        if (WINDOWS_PATH_PATTERN.matcher(normalizedInput).matches()) {
            candidatePath = Paths.get(normalizedInput);
        } else {
            String relativePart = normalizedInput.startsWith("/") ? normalizedInput.substring(1) : normalizedInput;
            candidatePath = basePath.resolve(relativePart);
        }

        Path normalizedPath = candidatePath.normalize().toAbsolutePath();
        if (!normalizedPath.startsWith(basePath)) {
            return null;
        }
        return normalizedPath;
    }

    private Resource getDefaultCover() {
        Path defaultCoverPath = resolveMediaPath("covers/default.jpg");
        if (defaultCoverPath == null) {
            return null;
        }

        File file = defaultCoverPath.toFile();
        if (file.exists()) {
            return new FileSystemResource(file);
        }
        return null;
    }
}
