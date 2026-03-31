import request from './request'

// ========== 歌曲检索 ==========

/** 搜索歌曲（按歌名/拼音） */
export const searchSongs = (keyword, pageNum = 1, pageSize = 20) =>
  request.get('/api/room/songs/search', { params: { keyword, pageNum, pageSize } })

/** 按歌手查歌 */
export const getSongsBySinger = (singerId, pageNum = 1, pageSize = 20) =>
  request.get(`/api/room/songs/by-singer/${singerId}`, { params: { pageNum, pageSize } })

/** 按分类查歌 */
export const getSongsByCategory = (categoryId, pageNum = 1, pageSize = 20) =>
  request.get(`/api/room/songs/by-category/${categoryId}`, { params: { pageNum, pageSize } })

/** 获取所有歌手 */
export const getAllSingers = (pinyinInitial) =>
  request.get('/api/room/singers', { params: { pinyinInitial } })

/** 获取所有分类 */
export const getAllCategories = () =>
  request.get('/api/room/categories')

/** 获取热门歌曲 */
export const getHotSongs = (limit = 30) =>
  request.get('/api/room/songs/hot', { params: { limit } })
