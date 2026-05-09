import { useEffect, useRef, useState } from 'react'
import ReactPlayer from 'react-player'
import { Toast } from 'antd-mobile'
import {
  RightOutline,
  AudioFill,
  CloseOutline,
  SoundOutline,
  SoundMuteOutline,
} from 'antd-mobile-icons'
import { nextSong, replaySong } from '../../api/play'
import useRoomStore from '../../store/roomStore'
import './index.css'

const getStreamUrl = (songId) => `/api/media/stream/${songId}`
const getCoverUrl = (songId) => `/api/media/cover/${songId}`

const getSavedVolume = () => {
  const savedVolume = parseFloat(localStorage.getItem('ktv_volume') || '0.7')
  return Number.isNaN(savedVolume) ? 0.7 : Math.max(0, Math.min(1, savedVolume))
}

const getSavedMuted = () => localStorage.getItem('ktv_muted') === 'true'

export default function VideoPlayer({ playInfo, visible, onClose }) {
  const orderId = useRoomStore((s) => s.orderId)
  const bumpQueueVersion = useRoomStore((s) => s.bumpQueueVersion)
  const playerRef = useRef(null)
  const controlsTimerRef = useRef(null)
  const replayTimerRef = useRef(null)
  const isMutedRef = useRef(getSavedMuted())

  const [playing, setPlaying] = useState(playInfo?.playStatus !== 'PAUSED')
  const [operating, setOperating] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const [isMuted, setIsMuted] = useState(getSavedMuted)
  const [volume, setVolume] = useState(getSavedVolume)

  isMutedRef.current = isMuted

  const resetControlsTimer = () => {
    if (controlsTimerRef.current) {
      clearTimeout(controlsTimerRef.current)
    }
    setShowControls(true)
    controlsTimerRef.current = setTimeout(() => {
      setShowControls(false)
      controlsTimerRef.current = null
    }, 3000)
  }

  useEffect(() => {
    if (!playInfo?.songId) {
      return
    }

    setPlaying(playInfo.playStatus !== 'PAUSED')
    resetControlsTimer()

    return () => {
      if (controlsTimerRef.current) {
        clearTimeout(controlsTimerRef.current)
        controlsTimerRef.current = null
      }
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
        replayTimerRef.current = null
      }
    }
  }, [playInfo?.songId, playInfo?.playStatus])

  useEffect(() => {
    if (!playInfo) {
      return
    }
    setPlaying(playInfo.playStatus !== 'PAUSED')
  }, [playInfo?.playStatus, playInfo])

  const handleEnd = async () => {
    if (!orderId) {
      return
    }
    try {
      await nextSong(orderId)
      bumpQueueVersion()
      Toast.show({ content: '已切歌', icon: 'success' })
    } catch {
      // handled by request interceptor
    }
  }

  const handleNext = async () => {
    if (operating || !orderId) {
      return
    }
    setOperating(true)
    try {
      await nextSong(orderId)
      bumpQueueVersion()
      Toast.show({ content: '已切歌', icon: 'success' })
    } catch {
      // handled by request interceptor
    } finally {
      setOperating(false)
    }
  }

  const handleReplay = async () => {
    if (operating || !playerRef.current || !orderId) {
      return
    }
    setOperating(true)
    try {
      await replaySong(orderId)
      bumpQueueVersion()
      Toast.show({ content: '已重唱', icon: 'success' })
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
      }
      replayTimerRef.current = setTimeout(() => {
        if (playerRef.current) {
          playerRef.current.seekTo(0)
          setPlaying(true)
        }
        replayTimerRef.current = null
      }, 300)
    } catch {
      // handled by request interceptor
    } finally {
      setOperating(false)
    }
  }

  const handleToggleMute = () => {
    const nextMuted = !isMuted
    setIsMuted(nextMuted)
    localStorage.setItem('ktv_muted', String(nextMuted))
  }

  if (!playInfo) {
    return null
  }

  return (
    <div
      className={`video-player-fullscreen ${visible ? 'visible' : 'hidden'}`}
      onClick={visible ? resetControlsTimer : undefined}
    >
      <ReactPlayer
        ref={playerRef}
        url={getStreamUrl(playInfo.songId)}
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
        onVolumeChange={(event) => {
          const nextVolume = event.target.volume
          setVolume(nextVolume)
          localStorage.setItem('ktv_volume', String(nextVolume))
          if (nextVolume > 0 && isMutedRef.current) {
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

      <div
        className={`video-controls ${visible && showControls ? 'visible' : 'hidden'}`}
      >
        <div className="video-header">
          <img
            src={getCoverUrl(playInfo.songId)}
            alt={playInfo.songName}
            className="video-cover"
            onError={(event) => {
              event.target.style.display = 'none'
            }}
          />
          <div className="video-song-info">
            <div className="video-song-name">{playInfo.songName}</div>
            <div className="video-singer-name">{playInfo.singerName}</div>
          </div>
          <button className="video-close-btn" onClick={onClose} type="button">
            <CloseOutline fontSize={28} />
          </button>
        </div>

        <div className="video-footer">
          <button
            className="video-ctrl-btn"
            onClick={handleToggleMute}
            title={isMuted ? '取消静音' : '静音'}
            type="button"
          >
            {isMuted ? <SoundMuteOutline fontSize={32} /> : <SoundOutline fontSize={32} />}
            <span>{isMuted ? '静音' : '音量'}</span>
          </button>
          <button
            className="video-ctrl-btn"
            onClick={handleReplay}
            disabled={operating}
            title="重唱"
            type="button"
          >
            <AudioFill fontSize={32} />
            <span>重唱</span>
          </button>
          <button
            className="video-ctrl-btn"
            onClick={handleNext}
            disabled={operating}
            title="切歌"
            type="button"
          >
            <RightOutline fontSize={32} />
            <span>切歌</span>
          </button>
        </div>
      </div>
    </div>
  )
}
