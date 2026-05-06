package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 歌手实体，对应表 `t_singer`。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_singer")
public class Singer extends BaseEntity {

    /**
     * 歌手名称。
     */
    private String name;

    /**
     * 拼音全拼。
     */
    private String pinyin;

    /**
     * 拼音首字母，大写。
     */
    private String pinyinInitial;

    /**
     * 性别：0 未知，1 男，2 女，3 组合。
     */
    private Integer gender;

    /**
     * 地区：内地、港台、欧美、日韩、其他。
     */
    private String region;

    /**
     * 头像 URL。
     */
    private String avatar;

    /**
     * 歌曲数量，冗余字段。
     */
    private Integer songCount;

    /**
     * 状态：0 禁用，1 启用。
     */
    private Integer status;
}
