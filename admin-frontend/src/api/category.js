import request from './request'

export const getCategoryList = () =>
  request({
    url: '/admin/categories',
    method: 'GET',
  })

export const getAllCategories = () =>
  request({
    url: '/admin/categories/all',
    method: 'GET',
  })

export const getCategoryById = (id) =>
  request({
    url: `/admin/categories/${id}`,
    method: 'GET',
  })

export const addCategory = (data) =>
  request({
    url: '/admin/categories',
    method: 'POST',
    data,
  })

export const updateCategory = (id, data) =>
  request({
    url: `/admin/categories/${id}`,
    method: 'PUT',
    data,
  })

export const deleteCategory = (id) =>
  request({
    url: `/admin/categories/${id}`,
    method: 'DELETE',
  })
