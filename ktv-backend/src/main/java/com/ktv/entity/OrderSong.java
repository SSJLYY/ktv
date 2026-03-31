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
 * 点歌记录实体类
 * 对应表：t_order_song
 * 深度审查修复（第10轮）：添加 @TableLogic 支持，确保 BaseMapper 方法自动过滤已删除记录
 */
@Data
@TableName("t_order_song")
public class OrderSong implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 歌曲ID
     */
    @TableField("song_id")
    private Long songId;

    /**
     * 歌曲名（冗余，防止歌曲被删除后无法显示）
     */
    @TableField("song_name")
    private String songName;

    /**
     * 歌手名（冗余）
     */
    @TableField("singer_name")
    private String singerName;

    /**
     * 排序序号（越小越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：0等待 1播放中 2已播放 3已跳过
     */
    private Integer status;

    /**
     * 开始播放时间
     */
    @TableField("play_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime playTime;

    /**
     * 播放结束时间
     */
    @TableField("finish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /**
     * 点歌时间
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     * SQL-S1/S4修复：补充 update_time 字段，与其他表保持一致
     */
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0未删除，1已删除
     * 深度审查修复（第10轮）：添加 @TableLogic，确保 BaseMapper 自动过滤 deleted=1
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== 非数据库字段（关联查询用） ==========

    /**
     * 歌曲时长（秒）
     */
    @TableField(exist = false)
    private Integer duration;

    /**
     * 歌曲文件路径
     */
    @TableField(exist = false)
    private String filePath;

    // ========== 便捷方法 ==========

    /**
     * 状态描述
     * N6修复：使用枚举替代魔法数字
     */
    public String getStatusText() {
        return OrderSongStatusEnum.getDescription(status);
    }

    /**
     * 是否等待中
     */
    public boolean isWaiting() {
        return status != null && status == OrderSongStatusEnum.WAITING.getCode();
    }

    /**
     * 是否播放中
     */
    public boolean isPlaying() {
        return status != null && status == OrderSongStatusEnum.PLAYING.getCode();
    }

    /**
     * 是否已播放或已跳过
     */
    public boolean isFinished() {
        return status != null && (status == OrderSongStatusEnum.PLAYED.getCode() || status == OrderSongStatusEnum.SKIPPED.getCode());
    }
}
