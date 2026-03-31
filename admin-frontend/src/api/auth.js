import request from './request'

/**
 * 认证相关API
 */

// 管理员登录
export const login = (data) => {
  return request({
    url: '/admin/login',
    method: 'POST',
    data,
  })
}

// 管理员登出
export const logout = () => {
  return request({
    url: '/admin/logout',
    method: 'POST',
  })
}
