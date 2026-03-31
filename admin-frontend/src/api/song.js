import request from './request'

/**
 * 歌曲管理API
 */

// 分页查询歌曲列表
export const getSongList = (params) => {
  // 后端参数名：current/size，前端传来的是 pageNum/pageSize
  // Bug B2修复：params 加默认值 {}，防止不传参时解构 undefined 报错
  // Bug B3修复：过滤空字符串参数，避免 name=''、singerId=''、language='' 等传给后端
  const { pageNum, pageSize, ...rest } = params || {}
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, v]) => v !== '' && v !== null && v !== undefined)
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

// 根据ID查询歌曲详情
export const getSongById = (id) => {
  return request({
    url: `/admin/songs/${id}`,
    method: 'GET',
  })
}

// 新增歌曲
export const addSong = (data) => {
  return request({
    url: '/admin/songs',
    method: 'POST',
    data,
  })
}

// 修改歌曲
export const updateSong = (id, data) => {
  return request({
    url: `/admin/songs/${id}`,
    method: 'PUT',
    data,
  })
}

// 删除歌曲
export const deleteSong = (id) => {
  return request({
    url: `/admin/songs/${id}`,
    method: 'DELETE',
  })
}

// 上传歌曲文件
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

// 上传封面图
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

// 获取分类列表（用于下拉选择）
// 注意：歌手列表请直接从 singer.js 导入 getSingerList，song.js 不应重复导出同名函数
export const getCategoryList = () => {
  return request({
    url: '/admin/categories/all',
    method: 'GET',
  })
}
