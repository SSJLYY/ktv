package com.ktv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 歌手DTO（新增/修改入参）
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class SingerDTO {

    /**
     * 歌手名（新增时必填）
     */
    @NotBlank(message = "歌手名不能为空", groups = {Create.class})
    private String name;

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
     * 状态：0禁用 1启用
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
