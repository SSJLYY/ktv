import request from './request'

export const getSingerList = (params) => {
  const { pageNum, pageSize, ...rest } = params || {}
  const cleanRest = Object.fromEntries(
    Object.entries(rest).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )

  return request({
    url: '/admin/singers',
    method: 'GET',
    params: {
      current: pageNum,
      size: pageSize,
      ...cleanRest,
    },
  })
}

export const getSingerById = (id) =>
  request({
    url: `/admin/singers/${id}`,
    method: 'GET',
  })

export const addSinger = (data) =>
  request({
    url: '/admin/singers',
    method: 'POST',
    data,
  })

export const updateSinger = (id, data) =>
  request({
    url: `/admin/singers/${id}`,
    method: 'PUT',
    data,
  })

export const deleteSinger = (id) =>
  request({
    url: `/admin/singers/${id}`,
    method: 'DELETE',
  })
