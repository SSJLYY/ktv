import axios from 'axios'
import { Toast } from 'antd-mobile'
import useRoomStore from '../store/roomStore'

const request = axios.create({
  baseURL: '',
  timeout: 10000,
})

function shouldResetRoomSession(message) {
  if (!message) {
    return false
  }
  return message.includes('订单不存在') || message.includes('不在进行中')
}

function resetRoomSession() {
  useRoomStore.getState().clearOrderId()
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ktv_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      return res
    }

    if (shouldResetRoomSession(res.message)) {
      resetRoomSession()
    }

    Toast.show({ icon: 'fail', content: res.message || '请求失败' })
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        Toast.show({ icon: 'fail', content: '登录已过期，请重新登录' })
        localStorage.removeItem('ktv_token')
      } else {
        if (shouldResetRoomSession(data?.message)) {
          resetRoomSession()
        }
        Toast.show({ icon: 'fail', content: data?.message || '网络错误' })
      }
    } else {
      Toast.show({ icon: 'fail', content: '网络连接失败' })
    }

    return Promise.reject(error)
  }
)

export default request
