package com.ktv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 歌曲 DTO，用于新增和修改歌曲。
 */
@Data
public class SongDTO {

    /**
     * 歌曲名称，新增时必填。
     */
    @NotBlank(message = "歌曲名称不能为空", groups = {Create.class})
    private String name;

    /**
     * 歌手 ID，新增时必填。
     */
    @NotNull(message = "歌手 ID 不能为空", groups = {Create.class})
    private Long singerId;

    /**
     * 分类 ID。
     */
    private Long categoryId;

    /**
     * 语言，如国语、粤语、英语、日语、韩语、其他。
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

    public interface Create {
    }

    public interface Update {
    }
}
