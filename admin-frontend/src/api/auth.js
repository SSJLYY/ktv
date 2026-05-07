import request from './request'

/**
 * 认证相关 API
 */

export const login = (data) => {
  return request({
    url: '/admin/login',
    method: 'POST',
    data,
  })
}

export const logout = () => {
  return request({
    url: '/admin/logout',
    method: 'POST',
  })
}
