package com.ktv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前播放状态VO
 */
@Data
public class CurrentPlayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 播放状态：PLAYING（播放中）、PAUSED（已暂停）、NONE（无歌曲）
     */
    private String playStatus;

    /**
     * 当前播放歌曲ID
     */
    private Long orderSongId;

    /**
     * 歌曲ID
     */
    private Long songId;

    /**
     * 歌曲名
     */
    private String songName;

    /**
     * 歌手名
     */
    private String singerName;

    /**
     * 歌曲时长（秒）
     */
    private Integer duration;

    /**
     * 歌曲文件路径
     */
    private String filePath;

    /**
     * 播放开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime playTime;

    /**
     * 队列剩余数量
     */
    private Integer queueRemaining;
}
