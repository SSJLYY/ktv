import request from './request'

/**
 * 包厢管理 API
 */

export const getRoomList = (params) => {
  const rest = { ...(params || {}) }
  delete rest.pageNum
  delete rest.pageSize

  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )

  return request({
    url: '/admin/rooms',
    method: 'GET',
    params: cleanRest,
  })
}

export const getAvailableRooms = () => {
  return request({
    url: '/admin/rooms/available',
    method: 'GET',
  })
}

export const getRoomById = (id) => {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'GET',
  })
}

export const addRoom = (data) => {
  return request({
    url: '/admin/rooms',
    method: 'POST',
    data,
  })
}

export const updateRoom = (id, data) => {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteRoom = (id) => {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'DELETE',
  })
}

export const updateRoomStatus = (id, status) => {
  return request({
    url: `/admin/rooms/${id}/status`,
    method: 'PUT',
    params: { status },
  })
}
