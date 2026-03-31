package com.ktv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 包厢DTO（新增/修改入参）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class RoomDTO {

    /**
     * 包厢名称（新增时必填）
     */
    @NotBlank(message = "包厢名称不能为空", groups = Create.class)
    private String name;

    /**
     * 类型：小包/中包/大包/豪华包
     */
    @NotBlank(message = "包厢类型不能为空", groups = {Create.class})
    private String type;

    /**
     * 容纳人数
     */
    @NotNull(message = "容纳人数不能为空", groups = {Create.class})
    @Min(value = 1, message = "容纳人数至少为1")
    private Integer capacity;

    /**
     * 每小时价格（元）
     */
    @NotNull(message = "每小时价格不能为空", groups = {Create.class})
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal pricePerHour;

    /**
     * 最低消费（元）
     */
    @DecimalMin(value = "0", message = "最低消费不能为负数")
    private BigDecimal minConsumption;

    /**
     * 状态：0空闲 1使用中 2清洁中 3维修中
     */
    private Integer status;

    /**
     * 描述
     */
    private String description;

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
