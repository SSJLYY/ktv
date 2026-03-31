import request from './request'

/**
 * 订单管理API
 */

// 分页查询订单列表
export const getOrderList = (params) => {
  // 后端参数名：current/size，前端传来的是 pageNum/pageSize
  // Bug B2修复：params 加默认值 {}，防止不传参时解构 undefined 报错
  // Bug B3修复：过滤空字符串参数，避免 status=''、startDate=''、endDate='' 传给后端
  const { pageNum, pageSize, ...rest } = params || {}
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, v]) => v !== '' && v !== null && v !== undefined)
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

// 根据ID查询订单详情
export const getOrderById = (id) => {
  return request({
    url: `/admin/orders/${id}`,
    method: 'GET',
  })
}

// 开台
export const openOrder = (data) => {
  return request({
    url: '/admin/orders/open',
    method: 'POST',
    data,
  })
}

// 结账
export const closeOrder = (id) => {
  return request({
    url: `/admin/orders/${id}/close`,
    method: 'POST',
  })
}

// 取消订单
export const cancelOrder = (id) => {
  return request({
    url: `/admin/orders/${id}`,
    method: 'DELETE',
  })
}

// 获取包厢进行中订单
export const getActiveOrderByRoomId = (roomId) => {
  return request({
    url: `/admin/orders/room/${roomId}/active`,
    method: 'GET',
  })
}
