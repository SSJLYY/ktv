import { useState, useEffect, useCallback } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { TabBar } from 'antd-mobile'
import { SearchOutline, UnorderedListOutline } from 'antd-mobile-icons'
import PlayBar from '../components/PlayBar/index'
import VideoPlayer from '../components/VideoPlayer/index'
import useRoomStore from '../store/roomStore'
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
  const [videoPlayInfo, setVideoPlayInfo] = useState(null)

  useEffect(() => {
    if (!hasHydrated) {
      return
    }
    if (!orderId) {
      navigate('/join', { replace: true })
    }
  }, [hasHydrated, orderId, navigate])

  const handleVideoPlay = useCallback((playInfo) => {
    setVideoPlayInfo(playInfo)
  }, [])

  const handleVideoClose = useCallback(() => {
    setVideoPlayInfo(null)
  }, [])

  if (!hasHydrated || !orderId) return null

  return (
    <div className="main-layout">
      <div className="main-content">
        <Outlet />
      </div>
      <PlayBar onVideoPlay={handleVideoPlay} />
      {videoPlayInfo && (
        <VideoPlayer playInfo={videoPlayInfo} onClose={handleVideoClose} />
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
