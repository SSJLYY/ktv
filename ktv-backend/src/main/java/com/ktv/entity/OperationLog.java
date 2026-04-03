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
 * 操作日志实体类
 * 对应表：t_operation_log
 * 用于记录系统操作日志，便于审计和问题追踪
 * H13修复：添加deleted字段和@TableLogic注解，与表结构保持一致
 */
@Data
@TableName("t_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作人用户名（冗余）
     */
    @TableField("username")
    private String username;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求URL
     */
    @TableField("request_url")
    private String requestUrl;

    /**
     * 请求参数（JSON）
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应数据（JSON，可选）
     */
    @TableField("response_data")
    private String responseData;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 浏览器UA
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 执行时长（毫秒）
     */
    @TableField("execute_time")
    private Integer executeTime;

    /**
     * 状态：0失败 1成功
     */
    private Integer status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * H13修复：逻辑删除标识（0未删除 1已删除）
     * 与表结构保持一致，使MyBatis-Plus全局逻辑删除配置生效
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== 便捷方法 ==========

    /**
     * 状态描述
     */
    public String getStatusText() {
        return status != null && status == 1 ? "成功" : "失败";
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status != null && status == 1;
    }
}
