import axios from 'axios'
import { message } from 'antd'
import { useUserStore } from '../store/userStore'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

request.interceptors.request.use(
  (config) => {
    const token = useUserStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request interceptor error:', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      return res
    }

    const businessMessage = res.message || '请求失败'
    message.error(businessMessage)
    const businessError = new Error(businessMessage)
    businessError.response = {
      data: res,
      status: response.status,
    }
    return Promise.reject(businessError)
  },
  (error) => {
    console.error('Response error:', error)

    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        useUserStore.getState().logout()
        message.error('登录已过期，请重新登录')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else if (status === 403) {
        message.error(data?.message || '没有权限访问')
      } else if (status === 404) {
        message.error(data?.message || '请求的资源不存在')
      } else if (status === 500) {
        message.error(data?.message || '服务器错误，请稍后重试')
      } else {
        message.error(data?.message || '请求失败')
      }
    } else {
      message.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default request
