package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 歌曲表
 * SQL-S3修复：继承 BaseEntity，复用 id/createTime/updateTime/deleted 公共字段
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_song")
public class Song extends BaseEntity {

    /**
     * 歌曲名
     */
    private String name;

    /**
     * 歌手ID
     */
    private Long singerId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 拼音全拼
     */
    private String pinyin;

    /**
     * 拼音首字母（大写）
     */
    private String pinyinInitial;

    /**
     * 语种：国语/粤语/英语/日语/韩语/其他
     */
    private String language;

    /**
     * 时长（秒）
     */
    private Integer duration;

    /**
     * 歌曲文件相对路径
     */
    private String filePath;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 歌词文件路径
     */
    private String lyricPath;

    /**
     * 总点播次数
     */
    private Integer playCount;

    /**
     * 是否热门：0否 1是
     */
    private Integer isHot;

    /**
     * 是否新歌：0否 1是
     */
    private Integer isNew;

    /**
     * 状态：0下架 1上架
     */
    private Integer status;
}
