package com.ktv.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 歌手VO（查询出参）
 * N5修复：添加 @Builder 支持链式构建，保留 @Data（MyBatis resultMap + BeanUtils.copyProperties 需要 setter）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingerVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 歌手名
     */
    private String name;

    /**
     * 拼音全拼
     */
    private String pinyin;

    /**
     * 拼音首字母（大写）
     */
    private String pinyinInitial;

    /**
     * 性别：0未知 1男 2女 3组合
     */
    private Integer gender;

    /**
     * 地区：内地/港台/欧美/日韩/其他
     */
    private String region;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 歌曲数量（冗余字段）
     */
    private Integer songCount;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
