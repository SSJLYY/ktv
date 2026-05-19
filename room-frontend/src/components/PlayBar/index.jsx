import { useCallback, useEffect, useRef, useState } from 'react'
import { Toast } from 'antd-mobile'
import { PlayOutline, StopOutline, RightOutline, AudioFill } from 'antd-mobile-icons'
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

const canAutoPlay = (playStatus) => playStatus === 'PLAYING'
const isPauseStatus = (playStatus) => playStatus === 'PAUSED'

export default function PlayBar({ onVideoPlay, onVideoUpdate }) {
  const orderId = useRoomStore((state) => state.orderId)
  const queueVersion = useRoomStore((state) => state.queueVersion)
  const bumpQueueVersion = useRoomStore((state) => state.bumpQueueVersion)
  const [playInfo, setPlayInfo] = useState(null)
  const [operating, setOperating] = useState(false)
  const [isVideoMode, setIsVideoMode] = useState(false)

  const playerRef = useRef(null)
  const playerContainerRef = useRef(null)
  const pollTimerRef = useRef(null)
  const retryTimerRef = useRef(null)
  const retryCountRef = useRef(0)
  const replayTimerRef = useRef(null)

  const MAX_RETRY_COUNT = 3
  const RETRY_DELAY_MS = 2000

  const destroyPlayer = useCallback(() => {
    if (!playerRef.current) {
      return
    }
    try {
      playerRef.current.destroy()
    } catch (error) {
      console.warn('Destroy player failed:', error)
    }
    playerRef.current = null
  }, [])

  const fetchPlayStatus = useCallback(async (isRetry = false) => {
    if (!orderId) {
      setPlayInfo(null)
      return
    }

    try {
      const res = await getCurrentPlayStatus(orderId)
      setPlayInfo(res.data)
      retryCountRef.current = 0
    } catch (error) {
      console.warn('Fetch play status failed:', error)
      if (!isRetry && retryCountRef.current < MAX_RETRY_COUNT) {
        retryCountRef.current += 1
        if (retryTimerRef.current) {
          clearTimeout(retryTimerRef.current)
        }
        retryTimerRef.current = setTimeout(() => {
          fetchPlayStatus(true)
        }, RETRY_DELAY_MS)
      }
    }
  }, [orderId])

  useEffect(() => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current)
    }
    if (retryTimerRef.current) {
      clearTimeout(retryTimerRef.current)
      retryTimerRef.current = null
    }

    fetchPlayStatus()

    if (orderId) {
      pollTimerRef.current = setInterval(fetchPlayStatus, 5000)
    }

    return () => {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current)
        pollTimerRef.current = null
      }
      if (retryTimerRef.current) {
        clearTimeout(retryTimerRef.current)
        retryTimerRef.current = null
      }
    }
  }, [orderId, fetchPlayStatus])

  useEffect(() => {
    if (queueVersion > 0) {
      fetchPlayStatus()
    }
  }, [queueVersion, fetchPlayStatus])

  useEffect(() => {
    if (!playInfo?.songId) {
      setIsVideoMode(false)
      destroyPlayer()
      onVideoPlay?.(null)
      return
    }

    if (isVideoFile(playInfo.filePath)) {
      setIsVideoMode(true)
      destroyPlayer()
      onVideoPlay?.(playInfo)
      return
    }

    setIsVideoMode(false)
    onVideoPlay?.(null)

    if (!playerContainerRef.current) {
      return
    }

    destroyPlayer()

    const savedVolume = Number.parseFloat(localStorage.getItem('ktv_volume') || '0.7')
    const ap = new APlayer({
      container: playerContainerRef.current,
      mini: true,
      autoplay: canAutoPlay(playInfo.playStatus),
      mutex: true,
      loop: false,
      volume: Number.isNaN(savedVolume) ? 0.7 : Math.max(0, Math.min(1, savedVolume)),
      mute: localStorage.getItem('ktv_muted') === 'true',
      audio: [
        {
          name: playInfo.songName || '未知歌曲',
          artist: playInfo.singerName || '未知歌手',
          url: getMediaStreamUrl(playInfo.songId),
          cover: getCoverUrl(playInfo.songId),
          lrc: '',
        },
      ],
    })

    ap.on('error', () => {
      Toast.show({ content: '音频播放失败，请检查媒体文件', icon: 'fail' })
    })
    ap.on('ended', async () => {
      try {
        await nextSong(orderId)
        bumpQueueVersion()
        fetchPlayStatus()
      } catch {
        // handled by interceptor
      }
    })
    ap.on('volumechange', (volume) => {
      localStorage.setItem('ktv_volume', String(volume))
    })
    ap.on('mute', () => {
      localStorage.setItem('ktv_muted', 'true')
    })
    ap.on('unmute', () => {
      localStorage.setItem('ktv_muted', 'false')
    })

    playerRef.current = ap

    return () => {
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
        replayTimerRef.current = null
      }
      destroyPlayer()
    }
  }, [
    playInfo?.songId,
    playInfo?.filePath,
    playInfo?.playStatus,
    orderId,
    onVideoPlay,
    destroyPlayer,
    fetchPlayStatus,
    bumpQueueVersion,
  ])

  useEffect(() => {
    if (isVideoMode && playInfo?.songId) {
      onVideoUpdate?.(playInfo)
    }
  }, [isVideoMode, onVideoUpdate, playInfo])

  useEffect(() => {
    if (!playerRef.current || !playInfo || isVideoMode) {
      return
    }

    if (canAutoPlay(playInfo.playStatus)) {
      playerRef.current.play().catch(() => {})
    } else if (isPauseStatus(playInfo.playStatus)) {
      playerRef.current.pause()
    }
  }, [playInfo?.playStatus, playInfo, isVideoMode])

  const handleTogglePause = async () => {
    if (operating || !playInfo || !orderId) {
      return
    }
    if (!canAutoPlay(playInfo.playStatus) && !isPauseStatus(playInfo.playStatus)) {
      return
    }

    setOperating(true)
    try {
      if (canAutoPlay(playInfo.playStatus)) {
        await pausePlay(orderId)
        Toast.show({ content: '已暂停', icon: 'success' })
      } else {
        await resumePlay(orderId)
        Toast.show({ content: '继续播放', icon: 'success' })
      }
      await fetchPlayStatus()
    } catch {
      // handled by interceptor
    } finally {
      setOperating(false)
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
      await fetchPlayStatus()
    } catch {
      // handled by interceptor
    } finally {
      setOperating(false)
    }
  }

  const handleReplay = async () => {
    if (operating || !playInfo || !orderId) {
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
          playerRef.current.seek(0)
          playerRef.current.play().catch(() => {})
        }
        replayTimerRef.current = null
      }, 300)
      await fetchPlayStatus()
    } catch {
      // handled by interceptor
    } finally {
      setOperating(false)
    }
  }

  const hasSong = Boolean(
    playInfo && playInfo.songId && playInfo.playStatus && playInfo.playStatus !== 'NONE'
  )
  const isPlaying = canAutoPlay(playInfo?.playStatus)
  const canTogglePause = canAutoPlay(playInfo?.playStatus) || isPauseStatus(playInfo?.playStatus)

  return (
    <div className="play-bar">
      {!hasSong ? (
        <div className="play-bar-empty">
          <span>♪</span>
          <span>暂无歌曲，去点一首吧</span>
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
                {playInfo.singerName || '未知歌手'}
                {playInfo.queueRemaining > 0 ? (
                  <span className="queue-count"> · 待唱 {playInfo.queueRemaining} 首</span>
                ) : null}
              </div>
            </div>
          </div>

          {!isVideoMode && <div className="player-container" ref={playerContainerRef} />}

          <div className="play-controls">
            <button className="ctrl-btn" onClick={handleReplay} disabled={operating} title="重唱">
              <AudioFill fontSize={26} />
            </button>
            <button
              className={`ctrl-btn play-pause-btn ${isPlaying ? 'is-playing' : ''}`}
              onClick={handleTogglePause}
              disabled={operating || !canTogglePause}
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
