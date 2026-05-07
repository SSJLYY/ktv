import request from './request'

/**
 * 订单管理 API
 */

export const getOrderList = (params) => {
  const { pageNum, pageSize, ...rest } = params || {}
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )

  return request({
    url: '/admin/orders',
    method: 'GET',
    params: {
      current: pageNum,
      size: pageSize,
      ...cleanRest,
    },
  })
}

export const getOrderById = (id) => {
  return request({
    url: `/admin/orders/${id}`,
    method: 'GET',
  })
}

export const openOrder = (data) => {
  return request({
    url: '/admin/orders/open',
    method: 'POST',
    data,
  })
}

export const closeOrder = (id) => {
  return request({
    url: `/admin/orders/${id}/close`,
    method: 'POST',
  })
}

export const cancelOrder = (id) => {
  return request({
    url: `/admin/orders/${id}`,
    method: 'DELETE',
  })
}

export const getActiveOrderByRoomId = (roomId) => {
  return request({
    url: `/admin/orders/room/${roomId}/active`,
    method: 'GET',
  })
}
