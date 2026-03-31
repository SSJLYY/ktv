import request from './request'

// ========== 播放控制 ==========

/** 切歌（下一首） */
export const nextSong = (orderId) =>
  request.post(`/api/room/${orderId}/play/next`)

/** 重唱 */
export const replaySong = (orderId) =>
  request.post(`/api/room/${orderId}/play/replay`)

/** 暂停 */
export const pausePlay = (orderId) =>
  request.post(`/api/room/${orderId}/play/pause`)

/** 恢复播放 */
export const resumePlay = (orderId) =>
  request.post(`/api/room/${orderId}/play/resume`)

/** 查询当前播放状态 */
export const getCurrentPlayStatus = (orderId) =>
  request.get(`/api/room/${orderId}/play/current`)

// ========== 媒体流 ==========

/** 获取媒体流URL */
export const getMediaStreamUrl = (songId) =>
  `/api/media/stream/${songId}`

/** 获取封面图URL */
export const getCoverUrl = (songId) =>
  `/api/media/cover/${songId}`

/** 获取媒体信息 */
export const getMediaInfo = (songId) =>
  request.get(`/api/media/info/${songId}`)

/** 判断是否为视频文件 */
export const isVideoFile = (filePath) => {
  if (!filePath) return false
  return /\.(mp4|avi|mkv|webm)$/i.test(filePath)
}
