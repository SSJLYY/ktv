package com.ktv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 歌曲DTO（新增/修改入参）
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class SongDTO {

    /**
     * 歌曲名（新增时必填）
     */
    @NotBlank(message = "歌曲名不能为空", groups = {Create.class})
    private String name;

    /**
     * 歌手ID（新增时必填）
     */
    @NotNull(message = "歌手ID不能为空", groups = {Create.class})
    private Long singerId;

    /**
     * 分类ID
     */
    private Long categoryId;

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

    /**
     * 新增校验组
     */
    public interface Create {
    }

    /**
     * 更新校验组
     */
    public interface Update {
    }
}
