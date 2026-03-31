package com.ktv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.dto.LoginDTO;
import com.ktv.entity.SysUser;
import com.ktv.vo.LoginVO;

/**
 * 系统用户Service
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     * 
     * @param loginDTO 登录DTO
     * @param loginIp 登录IP
     * @return 登录VO（包含JWT Token）
     */
    LoginVO login(LoginDTO loginDTO, String loginIp);

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getByUsername(String username);
}
