package com.ktv.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.SongDTO;
import com.ktv.entity.Song;
import com.ktv.mapper.SongMapper;
import com.ktv.security.FileSecurityChecker;
import com.ktv.service.SongService;
import com.ktv.util.AdminAccessUtils;
import com.ktv.util.MediaUtils;
import com.ktv.vo.SongVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin/songs")
@RequiredArgsConstructor
@Validated
public class SongController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SongService songService;
    private final SongMapper songMapper;
    private final FileSecurityChecker fileSecurityChecker;

    @Value("${media.base-path}")
    private String mediaBasePath;

    private static final List<String> ALLOWED_EXTENSIONS = List.copyOf(MediaUtils.ALLOWED_MEDIA_EXTENSIONS);
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = MediaUtils.ALLOWED_IMAGE_EXTENSIONS;

    @GetMapping
    public Result<IPage<SongVO>> getSongPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long singerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer status
    ) {
        validatePageParams(current, size);
        validateOptionalPositiveId(singerId, "歌手 ID 必须为正整数");
        validateOptionalPositiveId(categoryId, "分类 ID 必须为正整数");
        validateStatus(status);
        return Result.success(songService.getSongPage(current, size, name, singerId, categoryId, language, status));
    }

    @PostMapping
    public Result<Long> createSong(
            @Validated(SongDTO.Create.class) @RequestBody SongDTO songDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        AdminAccessUtils.requireSuperAdmin(userId, role);
        return Result.success(songService.createSong(songDTO));
    }

    @GetMapping("/{id}")
    public Result<SongVO> getSongById(@PathVariable Long id) {
        validatePositiveId(id, "歌曲 ID 必须为正整数");
        return Result.success(songService.getSongById(id));
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateSong(
            @PathVariable Long id,
            @Validated(SongDTO.Update.class) @RequestBody SongDTO songDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        validatePositiveId(id, "歌曲 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        return Result.success(songService.updateSong(id, songDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSong(
            @PathVariable Long id,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        validatePositiveId(id, "歌曲 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        return Result.success(songService.deleteSong(id));
    }

    @PostMapping("/{songId}/upload")
    public Result<UploadResult> uploadMediaFile(
            @PathVariable Long songId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        validatePositiveId(songId, "歌曲 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Song song = loadSong(songId);
        validateFilePresence(file);
        validateFileSize(file, 100L * 1024 * 1024, "媒体文件大小不能超过 100MB");

        String originalFilename = requireOriginalFilename(file);
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的媒体文件格式");
        }

        String contentType = requireContentType(file);
        if (!contentType.startsWith("audio/")
                && !contentType.startsWith("video/")
                && !contentType.equals("application/octet-stream")) {
            throw new BusinessException("不支持的媒体文件类型");
        }

        Long singerId = song.getSingerId();
        if (singerId == null || singerId <= 0) {
            throw new BusinessException("歌曲歌手信息无效");
        }

        Path basePath = resolveBasePath();
        Path targetDir = resolveChildPath(basePath, singerId.toString());
        ensureDirectory(targetDir, "创建媒体目录失败");

        String newFileName = songId + "." + extension;
        Path targetFilePath = resolveChildPath(targetDir, newFileName);
        saveAndCheckFile(file, targetFilePath, originalFilename, contentType);

        String relativePath = singerId + "/" + newFileName;
        song.setFilePath(relativePath);
        persistSong(song, targetFilePath, "媒体文件路径保存失败");
        songService.refreshSongCache(songId);

        log.info("媒体文件上传成功: songId={}, path={}", songId, relativePath);
        return Result.success(
                buildUploadResult(songId, newFileName, relativePath, file.getSize(), MediaUtils.getMediaType(extension))
        );
    }

    @PostMapping("/{songId}/cover")
    public Result<UploadResult> uploadCoverImage(
            @PathVariable Long songId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        validatePositiveId(songId, "歌曲 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Song song = loadSong(songId);
        validateFilePresence(file);
        validateFileSize(file, 10L * 1024 * 1024, "封面图片大小不能超过 10MB");

        String originalFilename = requireOriginalFilename(file);
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的封面图片格式");
        }

        String contentType = requireContentType(file);
        if (!contentType.startsWith("image/")) {
            throw new BusinessException("不支持的封面图片类型");
        }

        Path basePath = resolveBasePath();
        Path coverDirPath = resolveChildPath(basePath, "covers");
        ensureDirectory(coverDirPath, "创建封面目录失败");

        String newFileName = songId + "." + extension;
        Path coverFilePath = resolveChildPath(coverDirPath, newFileName);
        saveAndCheckFile(file, coverFilePath, originalFilename, contentType);

        String relativePath = "/covers/" + newFileName;
        song.setCoverUrl(relativePath);
        persistSong(song, coverFilePath, "封面路径保存失败");
        songService.refreshSongCache(songId);

        log.info("封面图片上传成功: songId={}, path={}", songId, relativePath);
        return Result.success(
                buildUploadResult(songId, newFileName, relativePath, file.getSize(), MediaUtils.getImageMediaType(extension))
        );
    }

    private Song loadSong(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        return song;
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validateOptionalPositiveId(Long id, String message) {
        if (id != null && id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validatePageParams(Integer current, Integer size) {
        if (current == null || current <= 0) {
            throw new BusinessException("页码必须大于 0");
        }
        if (size == null || size <= 0) {
            throw new BusinessException("每页数量必须大于 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量不能超过 100");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException("状态值只能是 0 或 1");
        }
    }

    private void validateFilePresence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
    }

    private void validateFileSize(MultipartFile file, long maxBytes, String message) {
        if (file.getSize() > maxBytes) {
            throw new BusinessException(message);
        }
    }

    private String requireOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名无效");
        }
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("文件名包含非法路径字符");
        }
        return originalFilename;
    }

    private String requireContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException("无法识别文件类型");
        }
        return contentType.toLowerCase();
    }

    private Path resolveBasePath() {
        return Paths.get(mediaBasePath).normalize().toAbsolutePath();
    }

    private Path resolveChildPath(Path basePath, String child) {
        Path childPath = basePath.resolve(child).normalize().toAbsolutePath();
        if (!childPath.startsWith(basePath)) {
            throw new BusinessException("非法的文件路径");
        }
        return childPath;
    }

    private void ensureDirectory(Path path, String errorMessage) {
        File dir = path.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException(errorMessage);
        }
    }

    private void persistSong(Song song, Path uploadedFilePath, String errorMessage) {
        if (songMapper.updateById(song) <= 0) {
            deleteUploadedFileQuietly(uploadedFilePath);
            throw new BusinessException(errorMessage);
        }
    }

    private void deleteUploadedFileQuietly(Path uploadedFilePath) {
        if (uploadedFilePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(uploadedFilePath);
        } catch (IOException e) {
            log.warn("删除失败的上传文件时出错: {}", uploadedFilePath, e);
        }
    }

    private void saveAndCheckFile(MultipartFile file, Path targetFilePath, String originalFilename, String contentType) {
        try {
            file.transferTo(targetFilePath.toFile());
            FileSecurityChecker.SecurityCheckResult securityResult =
                    fileSecurityChecker.check(targetFilePath, originalFilename, contentType);
            if (!securityResult.passed()) {
                Files.deleteIfExists(targetFilePath);
                throw new BusinessException("文件安全校验未通过: " + securityResult.message());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败，请稍后重试");
        }
    }

    private UploadResult buildUploadResult(Long songId, String fileName, String filePath, long fileSize, String mediaType) {
        UploadResult result = new UploadResult();
        result.setSongId(songId);
        result.setFileName(fileName);
        result.setFilePath(filePath);
        result.setFileSize(fileSize);
        result.setMediaType(mediaType);
        return result;
    }

    private String getFileExtension(String filename) {
        return MediaUtils.getFileExtension(filename);
    }

    @Data
    public static class UploadResult {
        private Long songId;
        private String fileName;
        private String filePath;
        private long fileSize;
        private String mediaType;
    }
}
