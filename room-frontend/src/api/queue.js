import request from './request'

// ========== 点歌队列 ==========

export const addSongToQueue = (orderId, songId) =>
  request.post(`/api/room/${orderId}/queue/add`, null, { params: { songId } })

export const topSong = (orderId, orderSongId) =>
  request.post(`/api/room/${orderId}/queue/top/${orderSongId}`)

export const removeSong = (orderId, orderSongId) =>
  request.delete(`/api/room/${orderId}/queue/remove/${orderSongId}`)

export const getQueueList = (orderId, current = 1, size = 50) =>
  request.get(`/api/room/${orderId}/queue`, { params: { current, size } })

export const getPlayedList = (orderId, current = 1, size = 50) =>
  request.get(`/api/room/${orderId}/queue/played`, { params: { current, size } })
