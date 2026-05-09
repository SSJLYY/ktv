import { useCallback, useEffect, useRef, useState } from 'react'
import { Dialog, DotLoading, SwipeAction, Tabs, Toast } from 'antd-mobile'
import { CloseCircleOutline, UpOutline } from 'antd-mobile-icons'
import { getPlayedList, getQueueList, removeSong, topSong } from '../../api/queue'
import useRoomStore from '../../store/roomStore'
import './index.css'

const tabItems = [
  { key: 'queue', title: '待唱' },
  { key: 'played', title: '已唱' },
]

export default function Queue() {
  const orderId = useRoomStore((state) => state.orderId)
  const queueVersion = useRoomStore((state) => state.queueVersion)
  const bumpQueueVersion = useRoomStore((state) => state.bumpQueueVersion)
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
          <QueueList
            orderId={orderId}
            queueVersion={queueVersion}
            bumpQueueVersion={bumpQueueVersion}
          />
        )}
        {activeTab === 'played' && <PlayedList orderId={orderId} />}
      </div>
    </div>
  )
}

function QueueList({ orderId, queueVersion, bumpQueueVersion }) {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [operatingId, setOperatingId] = useState(null)
  const timerRef = useRef(null)

  const fetchQueue = useCallback(async () => {
    if (!orderId) {
      setList([])
      return
    }

    try {
      const res = await getQueueList(orderId, 1, 100)
      setList(res.data?.records || [])
    } catch {
      // handled by interceptor
    }
  }, [orderId])

  useEffect(() => {
    let isMounted = true

    if (timerRef.current) {
      clearInterval(timerRef.current)
    }

    setLoading(true)
    fetchQueue().finally(() => {
      if (isMounted) {
        setLoading(false)
      }
    })

    if (orderId) {
      timerRef.current = setInterval(() => {
        if (isMounted) {
          fetchQueue()
        }
      }, 10000)
    }

    return () => {
      isMounted = false
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [fetchQueue, orderId, queueVersion])

  const handleTop = async (item) => {
    if (operatingId || !orderId || list[0]?.id === item.id) {
      return
    }

    const confirmed = await Dialog.confirm({
      content: `确定将“${item.songName}”置顶为下一首吗？`,
    })
    if (!confirmed) {
      return
    }

    setOperatingId(item.id)
    try {
      await topSong(orderId, item.id)
      Toast.show({ content: '已置顶', icon: 'success' })
      bumpQueueVersion()
      await fetchQueue()
    } catch {
      // handled by interceptor
    } finally {
      setOperatingId(null)
    }
  }

  const handleRemove = async (item) => {
    if (operatingId || !orderId) {
      return
    }

    const confirmed = await Dialog.confirm({
      content: `确定取消“${item.songName}”吗？`,
    })
    if (!confirmed) {
      return
    }

    setOperatingId(item.id)
    try {
      await removeSong(orderId, item.id)
      Toast.show({ content: '已取消', icon: 'success' })
      bumpQueueVersion()
      await fetchQueue()
    } catch {
      // handled by interceptor
    } finally {
      setOperatingId(null)
    }
  }

  if (loading) {
    return (
      <div className="loading-wrapper">
        <DotLoading /> 加载中...
      </div>
    )
  }
  if (!orderId) {
    return <div className="empty-text">请先加入包厢</div>
  }
  if (list.length === 0) {
    return <div className="empty-text">还没有点歌，快去搜索吧</div>
  }

  return (
    <div className="queue-list">
      {list.map((item, index) => {
        const isFirst = index === 0
        const rightActions = [
          !isFirst && {
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
        ].filter(Boolean)

        return (
          <SwipeAction
            key={item.id}
            rightActions={rightActions}
            stopPropagation
          >
            <div className={`queue-item ${isFirst ? 'first-item' : ''}`}>
              <div className="queue-index">
                {isFirst ? (
                  <span className="now-playing-badge">即将播放</span>
                ) : (
                  <span className="queue-num">{index + 1}</span>
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
                  disabled={operatingId === item.id || isFirst}
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
        )
      })}
    </div>
  )
}

function PlayedList({ orderId }) {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const timerRef = useRef(null)
  const initialLoadTimerRef = useRef(null)

  const fetchPlayedList = useCallback(async () => {
    if (!orderId) {
      setList([])
      return
    }
    const res = await getPlayedList(orderId, 1, 100)
    setList(res.data?.records || [])
  }, [orderId])

  useEffect(() => {
    let isMounted = true

    if (timerRef.current) {
      clearInterval(timerRef.current)
    }
    if (initialLoadTimerRef.current) {
      clearTimeout(initialLoadTimerRef.current)
    }

    initialLoadTimerRef.current = setTimeout(() => {
      fetchPlayedList()
        .catch(() => {})
        .finally(() => {
          if (isMounted) {
            setLoading(false)
          }
        })
    }, 0)

    if (orderId) {
      timerRef.current = setInterval(() => {
        if (isMounted) {
          fetchPlayedList().catch(() => {})
        }
      }, 5000)
    }

    return () => {
      isMounted = false
      if (initialLoadTimerRef.current) {
        clearTimeout(initialLoadTimerRef.current)
        initialLoadTimerRef.current = null
      }
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [fetchPlayedList, orderId])

  if (loading) {
    return (
      <div className="loading-wrapper">
        <DotLoading /> 加载中...
      </div>
    )
  }
  if (!orderId) {
    return <div className="empty-text">请先加入包厢</div>
  }
  if (list.length === 0) {
    return <div className="empty-text">暂无已唱歌曲</div>
  }

  return (
    <div className="played-list">
      {list.map((item, index) => (
        <div key={item.id} className="played-item">
          <div className="played-index">{index + 1}</div>
          <div className="played-info">
            <div className="played-song-name">{item.songName}</div>
            <div className="played-meta">
              {item.singerName}
              <span className="played-status">{item.status === 2 ? '已播放' : '已跳过'}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
