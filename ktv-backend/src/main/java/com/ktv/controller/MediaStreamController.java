package com.ktv.controller;

import com.ktv.common.exception.BusinessException;
import com.ktv.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResourceRegion;

import java.io.File;
import java.io.IOException;

/**
 * 媒体流Controller
 * 支持音频/视频文件的流式传输和Range请求
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaStreamController {

    private final MediaService mediaService;

    /**
     * 流媒体接口
     * 支持HTTP Range请求，实现边播边下载和进度拖拽
     *
     * @param songId 歌曲ID
     * @param headers HTTP请求头（包含Range信息）
     * @return 媒体流
     */
    @GetMapping("/stream/{songId}")
    public ResponseEntity<Resource> streamMedia(
            @PathVariable Long songId,
            @RequestHeader HttpHeaders headers) {

        log.info("流媒体请求：songId={}", songId);

        // 检查文件是否存在
        if (!mediaService.mediaExists(songId)) {
            log.warn("媒体文件不存在：songId={}", songId);
            throw new BusinessException("媒体文件不存在");
        }

        // 获取媒体资源
        Resource resource = mediaService.getMediaStream(songId);
        if (resource == null) {
            throw new BusinessException("无法读取媒体文件");
        }

        // 获取文件大小
        long fileSize;
        try {
            fileSize = resource.contentLength();
        } catch (IOException e) {
            log.error("获取文件大小失败", e);
            throw new BusinessException("无法读取文件");
        }

        // 获取媒体类型
        String mediaType = mediaService.getMediaType(songId);

        // 检查是否有Range请求头
        String rangeHeader = headers.getFirst(HttpHeaders.RANGE);
        if (rangeHeader == null) {
            // 没有Range请求，返回整个文件
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mediaType))
                    .contentLength(fileSize)
                    .body(resource);
        }

        // 解析Range请求头
        // 格式：bytes=start-end
        try {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Long.parseLong(ranges[1])
                    : fileSize - 1;

            // 限制范围
            if (start >= fileSize) {
                start = 0;
            }
            if (end >= fileSize) {
                end = fileSize - 1;
            }

            long contentLength = end - start + 1;

            log.info("Range请求：songId={}, start={}, end={}, length={}", songId, start, end, contentLength);

            // 返回206 Partial Content（使用ResourceRegion让Spring自动处理部分内容传输）
            ResourceRegion region = new ResourceRegion(resource, start, contentLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(mediaType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(contentLength)
                    .body(region);

        } catch (NumberFormatException e) {
            log.error("Range请求解析失败", e);
            // 解析失败，返回整个文件
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mediaType))
                    .contentLength(fileSize)
                    .body(resource);
        }
    }

    /**
     * 获取歌曲封面图
     *
     * @param songId 歌曲ID
     * @return 封面图
     */
    @GetMapping("/cover/{songId}")
    public ResponseEntity<Resource> getCover(@PathVariable Long songId) {
        log.info("封面图请求：songId={}", songId);

        Resource coverResource = mediaService.getCoverImage(songId);
        if (coverResource == null) {
            // 返回默认封面或404
            return ResponseEntity.notFound().build();
        }

        // 根据封面图文件的扩展名决定 Content-Type，而非歌曲媒体类型
        String filename = coverResource.getFilename();
        String mediaType = "image/jpeg"; // 默认
        if (filename != null) {
            String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
            mediaType = switch (ext) {
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                default -> "image/jpeg";
            };
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .body(coverResource);
    }

    /**
     * 获取媒体文件信息
     *
     * @param songId 歌曲ID
     * @return 媒体信息
     */
    @GetMapping("/info/{songId}")
    public ResponseEntity<MediaInfo> getMediaInfo(@PathVariable Long songId) {
        log.info("媒体信息请求：songId={}", songId);

        if (!mediaService.mediaExists(songId)) {
            throw new BusinessException("媒体文件不存在");
        }

        MediaInfo info = new MediaInfo();
        info.setSongId(songId);
        info.setMediaType(mediaService.getMediaType(songId));
        info.setFileSize(mediaService.getMediaSize(songId));
        info.setStreamUrl("/api/media/stream/" + songId);
        info.setCoverUrl("/api/media/cover/" + songId);

        return ResponseEntity.ok(info);
    }

    /**
     * 媒体信息内部类
     */
    public static class MediaInfo {
        private Long songId;
        private String mediaType;
        private long fileSize;
        private String streamUrl;
        private String coverUrl;

        public Long getSongId() {
            return songId;
        }

        public void setSongId(Long songId) {
            this.songId = songId;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getStreamUrl() {
            return streamUrl;
        }

        public void setStreamUrl(String streamUrl) {
            this.streamUrl = streamUrl;
        }

        public String getCoverUrl() {
            return coverUrl;
        }

        public void setCoverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
        }
    }
}
