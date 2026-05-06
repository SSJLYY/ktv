package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体，对应表 `t_operation_log`。
 */
@Data
@TableName("t_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人 ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作人用户名。
     */
    @TableField("username")
    private String username;

    /**
     * 操作模块。
     */
    private String module;

    /**
     * 操作描述。
     */
    private String operation;

    /**
     * 请求方法。
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求 URL。
     */
    @TableField("request_url")
    private String requestUrl;

    /**
     * 请求参数，JSON 格式。
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应数据，JSON 格式。
     */
    @TableField("response_data")
    private String responseData;

    /**
     * IP 地址。
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 浏览器 User-Agent。
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 执行时长，单位：毫秒。
     */
    @TableField("execute_time")
    private Integer executeTime;

    /**
     * 状态：0 失败，1 成功。
     */
    private Integer status;

    /**
     * 错误信息。
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public String getStatusText() {
        return status != null && status == 1 ? "成功" : "失败";
    }

    public boolean isSuccess() {
        return status != null && status == 1;
    }
}
