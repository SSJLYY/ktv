package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ktv.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Mapper接口
 * Bug21修复：SysUserMapper 文件缺失，导致 OrderServiceImpl 和 SysUserServiceImpl 编译失败
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
