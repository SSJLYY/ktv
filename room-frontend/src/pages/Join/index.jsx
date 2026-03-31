import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Input, Button, Toast } from 'antd-mobile'
import useRoomStore from '../../store/roomStore'
import request from '../../api/request'
import './index.css'

export default function Join() {
  const navigate = useNavigate()
  const setOrderId = useRoomStore((s) => s.setOrderId)
  const [orderIdInput, setOrderIdInput] = useState('')
  const [loading, setLoading] = useState(false)

  const handleJoin = async () => {
    const id = parseInt(orderIdInput.trim(), 10)
    if (!id || isNaN(id)) {
      Toast.show({ content: '请输入正确的订单号', icon: 'fail' })
      return
    }
    setLoading(true)
    try {
      // Bug5修复：改用无需 JWT 认证的 /api/room/orders/{id} 接口
      // 原来调用 /api/admin/orders/{id} 会被 JwtInterceptor 拦截，包厢端无 Token 会一直401
      const res = await request.get(`/api/room/orders/${id}`)
      const order = res.data
      if (!order) {
        Toast.show({ content: '未找到该订单', icon: 'fail' })
        return
      }
      if (order.status !== 1) {
        Toast.show({ content: '该订单不在进行中', icon: 'fail' })
        return
      }
      setOrderId(id)
      // orderIdInput 已通过 Zustand persist 自动保存到 localStorage，无需重复存储
      Toast.show({ content: `已加入包厢 ${order.roomName}`, icon: 'success' })
      navigate('/search', { replace: true })
    } catch {
      // 错误已在拦截器中处理
    } finally {
      setLoading(false)
    }
  }

  // 快速进入（开发调试用）- 需要知道一个进行中的订单ID
  // 注意：快速加入需要手动输入一个已知的进行中的订单ID，此处默认尝试 ID=1
  const handleQuickJoin = async () => {
    try {
      // Bug5修复：不能调用需要 JWT 的 /api/admin/orders，改为提示用户手动输入
      // 开发模式下可手动填写已知订单号，这里演示查询 ID=1 的订单
      const res = await request.get('/api/room/orders/1')
      const order = res.data
      if (order && order.status === 1) {
        setOrderId(order.id)
        Toast.show({ content: `已加入包厢 ${order.roomName}`, icon: 'success' })
        navigate('/search', { replace: true })
      } else {
        Toast.show({ content: '订单ID=1不在进行中，请手动输入订单号', icon: 'fail' })
      }
    } catch {
      Toast.show({ content: '请手动输入订单号', icon: 'fail' })
    }
  }

  return (
    <div className="join-page">
      <div className="join-header">
        <h1>🎤 KTV 点歌</h1>
        <p>输入订单号加入包厢开始点歌</p>
      </div>
      <div className="join-form">
        <Input
          placeholder="请输入订单号"
          type="number"
          value={orderIdInput}
          onChange={setOrderIdInput}
          size="large"
          clearable
          style={{
            '--font-size': '18px',
            '--height': '56px',
          }}
        />
        <Button
          block
          color="primary"
          size="large"
          loading={loading}
          onClick={handleJoin}
          style={{ '--height': '56px', '--font-size': '18px' }}
        >
          加入包厢
        </Button>
        <Button
          block
          fill="outline"
          size="large"
          onClick={handleQuickJoin}
          style={{ '--height': '48px', marginTop: '12px' }}
        >
          快速加入（开发模式）
        </Button>
      </div>
    </div>
  )
}
