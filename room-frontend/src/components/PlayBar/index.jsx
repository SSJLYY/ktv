import { useEffect, useRef, useState, useCallback } from 'react'
import { Toast } from 'antd-mobile'
import {
  PlayOutline,
  StopOutline,
  RightOutline,
  AudioFill,
} from 'antd-mobile-icons'
import APlayer from 'aplayer'
import 'aplayer/dist/APlayer.min.css'
import {
  getCurrentPlayStatus,
  nextSong,
  replaySong,
  pausePlay,
  resumePlay,
  getMediaStreamUrl,
  getCoverUrl,
  isVideoFile,
} from '../../api/play'
import useRoomStore from '../../store/roomStore'
import './index.css'

export default function PlayBar({ onVideoPlay }) {
  const orderId = useRoomStore((s) => s.orderId)
  const queueVersion = useRoomStore((s) => s.queueVersion)
  const [playInfo, setPlayInfo] = useState(null)
  const [operating, setOperating] = useState(false)
  const [isVideoMode, setIsVideoMode] = useState(false)
  const playerRef = useRef(null)
  const playerContainerRef = useRef(null)
  const timerRef = useRef(null)
  const retryCountRef = useRef(0)
  const retryTimerRef = useRef(null)
  const callbacksRef = useRef({})
  const destroyTimerRef = useRef(null)
  const replayTimerRef = useRef(null)
  const playInfoRef = useRef(playInfo)
  playInfoRef.current = playInfo

  const MAX_RETRY_COUNT = 3
  const RETRY_DELAY_MS = 2000

  const fetchPlayStatus = useCallback(async (isRetry = false) => {
    if (!orderId) return
    try {
      const res = await getCurrentPlayStatus(orderId)
      setPlayInfo(res.data)
      retryCountRef.current = 0
    } catch (err) {
      console.warn('获取播放状态失败', err)
      if (!isRetry && retryCountRef.current < MAX_RETRY_COUNT) {
        retryCountRef.current++
        console.log(`将在 ${RETRY_DELAY_MS / 1000}s 后重试获取播放状态（第${retryCountRef.current}次）`)
        if (retryTimerRef.current) clearTimeout(retryTimerRef.current)
        retryTimerRef.current = setTimeout(() => {
          fetchPlayStatus(true)
        }, RETRY_DELAY_MS)
      }
    }
  }, [orderId])

  useEffect(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
    }

    fetchPlayStatus()
    timerRef.current = setInterval(fetchPlayStatus, 5000)

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
      if (retryTimerRef.current) {
        clearTimeout(retryTimerRef.current)
        retryTimerRef.current = null
      }
    }
  }, [fetchPlayStatus])

  useEffect(() => {
    if (queueVersion > 0) {
      fetchPlayStatus()
    }
  }, [queueVersion, fetchPlayStatus])

  useEffect(() => {
    const currentPlayInfo = playInfoRef.current
    if (!currentPlayInfo?.songId || !playerContainerRef.current) return

    if (isVideoFile(currentPlayInfo.filePath)) {
      setIsVideoMode(true)
      if (playerRef.current) {
        try {
          playerRef.current.pause()
        } catch (e) {
          console.warn('暂停播放器失败', e)
        }
        if (destroyTimerRef.current) {
          clearTimeout(destroyTimerRef.current)
        }
        destroyTimerRef.current = setTimeout(() => {
          if (playerRef.current) {
            try {
              playerRef.current.destroy()
            } catch (e) {
              console.warn('销毁播放器失败', e)
            }
            playerRef.current = null
          }
          destroyTimerRef.current = null
        }, 250)
      }
      if (onVideoPlay) {
        onVideoPlay(currentPlayInfo)
      }
      return
    }

    setIsVideoMode(false)

    const streamUrl = getMediaStreamUrl(currentPlayInfo.songId)
    const coverUrl = getCoverUrl(currentPlayInfo.songId)

    if (playerRef.current) {
      try {
        playerRef.current.destroy()
      } catch (e) {
        console.warn('销毁播放器失败', e)
      }
    }

    const isMuted = localStorage.getItem('ktv_muted') === 'true'
    const ap = new APlayer({
      container: playerContainerRef.current,
      mini: true,
      autoplay: currentPlayInfo.playStatus === 'PLAYING',
      mutex: true,
      loop: false,
      volume: (() => {
        const savedVolume = parseFloat(localStorage.getItem('ktv_volume') || '0.7')
        return Number.isNaN(savedVolume) ? 0.7 : Math.max(0, Math.min(1, savedVolume))
      })(),
      mute: isMuted,
      audio: [{
        name: currentPlayInfo.songName || '未知歌曲',
        artist: currentPlayInfo.singerName || '未知歌手',
        url: streamUrl,
        cover: coverUrl,
        lrc: '',
      }],
    })

    ap.on('error', () => {
      console.error('APlayer 播放错误')
      Toast.show({ content: '音频播放失败，请检查文件', icon: 'fail' })
    })

    callbacksRef.current = {
      onEnded: async () => {
        try {
          await nextSong(orderId)
          fetchPlayStatus()
        } catch {
          // handled
        }
      },
      onVolumeChange: (newVolume) => {
        localStorage.setItem('ktv_volume', newVolume.toString())
      },
      onMute: () => {
        localStorage.setItem('ktv_muted', 'true')
      },
      onUnmute: () => {
        localStorage.setItem('ktv_muted', 'false')
      },
    }

    ap.on('ended', callbacksRef.current.onEnded)
    ap.on('volumechange', callbacksRef.current.onVolumeChange)
    ap.on('mute', callbacksRef.current.onMute)
    ap.on('unmute', callbacksRef.current.onUnmute)

    playerRef.current = ap

    return () => {
      if (destroyTimerRef.current) {
        clearTimeout(destroyTimerRef.current)
        destroyTimerRef.current = null
      }
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
        replayTimerRef.current = null
      }
      if (playerRef.current) {
        try {
          playerRef.current.destroy()
        } catch (e) {
          console.warn('销毁播放器失败', e)
        }
        playerRef.current = null
      }
    }
  }, [playInfo?.songId, playInfo?.filePath, onVideoPlay, orderId, fetchPlayStatus])

  useEffect(() => {
    if (!playerRef.current || !playInfoRef.current) return

    const currentPlayInfo = playInfoRef.current
    if (currentPlayInfo.playStatus === 'PLAYING') {
      playerRef.current.play().catch(() => {})
    } else if (currentPlayInfo.playStatus === 'PAUSED') {
      playerRef.current.pause()
    }
  }, [playInfo?.playStatus])

  const handleTogglePause = async () => {
    if (operating || !playInfo) return
    setOperating(true)
    try {
      if (playInfo.playStatus === 'PLAYING') {
        await pausePlay(orderId)
        Toast.show({ content: '已暂停', icon: 'success' })
      } else if (playInfo.playStatus === 'PAUSED') {
        await resumePlay(orderId)
        Toast.show({ content: '继续播放', icon: 'success' })
      }
      fetchPlayStatus()
    } catch {
      // handled
    } finally {
      setOperating(false)
    }
  }

  const handleNext = async () => {
    if (operating) return
    setOperating(true)
    try {
      await nextSong(orderId)
      Toast.show({ content: '已切歌', icon: 'success' })
      fetchPlayStatus()
    } catch {
      // handled
    } finally {
      setOperating(false)
    }
  }

  const handleReplay = async () => {
    if (operating || !playInfo) return
    setOperating(true)
    try {
      await replaySong(orderId)
      Toast.show({ content: '重唱中', icon: 'success' })
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
      }
      if (playerRef.current) {
        replayTimerRef.current = setTimeout(() => {
          if (playerRef.current) {
            playerRef.current.seek(0)
            playerRef.current.play().catch(() => {})
          }
          replayTimerRef.current = null
        }, 300)
      }
      fetchPlayStatus()
    } catch {
      // handled
    } finally {
      setOperating(false)
    }
  }

  const hasSong = playInfo && playInfo.songId && playInfo.playStatus && playInfo.playStatus !== 'NONE'
  const isPlaying = playInfo?.playStatus === 'PLAYING'

  return (
    <div className="play-bar">
      {!hasSong ? (
        <div className="play-bar-empty">
          <span>🎵</span>
          <span>暂无歌曲，快去点歌吧</span>
        </div>
      ) : (
        <div className="play-bar-active">
          <div className="play-info">
            <div className={`play-indicator ${isPlaying ? 'playing' : 'paused'}`}>
              <span />
              <span />
              <span />
            </div>
            <div className="play-text">
              <div className="play-song-name">{playInfo.songName}</div>
              <div className="play-singer-name">
                {playInfo.singerName}
                {playInfo.queueRemaining > 0 && (
                  <span className="queue-count"> · 待唱{playInfo.queueRemaining}首</span>
                )}
              </div>
            </div>
          </div>

          {!isVideoMode && (
            <div className="player-container" ref={playerContainerRef} />
          )}

          <div className="play-controls">
            <button className="ctrl-btn" onClick={handleReplay} disabled={operating} title="重唱">
              <AudioFill fontSize={26} />
            </button>
            <button
              className={`ctrl-btn play-pause-btn ${isPlaying ? 'is-playing' : ''}`}
              onClick={handleTogglePause}
              disabled={operating}
              title={isPlaying ? '暂停' : '继续'}
            >
              {isPlaying ? <StopOutline fontSize={30} /> : <PlayOutline fontSize={30} />}
            </button>
            <button className="ctrl-btn" onClick={handleNext} disabled={operating} title="切歌">
              <RightOutline fontSize={26} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
