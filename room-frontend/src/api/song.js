import request from './request'

// ========== 歌曲搜索 ==========

export const searchSongs = (keyword, current = 1, size = 20) =>
  request.get('/api/room/songs/search', { params: { keyword, current, size } })

export const getSongsBySinger = (singerId, current = 1, size = 20) =>
  request.get(`/api/room/songs/by-singer/${singerId}`, { params: { current, size } })

export const getSongsByCategory = (categoryId, current = 1, size = 20) =>
  request.get(`/api/room/songs/by-category/${categoryId}`, { params: { current, size } })

export const getAllSingers = (pinyinInitial) =>
  request.get('/api/room/singers', { params: { pinyinInitial } })

export const getAllCategories = () =>
  request.get('/api/room/categories')

export const getHotSongs = (limit = 30) =>
  request.get('/api/room/songs/hot', { params: { limit } })
