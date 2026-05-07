package com.ktv.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌曲 VO，查询出参，包含歌手名和分类名。
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class SongVO {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 歌曲名称。
     */
    private String name;

    /**
     * 歌手 ID。
     */
    private Long singerId;

    /**
     * 歌手名称，关联查询。
     */
    private String singerName;

    /**
     * 分类 ID。
     */
    private Long categoryId;

    /**
     * 分类名称，关联查询。
     */
    private String categoryName;

    /**
     * 拼音全拼。
     */
    private String pinyin;

    /**
     * 拼音首字母，大写。
     */
    private String pinyinInitial;

    /**
     * 语种：国语、粤语、英语、日语、韩语、其他。
     */
    private String language;

    /**
     * 时长，单位秒。
     */
    private Integer duration;

    /**
     * 歌曲文件相对路径。
     */
    private String filePath;

    /**
     * 封面图片 URL。
     */
    private String coverUrl;

    /**
     * 歌词文件路径。
     */
    private String lyricPath;

    /**
     * 总点播次数。
     */
    private Integer playCount;

    /**
     * 是否热门：0 否，1 是。
     */
    private Integer isHot;

    /**
     * 是否新歌：0 否，1 是。
     */
    private Integer isNew;

    /**
     * 状态：0 下架，1 上架。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
