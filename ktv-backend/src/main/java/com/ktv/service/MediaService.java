package com.ktv.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * 媒体文件Service接口
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface MediaService {

    /**
     * 获取媒体流
     *
     * @param songId 歌曲ID
     * @return 媒体资源
     */
    Resource getMediaStream(Long songId);

    /**
     * 获取媒体文件
     *
     * @param songId 歌曲ID
     * @return 媒体资源
     */
    Resource getMediaFile(Long songId);

    /**
     * 获取媒体类型
     *
     * @param songId 歌曲ID
     * @return MIME类型
     */
    String getMediaType(Long songId);

    /**
     * 获取媒体文件大小
     *
     * @param songId 歌曲ID
     * @return 文件大小（字节）
     */
    long getMediaSize(Long songId);

    /**
     * 获取歌曲封面图
     *
     * @param songId 歌曲ID
     * @return 封面图资源
     */
    Resource getCoverImage(Long songId);

    /**
     * 检查媒体文件是否存在
     *
     * @param songId 歌曲ID
     * @return 是否存在
     */
    boolean mediaExists(Long songId);
}
