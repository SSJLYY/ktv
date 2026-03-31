import request from './request'

// ========== 点歌队列 ==========

/** 点歌（加入队列） */
export const addSongToQueue = (orderId, songId) =>
  request.post(`/api/room/${orderId}/queue/add`, null, { params: { songId } })

/** 置顶 */
export const topSong = (orderId, orderSongId) =>
  request.post(`/api/room/${orderId}/queue/top/${orderSongId}`)

/** 取消点歌 */
export const removeSong = (orderId, orderSongId) =>
  request.delete(`/api/room/${orderId}/queue/remove/${orderSongId}`)

/** 查询排队列表 */
export const getQueueList = (orderId, page = 1, size = 50) =>
  request.get(`/api/room/${orderId}/queue`, { params: { page, size } })

/** 查询已唱列表 */
export const getPlayedList = (orderId, page = 1, size = 50) =>
  request.get(`/api/room/${orderId}/queue/played`, { params: { page, size } })
