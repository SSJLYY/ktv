import request from './request'

/**
 * 包厢管理API
 */

// 查询包厢列表（后端返回 List<RoomVO>，不是分页结构）
// BugA1修复：后端 RoomController.getRoomList 参数只有 status/type（没有 current/size），
// 返回值是 List<RoomVO>，不是 IPage；前端不应传分页参数，也不应用 res.data.records 读取
// 过滤掉空字符串参数（前端用 '' 表示"全部"，后端不应接收这些空值）
export const getRoomList = (params) => {
  const { pageNum, pageSize, ...rest } = params || {}
  // pageNum/pageSize 丢弃（后端不支持分页）
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, v]) => v !== '' && v !== null && v !== undefined)
  )
  return request({
    url: '/admin/rooms',
    method: 'GET',
    params: cleanRest,
  })
}

// 获取空闲包厢列表
export const getAvailableRooms = () => {
  return request({
    url: '/admin/rooms/available',
    method: 'GET',
  })
}

// 根据ID查询包厢详情
export const getRoomById = (id) => {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'GET',
  })
}

// 新增包厢
export const addRoom = (data) => {
  return request({
    url: '/admin/rooms',
    method: 'POST',
    data,
  })
}

// 修改包厢
export const updateRoom = (id, data) => {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'PUT',
    data,
  })
}

// 删除包厢
export const deleteRoom = (id) => {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'DELETE',
  })
}

// 更新包厢状态
export const updateRoomStatus = (id, status) => {
  return request({
    url: `/admin/rooms/${id}/status`,
    method: 'PUT',
    params: { status },
  })
}
