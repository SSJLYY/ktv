package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户实体，对应表 `t_sys_user`。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码，使用 BCrypt 加密，不返回给前端。
     */
    @JsonIgnore
    private String password;

    /**
     * 真实姓名。
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 角色，例如 `super_admin`、`admin`。
     */
    private String role;

    /**
     * 状态：0 禁用，1 启用。
     */
    private Integer status;

    /**
     * 最后登录时间。
     */
    @TableField("last_login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录 IP。
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    public String getRoleText() {
        return "super_admin".equals(role) ? "超级管理员" : "普通管理员";
    }

    public String getStatusText() {
        return status != null && status == 1 ? "启用" : "禁用";
    }

    public boolean isSuperAdmin() {
        return "super_admin".equals(role);
    }

    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
