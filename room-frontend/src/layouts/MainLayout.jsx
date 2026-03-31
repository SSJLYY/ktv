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
  // 视频播放状态
  const [videoPlayInfo, setVideoPlayInfo] = useState(null)

  // 未绑定包厢时跳转到加入页面
  useEffect(() => {
    if (!orderId) {
      navigate('/join', { replace: true })
    }
  }, [orderId, navigate])

  // 处理视频播放 - 使用 useCallback 避免子组件重渲染
  const handleVideoPlay = useCallback((playInfo) => {
    setVideoPlayInfo(playInfo)
  }, [])

  // 关闭视频播放器 - 使用 useCallback 避免子组件重渲染
  const handleVideoClose = useCallback(() => {
    setVideoPlayInfo(null)
  }, [])

  if (!orderId) return null

  return (
    <div className="main-layout">
      <div className="main-content">
        <Outlet />
      </div>
      <PlayBar onVideoPlay={handleVideoPlay} />
      {/* 视频播放器 */}
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
