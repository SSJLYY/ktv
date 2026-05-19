import request, { isInactiveRoomOrderMessage } from './request'

export const getRoomOrder = (orderId) =>
  request.get(`/api/room/orders/${orderId}`, { skipErrorToast: true })

export const getActiveOrderByRoomId = (roomId) =>
  request.get(`/api/room/orders/room/${roomId}/active`, { skipErrorToast: true })

async function resolveActiveOrder(requester, value) {
  if (!Number.isInteger(value) || value <= 0) {
    return null
  }

  try {
    const res = await requester(value)
    const order = res?.data
    if (!order || order.status !== 1) {
      return null
    }

    return order
  } catch (error) {
    if (isInactiveRoomOrderMessage(error?.businessMessage || error?.message)) {
      return null
    }

    throw error
  }
}

export const validateActiveRoomOrder = (orderId) =>
  resolveActiveOrder(getRoomOrder, orderId)

export const getActiveRoomOrderByRoomId = (roomId) =>
  resolveActiveOrder(getActiveOrderByRoomId, roomId)
