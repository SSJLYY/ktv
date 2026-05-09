import { useState, useEffect, useCallback } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { TabBar } from 'antd-mobile'
import { SearchOutline, UnorderedListOutline, PlayOutline } from 'antd-mobile-icons'
import PlayBar from '../components/PlayBar/index'
import VideoPlayer from '../components/VideoPlayer/index'
import useRoomStore from '../store/roomStore'
import { validateActiveRoomOrder } from '../api/room'
import './MainLayout.css'

const tabs = [
  { key: '/search', title: '点歌', icon: <SearchOutline /> },
  { key: '/queue', title: '已点', icon: <UnorderedListOutline /> },
]

export default function MainLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const orderId = useRoomStore((s) => s.orderId)
  const hasHydrated = useRoomStore((s) => s.hasHydrated)
  const clearOrderId = useRoomStore((s) => s.clearOrderId)
  const [videoPlayInfo, setVideoPlayInfo] = useState(null)
  const [videoVisible, setVideoVisible] = useState(false)
  const [sessionChecked, setSessionChecked] = useState(false)

  useEffect(() => {
    if (!hasHydrated) {
      return
    }

    let cancelled = false

    if (!orderId) {
      setSessionChecked(true)
      navigate('/join', { replace: true })
      return
    }

    setSessionChecked(false)

    validateActiveRoomOrder(orderId)
      .then((order) => {
        if (cancelled) {
          return
        }
        if (!order) {
          clearOrderId()
          navigate('/join', { replace: true })
          return
        }
        setSessionChecked(true)
      })
      .catch(() => {
        if (!cancelled) {
          setSessionChecked(true)
        }
      })

    return () => {
      cancelled = true
    }
  }, [hasHydrated, orderId, clearOrderId, navigate])

  const handleVideoPlay = useCallback((playInfo) => {
    if (!playInfo?.songId) {
      setVideoPlayInfo(null)
      setVideoVisible(false)
      return
    }
    setVideoPlayInfo(playInfo)
    setVideoVisible(true)
  }, [])

  const handleVideoUpdate = useCallback((playInfo) => {
    if (!playInfo?.songId) {
      setVideoPlayInfo(null)
      setVideoVisible(false)
      return
    }
    setVideoPlayInfo(playInfo)
  }, [])

  const handleVideoClose = useCallback(() => {
    setVideoVisible(false)
  }, [])

  if (!hasHydrated || !orderId || !sessionChecked) return null

  return (
    <div className="main-layout">
      <div className="main-content">
        <Outlet />
      </div>
      <PlayBar onVideoPlay={handleVideoPlay} onVideoUpdate={handleVideoUpdate} />
      {videoPlayInfo && !videoVisible && (
        <button
          className="video-reopen-btn"
          onClick={() => setVideoVisible(true)}
          type="button"
        >
          <PlayOutline fontSize={18} />
          <span>继续视频</span>
        </button>
      )}
      {videoPlayInfo && (
        <VideoPlayer
          playInfo={videoPlayInfo}
          visible={videoVisible}
          onClose={handleVideoClose}
        />
      )}
      <div className="tab-bar-wrapper">
        <TabBar
          activeKey={location.pathname}
          onChange={(key) => navigate(key)}
        >
          {tabs.map((tab) => (
            <TabBar.Item key={tab.key} icon={tab.icon} title={tab.title} />
          ))}
        </TabBar>
      </div>
    </div>
  )
}
