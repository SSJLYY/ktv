package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 歌曲实体，对应表 `t_song`。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_song")
public class Song extends BaseEntity {

    /**
     * 歌曲名称。
     */
    private String name;

    /**
     * 歌手 ID。
     */
    private Long singerId;

    /**
     * 分类 ID。
     */
    private Long categoryId;

    /**
     * 拼音全拼。
     */
    private String pinyin;

    /**
     * 拼音首字母，大写。
     */
    private String pinyinInitial;

    /**
     * 语言，例如国语、粤语、英语、日语、韩语、其他。
     */
    private String language;

    /**
     * 时长，单位：秒。
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
     * 总播放次数。
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
}
