package com.ktv.controller;

import com.ktv.common.exception.BusinessException;
import com.ktv.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 媒体流控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaStreamController {

    private final MediaService mediaService;

    @GetMapping("/stream/{songId}")
    public ResponseEntity<?> streamMedia(@PathVariable Long songId, @RequestHeader HttpHeaders headers) {
        validatePositiveId(songId, "歌曲 ID 必须为正整数");
        log.info("媒体流请求: songId={}", songId);

        if (!mediaService.mediaExists(songId)) {
            log.warn("媒体文件不存在: songId={}", songId);
            throw new BusinessException("媒体文件不存在");
        }

        Resource resource = mediaService.getMediaStream(songId);
        if (resource == null) {
            throw new BusinessException("无法读取媒体文件");
        }

        long fileSize;
        try {
            fileSize = resource.contentLength();
        } catch (IOException e) {
            log.error("获取文件大小失败", e);
            throw new BusinessException("无法读取文件");
        }

        String mediaType = mediaService.getMediaType(songId);
        String rangeHeader = headers.getFirst(HttpHeaders.RANGE);
        if (rangeHeader == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mediaType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(fileSize)
                    .body(resource);
        }

        try {
            ByteRange range = parseRange(rangeHeader, fileSize);
            long contentLength = range.end() - range.start() + 1;

            log.info(
                    "Range 请求: songId={}, start={}, end={}, length={}",
                    songId,
                    range.start(),
                    range.end(),
                    contentLength
            );

            ResourceRegion region = new ResourceRegion(resource, range.start(), contentLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(mediaType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes %d-%d/%d".formatted(range.start(), range.end(), fileSize))
                    .contentLength(contentLength)
                    .body(region);
        } catch (IllegalArgumentException e) {
            log.error("Range 请求解析失败", e);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }
    }

    @GetMapping("/cover/{songId}")
    public ResponseEntity<Resource> getCover(@PathVariable Long songId) {
        validatePositiveId(songId, "歌曲 ID 必须为正整数");
        log.info("封面请求: songId={}", songId);

        Resource coverResource = mediaService.getCoverImage(songId);
        if (coverResource == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = coverResource.getFilename();
        String mediaType = "image/jpeg";
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

    @GetMapping("/info/{songId}")
    public ResponseEntity<MediaInfo> getMediaInfo(@PathVariable Long songId) {
        validatePositiveId(songId, "歌曲 ID 必须为正整数");
        log.info("媒体信息请求: songId={}", songId);

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

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private ByteRange parseRange(String rangeHeader, long fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("empty media file");
        }
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            throw new IllegalArgumentException("unsupported range header: " + rangeHeader);
        }

        String rawRange = rangeHeader.substring("bytes=".length()).trim();
        if (rawRange.isEmpty()) {
            throw new IllegalArgumentException("empty range header");
        }

        String firstRange = rawRange.split(",", 2)[0].trim();
        String[] bounds = firstRange.split("-", 2);
        if (bounds.length != 2) {
            throw new IllegalArgumentException("invalid range: " + rangeHeader);
        }

        long start;
        long end;
        if (bounds[0].isEmpty()) {
            long suffixLength = Long.parseLong(bounds[1]);
            if (suffixLength <= 0) {
                throw new IllegalArgumentException("invalid suffix range: " + rangeHeader);
            }
            long normalizedLength = Math.min(suffixLength, fileSize);
            start = fileSize - normalizedLength;
            end = fileSize - 1;
        } else {
            start = Long.parseLong(bounds[0]);
            end = bounds[1].isEmpty() ? fileSize - 1 : Long.parseLong(bounds[1]);
        }

        if (start < 0 || start >= fileSize || end < start) {
            throw new IllegalArgumentException("range out of bounds: " + rangeHeader);
        }

        return new ByteRange(start, Math.min(end, fileSize - 1));
    }

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

    private record ByteRange(long start, long end) {
    }
}
