import request from './request'

/**
 * 分类管理API
 */

// 获取所有启用分类列表
export const getCategoryList = () => {
  return request({
    url: '/admin/categories',
    method: 'GET',
  })
}

// 获取所有分类列表(包含禁用的)
export const getAllCategories = () => {
  return request({
    url: '/admin/categories/all',
    method: 'GET',
  })
}

// 根据ID查询分类详情
export const getCategoryById = (id) => {
  return request({
    url: `/admin/categories/${id}`,
    method: 'GET',
  })
}

// 新增分类
export const addCategory = (data) => {
  return request({
    url: '/admin/categories',
    method: 'POST',
    data,
  })
}

// 修改分类
export const updateCategory = (id, data) => {
  return request({
    url: `/admin/categories/${id}`,
    method: 'PUT',
    data,
  })
}

// 删除分类
export const deleteCategory = (id) => {
  return request({
    url: `/admin/categories/${id}`,
    method: 'DELETE',
  })
}
