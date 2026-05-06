package com.ktv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 包厢 DTO（新增/修改入参）。
 */
@Data
public class RoomDTO {

    @NotBlank(message = "包厢名称不能为空")
    private String name;

    @NotBlank(message = "包厢类型不能为空")
    private String type;

    @NotNull(message = "容纳人数不能为空")
    @Min(value = 1, message = "容纳人数至少为1")
    private Integer capacity;

    @NotNull(message = "每小时价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal pricePerHour;

    @DecimalMin(value = "0", message = "最低消费不能为负数")
    private BigDecimal minConsumption;

    private Integer status;

    private String description;

    public interface Create {
    }

    public interface Update {
    }
}
