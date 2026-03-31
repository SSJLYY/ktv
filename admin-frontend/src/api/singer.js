import request from './request'

/**
 * 歌手管理API
 */

// 分页查询歌手列表
// Bug11修复：前端用 pageNum/pageSize，后端 SingerController 接收 current/size
// Bug B2修复：params 加默认值 {}，防止调用方不传参时解构 undefined 报错
// Bug B3修复：过滤空字符串参数，避免将 name=''、region='' 传给后端
export const getSingerList = (params) => {
  const { pageNum, pageSize, ...rest } = params || {}
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, v]) => v !== '' && v !== null && v !== undefined)
  )
  return request({
    url: '/admin/singers',
    method: 'GET',
    params: { current: pageNum, size: pageSize, ...cleanRest },
  })
}

// 根据ID查询歌手详情
export const getSingerById = (id) => {
  return request({
    url: `/admin/singers/${id}`,
    method: 'GET',
  })
}

// 新增歌手
export const addSinger = (data) => {
  return request({
    url: '/admin/singers',
    method: 'POST',
    data,
  })
}

// 修改歌手
export const updateSinger = (id, data) => {
  return request({
    url: `/admin/singers/${id}`,
    method: 'PUT',
    data,
  })
}

// 删除歌手
export const deleteSinger = (id) => {
  return request({
    url: `/admin/singers/${id}`,
    method: 'DELETE',
  })
}
