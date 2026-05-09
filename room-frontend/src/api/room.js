import request, { isInactiveRoomOrderMessage } from './request'

export const getRoomOrder = (orderId) =>
  request.get(`/api/room/orders/${orderId}`, { skipErrorToast: true })

export const validateActiveRoomOrder = async (orderId) => {
  if (!Number.isInteger(orderId) || orderId <= 0) {
    return null
  }

  try {
    const res = await getRoomOrder(orderId)
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
