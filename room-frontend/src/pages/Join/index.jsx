import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Input, Button, Toast } from 'antd-mobile'
import useRoomStore from '../../store/roomStore'
import request from '../../api/request'
import './index.css'

export default function Join() {
  const isDev = import.meta.env.DEV
  const navigate = useNavigate()
  const orderId = useRoomStore((s) => s.orderId)
  const hasHydrated = useRoomStore((s) => s.hasHydrated)
  const setOrderId = useRoomStore((s) => s.setOrderId)
  const [orderIdInput, setOrderIdInput] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (hasHydrated && orderId) {
      navigate('/search', { replace: true })
    }
  }, [hasHydrated, orderId, navigate])

  const handleJoin = async () => {
    const parsedOrderId = parseInt(orderIdInput.trim(), 10)
    if (!parsedOrderId || Number.isNaN(parsedOrderId) || parsedOrderId <= 0) {
      Toast.show({ content: '请输入有效的订单号', icon: 'fail' })
      return
    }

    setLoading(true)
    try {
      const res = await request.get(`/api/room/orders/${parsedOrderId}`)
      const order = res.data
      if (!order) {
        Toast.show({ content: '未找到该订单', icon: 'fail' })
        return
      }
      if (order.status !== 1) {
        Toast.show({ content: '该订单不在进行中', icon: 'fail' })
        return
      }

      setOrderId(parsedOrderId)
      Toast.show({ content: `已加入包厢 ${order.roomName}`, icon: 'success' })
      navigate('/search', { replace: true })
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="join-page">
      <div className="join-header">
        <h1>🎤 KTV 点歌</h1>
        <p>输入订单号加入包厢，开始点歌</p>
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
        {isDev ? (
          <div style={{ marginTop: '12px', color: '#999', fontSize: '13px', textAlign: 'center' }}>
            开发模式下也需要输入真实订单号，避免误连到错误包厢。
          </div>
        ) : null}
      </div>
    </div>
  )
}
