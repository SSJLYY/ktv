package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ktv.common.enums.OrderSongStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点歌记录实体，对应表 `t_order_song`。
 */
@Data
@TableName("t_order_song")
public class OrderSong implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单 ID。
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 歌曲 ID。
     */
    @TableField("song_id")
    private Long songId;

    /**
     * 歌曲名称，冗余保存以防歌曲被删除后无法展示。
     */
    @TableField("song_name")
    private String songName;

    /**
     * 歌手名称，冗余保存。
     */
    @TableField("singer_name")
    private String singerName;

    /**
     * 排序序号，值越小越靠前。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：0 等待中，1 播放中，2 已播放，3 已跳过。
     */
    private Integer status;

    /**
     * 开始播放时间。
     */
    @TableField("play_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime playTime;

    /**
     * 播放结束时间。
     */
    @TableField("finish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /**
     * 点歌时间。
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 歌曲时长，单位：秒。
     */
    @TableField(exist = false)
    private Integer duration;

    /**
     * 歌曲文件路径。
     */
    @TableField(exist = false)
    private String filePath;

    public String getStatusText() {
        return OrderSongStatusEnum.getDescription(status);
    }

    public boolean isWaiting() {
        return status != null && status == OrderSongStatusEnum.WAITING.getCode();
    }

    public boolean isPlaying() {
        return status != null && status == OrderSongStatusEnum.PLAYING.getCode();
    }

    public boolean isFinished() {
        return status != null
                && (status == OrderSongStatusEnum.PLAYED.getCode()
                || status == OrderSongStatusEnum.SKIPPED.getCode());
    }
}
