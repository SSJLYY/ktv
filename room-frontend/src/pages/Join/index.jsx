import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, Toast } from 'antd-mobile'
import useRoomStore from '../../store/roomStore'
import { getActiveRoomOrderByRoomId, validateActiveRoomOrder } from '../../api/room'
import './index.css'

export default function Join() {
  const isDev = import.meta.env.DEV
  const navigate = useNavigate()
  const orderId = useRoomStore((state) => state.orderId)
  const roomId = useRoomStore((state) => state.roomId)
  const hasHydrated = useRoomStore((state) => state.hasHydrated)
  const setSession = useRoomStore((state) => state.setSession)
  const clearSession = useRoomStore((state) => state.clearSession)
  const [orderIdInput, setOrderIdInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [checkingStoredOrder, setCheckingStoredOrder] = useState(false)

  useEffect(() => {
    if (!hasHydrated || (!orderId && !roomId)) {
      return
    }

    let cancelled = false
    setCheckingStoredOrder(true)

    const restoreSession = async () => {
      try {
        if (orderId) {
          const activeOrder = await validateActiveRoomOrder(orderId)
          if (cancelled) {
            return
          }
          if (activeOrder) {
            setSession({
              orderId: activeOrder.id ?? orderId,
              roomId: activeOrder.roomId ?? roomId ?? null,
            })
            navigate('/search', { replace: true })
            return
          }
        }

        if (roomId) {
          const activeOrder = await getActiveRoomOrderByRoomId(roomId)
          if (cancelled) {
            return
          }
          if (activeOrder) {
            setSession({
              orderId: activeOrder.id,
              roomId: activeOrder.roomId ?? roomId,
            })
            navigate('/search', { replace: true })
            return
          }
        }

        clearSession()
      } catch {
        // handled by interceptor
      } finally {
        if (!cancelled) {
          setCheckingStoredOrder(false)
        }
      }
    }

    restoreSession()

    return () => {
      cancelled = true
    }
  }, [hasHydrated, orderId, roomId, setSession, clearSession, navigate])

  const handleJoin = async () => {
    const parsedOrderId = Number.parseInt(orderIdInput.trim(), 10)
    if (!parsedOrderId || Number.isNaN(parsedOrderId) || parsedOrderId <= 0) {
      Toast.show({ content: '请输入有效的订单号', icon: 'fail' })
      return
    }

    setLoading(true)
    try {
      const order = await validateActiveRoomOrder(parsedOrderId)
      if (!order) {
        Toast.show({ content: '该订单当前不在进行中', icon: 'fail' })
        return
      }

      setSession({
        orderId: order.id ?? parsedOrderId,
        roomId: order.roomId ?? null,
      })
      Toast.show({
        content: `已加入包厢：${order.roomName || parsedOrderId}`,
        icon: 'success',
      })
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
        <h1>KTV 点歌</h1>
        <p>输入订单号加入包厢，开始点歌。</p>
      </div>

      <div className="join-form">
        {checkingStoredOrder ? (
          <div style={{ marginBottom: '12px', color: '#999', textAlign: 'center' }}>
            正在验证当前包厢状态...
          </div>
        ) : null}

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
          loading={loading || checkingStoredOrder}
          disabled={checkingStoredOrder}
          onClick={handleJoin}
          style={{ '--height': '56px', '--font-size': '18px' }}
        >
          加入包厢
        </Button>

        {isDev ? (
          <div style={{ marginTop: '12px', color: '#999', fontSize: '13px', textAlign: 'center' }}>
            开发环境也需要输入真实订单号，避免误连到错误包厢。
          </div>
        ) : null}
      </div>
    </div>
  )
}
