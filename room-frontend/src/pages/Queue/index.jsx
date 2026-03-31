import { useState, useEffect, useCallback, useRef } from 'react'
import { Tabs, Toast, DotLoading, Dialog, SwipeAction } from 'antd-mobile'
import { UpOutline, CloseCircleOutline } from 'antd-mobile-icons'
import { getQueueList, getPlayedList, topSong, removeSong } from '../../api/queue'
import useRoomStore from '../../store/roomStore'
import './index.css'

const tabItems = [
  { key: 'queue', title: '待唱' },
  { key: 'played', title: '已唱' },
]

export default function Queue() {
  const orderId = useRoomStore((s) => s.orderId)
  const queueVersion = useRoomStore((s) => s.queueVersion)
  const [activeTab, setActiveTab] = useState('queue')

  return (
    <div className="queue-page">
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        style={{
          '--title-font-size': '16px',
          '--active-line-height': '3px',
          '--active-line-color': '#1677ff',
        }}
      >
        {tabItems.map((tab) => (
          <Tabs.Tab key={tab.key} title={tab.title} />
        ))}
      </Tabs>
      <div className="queue-content">
        {activeTab === 'queue' && (
          <QueueList orderId={orderId} queueVersion={queueVersion} />
        )}
        {activeTab === 'played' && (
          <PlayedList orderId={orderId} />
        )}
      </div>
    </div>
  )
}

// ==================== 待唱列表 ====================
function QueueList({ orderId, queueVersion }) {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [operatingId, setOperatingId] = useState(null)
  const timerRef = useRef(null)

  const fetchQueue = useCallback(async () => {
    try {
      const res = await getQueueList(orderId, 1, 100)
      setList(res.data?.records || [])
    } catch { /* handled */ }
  }, [orderId])

  // 初始加载 + 每10秒轮询
  useEffect(() => {
    let isMounted = true

    // 先清理旧的定时器
    if (timerRef.current) {
      clearInterval(timerRef.current)
    }

    setLoading(true)
    fetchQueue().finally(() => {
      if (isMounted) setLoading(false)
    })

    timerRef.current = setInterval(() => {
      if (isMounted) fetchQueue()
    }, 10000)

    return () => {
      isMounted = false
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [fetchQueue, queueVersion])

  const handleTop = async (item) => {
    if (operatingId) return
    // F-N2修复：避免变量名 confirm 覆盖全局 Dialog.confirm，改为 confirmed
    const confirmed = await Dialog.confirm({
      content: `确定将「${item.songName}」置顶为下一首？`,
    })
    if (!confirmed) return

    setOperatingId(item.id)
    try {
      await topSong(orderId, item.id)
      Toast.show({ content: '已置顶', icon: 'success' })
      fetchQueue()
    } catch { /* handled */ }
    finally { setOperatingId(null) }
  }

  const handleRemove = async (item) => {
    if (operatingId) return
    // F-N2修复：避免变量名 confirm 覆盖全局 Dialog.confirm，改为 confirmed
    const confirmed = await Dialog.confirm({
      content: `确定取消「${item.songName}」？`,
    })
    if (!confirmed) return

    setOperatingId(item.id)
    try {
      await removeSong(orderId, item.id)
      Toast.show({ content: '已取消', icon: 'success' })
      fetchQueue()
    } catch { /* handled */ }
    finally { setOperatingId(null) }
  }

  if (loading) return <div className="loading-wrapper"><DotLoading /> 加载中...</div>
  if (list.length === 0) return <div className="empty-text">还没有点歌，快去搜索吧 🎤</div>

  return (
    <div className="queue-list">
      {list.map((item, idx) => (
        <SwipeAction
          key={item.id}
          rightActions={[
            {
              key: 'top',
              text: '置顶',
              color: '#1677ff',
              onClick: () => handleTop(item),
            },
            {
              key: 'delete',
              text: '取消',
              color: '#ff3141',
              onClick: () => handleRemove(item),
            },
          ]}
          stopPropagation
        >
          <div className={`queue-item ${idx === 0 ? 'first-item' : ''}`}>
            <div className="queue-index">
              {idx === 0 ? (
                <span className="now-playing-badge">即将播放</span>
              ) : (
                <span className="queue-num">{idx + 1}</span>
              )}
            </div>
            <div className="queue-info">
              <div className="queue-song-name">{item.songName}</div>
              <div className="queue-singer-name">{item.singerName}</div>
            </div>
            <div className="queue-actions">
              <button
                className="action-btn top-btn"
                onClick={() => handleTop(item)}
                disabled={operatingId === item.id || idx === 0}
                title="置顶"
              >
                <UpOutline fontSize={20} />
              </button>
              <button
                className="action-btn del-btn"
                onClick={() => handleRemove(item)}
                disabled={operatingId === item.id}
                title="取消"
              >
                <CloseCircleOutline fontSize={20} />
              </button>
            </div>
          </div>
        </SwipeAction>
      ))}
    </div>
  )
}

// ==================== 已唱列表 ====================
function PlayedList({ orderId }) {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let isMounted = true
    setLoading(true)
    getPlayedList(orderId, 1, 100)
      .then((res) => {
        if (isMounted) setList(res.data?.records || [])
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false)
      })
    return () => { isMounted = false }
  }, [orderId])

  if (loading) return <div className="loading-wrapper"><DotLoading /> 加载中...</div>
  if (list.length === 0) return <div className="empty-text">暂无已唱歌曲</div>

  return (
    <div className="played-list">
      {list.map((item, idx) => (
        <div key={item.id} className="played-item">
          <div className="played-index">{idx + 1}</div>
          <div className="played-info">
            <div className="played-song-name">{item.songName}</div>
            <div className="played-meta">
              {item.singerName}
              <span className="played-status">
                {item.status === 2 ? '已播放' : '已跳过'}
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
