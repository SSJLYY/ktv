import axios from 'axios'
import { message } from 'antd'
import { useUserStore } from '../store/userStore'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

// 请求拦截器 - 自动附加Token
request.interceptors.request.use(
  (config) => {
    // 从 Zustand store 读取 token（persist 已同步到 localStorage，直接读 store 即可）
    const token = useUserStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理401和统一错误提示
request.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 后端统一返回格式: { code, message, data }
    if (res.code === 200 || res.code === 0) {
      return res
    } else {
      // 业务错误
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status } = error.response
      
      if (status === 401) {
        // Bug修复：401时同时清除 Zustand store 和 localStorage（通过 store.logout() 统一处理）
        useUserStore.getState().logout()
        message.error('登录已过期,请重新登录')
        
        // 跳转到登录页
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else if (status === 403) {
        message.error('没有权限访问')
      } else if (status === 404) {
        message.error('请求的资源不存在')
      } else if (status === 500) {
        message.error('服务器错误,请稍后重试')
      } else {
        message.error(error.response.data?.message || '请求失败')
      }
    } else {
      message.error('网络错误,请检查网络连接')
    }
    
    return Promise.reject(error)
  }
)

export default request
