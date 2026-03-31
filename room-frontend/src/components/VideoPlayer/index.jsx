import { useEffect, useRef, useState } from 'react'
import ReactPlayer from 'react-player'
import { Toast } from 'antd-mobile'
import { RightOutline, AudioFill, CloseOutline, SoundOutline, SoundMuteOutline } from 'antd-mobile-icons'
import { nextSong, replaySong } from '../../api/play'
import useRoomStore from '../../store/roomStore'
import './index.css'

// 获取媒体流URL
const getStreamUrl = (songId) => `/api/media/stream/${songId}`

// 获取封面URL
const getCoverUrl = (songId) => `/api/media/cover/${songId}`

// 获取保存的音量
const getSavedVolume = () => {
  const savedVolume = parseFloat(localStorage.getItem('ktv_volume') || '0.7')
  return isNaN(savedVolume) ? 0.7 : Math.max(0, Math.min(1, savedVolume))
}

// 获取保存的静音状态
const getSavedMuted = () => localStorage.getItem('ktv_muted') === 'true'

export default function VideoPlayer({ playInfo, onClose }) {
  const orderId = useRoomStore((s) => s.orderId)
  const playerRef = useRef(null)
  const [playing, setPlaying] = useState(true)
  const [operating, setOperating] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const [isMuted, setIsMuted] = useState(getSavedMuted)
  const [volume, setVolume] = useState(getSavedVolume)
  const controlsTimerRef = useRef(null)
  // 使用 ref 跟踪重唱定时器，避免内存泄漏
  const replayTimerRef = useRef(null)
  // F-S1修复：使用 ref 跟踪 isMuted，避免 onVolumeChange 闭包旧值
  const isMutedRef = useRef(isMuted)
  isMutedRef.current = isMuted
  // 记录当前播放的歌曲ID，用于检测切歌
  const currentSongIdRef = useRef(playInfo?.songId)

  // 监听歌曲变化，如果切歌了就关闭视频播放器
  useEffect(() => {
    if (playInfo?.songId && currentSongIdRef.current && currentSongIdRef.current !== playInfo.songId) {
      // 歌曲已切换，关闭视频
      if (onClose) onClose()
      return
    }
    currentSongIdRef.current = playInfo?.songId

    if (playInfo) {
      setPlaying(true)
      // 3秒后隐藏控制栏
      resetControlsTimer()
    }

    return () => {
      if (controlsTimerRef.current) {
        clearTimeout(controlsTimerRef.current)
      }
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
      }
    }
  }, [playInfo?.songId])

  // 重置控制栏显示计时器
  const resetControlsTimer = () => {
    if (controlsTimerRef.current) {
      clearTimeout(controlsTimerRef.current)
    }
    setShowControls(true)
    controlsTimerRef.current = setTimeout(() => {
      setShowControls(false)
    }, 3000)
  }

  // 处理视频播放结束
  const handleEnd = async () => {
    try {
      await nextSong(orderId)
      Toast.show({ content: '已切歌', icon: 'success' })
      if (onClose) onClose()
    } catch { /* handled */ }
  }

  // 切歌
  const handleNext = async () => {
    if (operating) return
    setOperating(true)
    try {
      await nextSong(orderId)
      Toast.show({ content: '已切歌', icon: 'success' })
      if (onClose) onClose()
    } catch { /* handled */ }
    finally { setOperating(false) }
  }

  // 重唱
  const handleReplay = async () => {
    if (operating || !playerRef.current) return
    setOperating(true)
    try {
      await replaySong(orderId)
      Toast.show({ content: '重唱中', icon: 'success' })
      // 清理之前的重唱定时器
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
      }
      // 等待短暂延迟确保视频已重新加载，然后seek到开头
      replayTimerRef.current = setTimeout(() => {
        if (playerRef.current) {
          playerRef.current.seekTo(0)
          setPlaying(true)
        }
        replayTimerRef.current = null
      }, 300)
    } catch { /* handled */ }
    finally { setOperating(false) }
  }

  // 关闭视频
  const handleClose = () => {
    if (onClose) onClose()
  }

  // 点击屏幕显示/隐藏控制栏
  const handleClick = () => {
    resetControlsTimer()
  }

  // 切换静音
  const handleToggleMute = () => {
    const newMuted = !isMuted
    setIsMuted(newMuted)
    localStorage.setItem('ktv_muted', newMuted.toString())
  }

  if (!playInfo) return null

  const streamUrl = getStreamUrl(playInfo.songId)
  const coverUrl = getCoverUrl(playInfo.songId)

  return (
    <div className="video-player-fullscreen" onClick={handleClick}>
      <ReactPlayer
        ref={playerRef}
        url={streamUrl}
        playing={playing}
        controls={false}
        volume={volume}
        muted={isMuted}
        width="100%"
        height="100%"
        onEnded={handleEnd}
        onError={() => {
          Toast.show({ content: '视频播放失败', icon: 'fail' })
        }}
        onVolumeChange={(e) => {
          const newVolume = e.target.volume
          setVolume(newVolume)
          localStorage.setItem('ktv_volume', newVolume.toString())
          // F-S1修复：使用 isMutedRef 避免闭包旧值
          if (newVolume > 0 && isMutedRef.current) {
            setIsMuted(false)
            localStorage.setItem('ktv_muted', 'false')
          }
        }}
        config={{
          file: {
            forceVideo: true,
          },
        }}
      />

      {/* 控制栏 */}
      <div className={`video-controls ${showControls ? 'visible' : 'hidden'}`}>
        {/* 顶部：歌曲信息 + 封面 */}
        <div className="video-header">
          <img
            src={coverUrl}
            alt={playInfo.songName}
            className="video-cover"
            onError={(e) => { e.target.style.display = 'none' }}
          />
          <div className="video-song-info">
            <div className="video-song-name">{playInfo.songName}</div>
            <div className="video-singer-name">{playInfo.singerName}</div>
          </div>
          <button className="video-close-btn" onClick={handleClose}>
            <CloseOutline fontSize={28} />
          </button>
        </div>

        {/* 底部：控制按钮 */}
        <div className="video-footer">
          <button
            className="video-ctrl-btn"
            onClick={handleToggleMute}
            title={isMuted ? '取消静音' : '静音'}
          >
            {isMuted ? <SoundMuteOutline fontSize={32} /> : <SoundOutline fontSize={32} />}
            <span>{isMuted ? '静音' : '音量'}</span>
          </button>
          <button
            className="video-ctrl-btn"
            onClick={handleReplay}
            disabled={operating}
            title="重唱"
          >
            <AudioFill fontSize={32} />
            <span>重唱</span>
          </button>
          <button
            className="video-ctrl-btn"
            onClick={handleNext}
            disabled={operating}
            title="切歌"
          >
            <RightOutline fontSize={32} />
            <span>切歌</span>
          </button>
        </div>
      </div>
    </div>
  )
}
