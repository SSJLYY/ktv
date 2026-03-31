package com.ktv.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.SongDTO;
import com.ktv.entity.Song;
import com.ktv.mapper.SongMapper;
import com.ktv.service.SongService;
import com.ktv.util.MediaUtils;
import com.ktv.vo.SongVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 歌曲管理Controller
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/songs")
@RequiredArgsConstructor
@Validated
public class SongController {

    private final SongService songService;
    private final SongMapper songMapper;

    /**
     * 媒体文件基础路径
     */
    @Value("${media.base-path:D:/ktv-media}")
    private String mediaBasePath;

    /**
     * 允许上传的文件类型（S10修复：引用 MediaUtils 统一管理）
     */
    private static final List<String> ALLOWED_EXTENSIONS = List.copyOf(MediaUtils.ALLOWED_MEDIA_EXTENSIONS);

    /**
     * 允许上传的文件 MIME 类型白名单（S10修复：引用 MediaUtils 统一管理）
     */
    private static final Set<String> ALLOWED_MEDIA_CONTENT_TYPES = MediaUtils.ALLOWED_MEDIA_CONTENT_TYPES;

    /**
     * 允许上传的图片类型（S10修复：引用 MediaUtils 统一管理）
     */
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.copyOf(MediaUtils.ALLOWED_IMAGE_EXTENSIONS);

    /**
     * 允许上传的图片 MIME 类型白名单（S10修复：引用 MediaUtils 统一管理）
     */
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = MediaUtils.ALLOWED_IMAGE_CONTENT_TYPES;

    /**
     * 分页查询歌曲列表（带筛选）
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param name 歌曲名（可选）
     * @param singerId 歌手ID（可选）
     * @param categoryId 分类ID（可选）
     * @param language 语种（可选）
     * @return 分页结果
     */
    @GetMapping
    public Result<IPage<SongVO>> getSongPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long singerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer status) {
        
        IPage<SongVO> page = songService.getSongPage(current, size, name, singerId, categoryId, language, status);
        return Result.success(page);
    }

    /**
     * 新增歌曲
     * 
     * @param songDTO 歌曲DTO
     * @return 歌曲ID
     */
    @PostMapping
    public Result<Long> createSong(@Valid @RequestBody SongDTO songDTO) {
        Long id = songService.createSong(songDTO);
        return Result.success(id);
    }

    /**
     * 根据ID获取歌曲详情
     * 
     * @param id 歌曲ID
     * @return 歌曲VO
     */
    @GetMapping("/{id}")
    public Result<SongVO> getSongById(@PathVariable Long id) {
        SongVO songVO = songService.getSongById(id);
        return Result.success(songVO);
    }

    /**
     * 修改歌曲
     * 
     * @param id 歌曲ID
     * @param songDTO 歌曲DTO
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateSong(@PathVariable Long id,
                                      @Valid @RequestBody SongDTO songDTO) {
        Boolean success = songService.updateSong(id, songDTO);
        return Result.success(success);
    }

    /**
     * 删除歌曲（逻辑删除）
     *
     * @param id 歌曲ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSong(@PathVariable Long id) {
        Boolean success = songService.deleteSong(id);
        return Result.success(success);
    }

    /**
     * 上传歌曲文件
     * 支持 MP3、FLAC、MP4 等音视频格式
     *
     * @param file 上传的文件
     * @param songId 歌曲ID
     * @return 上传结果
     */
    @PostMapping("/{songId}/upload")
    public Result<UploadResult> uploadMediaFile(
            @PathVariable Long songId,
            @RequestParam("file") MultipartFile file) {

        log.info("上传文件请求：songId={}, fileName={}", songId, file.getOriginalFilename());

        // 检查歌曲是否存在
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名无效");
        }

        // S3修复：目录穿越防护 —— 校验文件名不含路径穿越字符（..、/、\）
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("文件名包含非法字符");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件格式，仅支持：mp3、flac、wav、ogg、m4a、mp4、avi、mkv、webm");
        }

        // 校验 MIME 类型，防止恶意文件伪装扩展名
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MEDIA_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("不支持的文件类型（MIME: " + contentType + "），仅支持音视频格式");
        }

        try {
            // 创建目录结构：{media.base-path}/{singer_id}/
            File singerDir = new File(mediaBasePath + "/" + song.getSingerId());
            if (!singerDir.exists()) {
                singerDir.mkdirs();
            }

            // 生成文件名：{song_id}.{ext}
            String newFileName = songId + "." + extension;
            File targetFile = new File(singerDir, newFileName);

            // 保存文件
            file.transferTo(targetFile);

            // 更新数据库中的文件路径（相对路径）
            String relativePath = song.getSingerId() + "/" + newFileName;
            song.setFilePath(relativePath);
            songMapper.updateById(song);

            // 刷新缓存
            songService.refreshSongCache(songId);

            log.info("文件上传成功：songId={}, path={}", songId, relativePath);

            // 返回结果
            UploadResult result = new UploadResult();
            result.setSongId(songId);
            result.setFileName(newFileName);
            result.setFilePath(relativePath);
            result.setFileSize(file.getSize());
            result.setMediaType(MediaUtils.getMediaType(extension));

            return Result.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传歌曲封面图
     *
     * @param file 上传的图片文件
     * @param songId 歌曲ID
     * @return 上传结果
     */
    @PostMapping("/{songId}/cover")
    public Result<UploadResult> uploadCoverImage(
            @PathVariable Long songId,
            @RequestParam("file") MultipartFile file) {

        log.info("上传封面图请求：songId={}, fileName={}", songId, file.getOriginalFilename());

        // 检查歌曲是否存在
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名无效");
        }

        // S3修复：目录穿越防护 —— 校验文件名不含路径穿越字符（..、/、\）
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("文件名包含非法字符");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的图片格式，仅支持：jpg、jpeg、png、gif、webp");
        }

        // 校验 MIME 类型，防止恶意文件伪装扩展名
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("不支持的图片类型（MIME: " + contentType + "），仅支持 jpg/png/gif/webp");
        }

        try {
            // 创建封面图目录
            File coverDir = new File(mediaBasePath + "/covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }

            // 生成文件名：{song_id}.{ext}
            String newFileName = songId + "." + extension;
            File targetFile = new File(coverDir, newFileName);

            // 保存文件
            file.transferTo(targetFile);

            // 更新数据库中的封面图路径
            String relativePath = "/covers/" + newFileName;
            song.setCoverUrl(relativePath);
            songMapper.updateById(song);

            // 刷新缓存
            songService.refreshSongCache(songId);

            log.info("封面图上传成功：songId={}, path={}", songId, relativePath);

            // 返回结果
            UploadResult result = new UploadResult();
            result.setSongId(songId);
            result.setFileName(newFileName);
            result.setFilePath(relativePath);
            result.setFileSize(file.getSize());
            result.setMediaType("image/" + extension);

            return Result.success(result);

        } catch (IOException e) {
            log.error("封面图上传失败", e);
            throw new BusinessException("封面图上传失败：" + e.getMessage());
        }
    }

    /**
     * S10修复：文件扩展名获取统一使用 MediaUtils
     */
    private String getFileExtension(String filename) {
        return MediaUtils.getFileExtension(filename);
    }

    /**
     * 上传结果内部类
     * N7修复：使用 @Data 替代手写 getter/setter
     */
    @Data
    public static class UploadResult {
        private Long songId;
        private String fileName;
        private String filePath;
        private long fileSize;
        private String mediaType;
    }
}
