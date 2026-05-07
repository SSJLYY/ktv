import request from './request'

/**
 * 歌曲管理 API
 */

export const getSongList = (params) => {
  const { pageNum, pageSize, ...rest } = params || {}
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )

  return request({
    url: '/admin/songs',
    method: 'GET',
    params: {
      current: pageNum,
      size: pageSize,
      ...cleanRest,
    },
  })
}

export const getSongById = (id) => {
  return request({
    url: `/admin/songs/${id}`,
    method: 'GET',
  })
}

export const addSong = (data) => {
  return request({
    url: '/admin/songs',
    method: 'POST',
    data,
  })
}

export const updateSong = (id, data) => {
  return request({
    url: `/admin/songs/${id}`,
    method: 'PUT',
    data,
  })
}

export const deleteSong = (id) => {
  return request({
    url: `/admin/songs/${id}`,
    method: 'DELETE',
  })
}

export const uploadSongFile = (songId, file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: `/admin/songs/${songId}/upload`,
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: onProgress,
  })
}

export const uploadCoverImage = (songId, file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: `/admin/songs/${songId}/cover`,
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: onProgress,
  })
}

export const getCategoryList = () => {
  return request({
    url: '/admin/categories/all',
    method: 'GET',
  })
}
