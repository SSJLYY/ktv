import { useEffect, useRef, useState, useCallback } from 'react'
import { Toast } from 'antd-mobile'
import {
  PlayOutline,
  SoundMuteOutline,
  RightOutline,
  AudioFill,
} from 'antd-mobile-icons'
import APlayer from 'aplayer'
import 'aplayer/dist/APlayer.min.css'
import { getCurrentPlayStatus, nextSong, replaySong, pausePlay, resumePlay, getMediaStreamUrl, getCoverUrl, isVideoFile } from '../../api/play'
import useRoomStore from '../../store/roomStore'
import './index.css'

export default function PlayBar({ onVideoPlay }) {
  const orderId = useRoomStore((s) => s.orderId)
  const queueVersion = useRoomStore((s) => s.queueVersion)
  const [playInfo, setPlayInfo] = useState(null) // CurrentPlayVO
  const [operating, setOperating] = useState(false)
  const [isVideoMode, setIsVideoMode] = useState(false)
  const playerRef = useRef(null)
  const playerContainerRef = useRef(null)
  const timerRef = useRef(null)

  // F-S3修复：使用 useCallback 缓存 fetchPlayStatus，避免定时器因 fetchPlayStatus 变化而不必要地销毁重建
  // 获取播放状态
  const fetchPlayStatus = useCallback(async () => {
    if (!orderId) return
    try {
      const res = await getCurrentPlayStatus(orderId)
      setPlayInfo(res.data)
    } catch { /* handled */ }
  }, [orderId])

  // F-S3修复：使用 ref 追踪 queueVersion，避免每次点歌后定时器不必要的销毁重建
  const queueVersionRef = useRef(queueVersion)
  queueVersionRef.current = queueVersion

  // 初始化 + 轮询（每5秒）
  // F-S3：依赖仅用 fetchPlayStatus（依赖 orderId），queueVersion 变化时通过 ref 追踪，
  // 点歌后手动调一次 fetchPlayStatus 即可，无需重建整个定时器
  useEffect(() => {
    // 先清理旧的定时器
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
    }
  }, [fetchPlayStatus])

  // F-S3修复：queueVersion 变化时手动刷新一次播放状态（点歌后立即可见队列变化）
  useEffect(() => {
    if (queueVersion > 0) {
      fetchPlayStatus()
    }
  }, [queueVersion, fetchPlayStatus])

  // 使用 ref 保存回调函数，避免闭包问题
  const callbacksRef = useRef({})
  // 使用 ref 跟踪销毁定时器，避免内存泄漏
  const destroyTimerRef = useRef(null)
  // 使用 ref 跟踪重唱定时器
  const replayTimerRef = useRef(null)

  // 初始化/更新 APlayer
  useEffect(() => {
    if (!playInfo?.songId || !playerContainerRef.current) return

    // 如果是视频文件，交给VideoPlayer处理
    if (isVideoFile(playInfo.filePath)) {
      setIsVideoMode(true)
      // 先暂停音频，再销毁播放器，防止音频继续播放
      if (playerRef.current) {
        try {
          playerRef.current.pause()
        } catch (e) {
          console.warn('暂停播放器失败', e)
        }
        // 清理之前的销毁定时器
        if (destroyTimerRef.current) {
          clearTimeout(destroyTimerRef.current)
        }
        // 延迟销毁，确保音频已停止（250ms足够大多数设备完成切换）
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
      // 通知父组件显示视频
      if (onVideoPlay) {
        onVideoPlay(playInfo)
      }
      return
    }

    setIsVideoMode(false)

    // 音频模式：使用APlayer
    const streamUrl = getMediaStreamUrl(playInfo.songId)
    const coverUrl = getCoverUrl(playInfo.songId)

    // 销毁旧播放器
    if (playerRef.current) {
      try {
        playerRef.current.destroy()
      } catch (e) {
        console.warn('销毁播放器失败', e)
      }
    }

    // 创建新播放器
    const isMuted = localStorage.getItem('ktv_muted') === 'true'
    const ap = new APlayer({
      container: playerContainerRef.current,
      mini: true,
      autoplay: playInfo.playStatus === 'PLAYING',
      mutex: true,
      loop: false,
      volume: (() => {
        const savedVolume = parseFloat(localStorage.getItem('ktv_volume') || '0.7')
        return isNaN(savedVolume) ? 0.7 : Math.max(0, Math.min(1, savedVolume))
      })(),
      mute: isMuted,
      audio: [{
        name: playInfo.songName || '未知歌曲',
        artist: playInfo.singerName || '未知歌手',
        url: streamUrl,
        cover: coverUrl,
        lrc: '', // Bug1修复：CurrentPlayVO 无 lyricPath 字段，暂不支持歌词
      }],
    })

    // M27修复：添加APlayer错误处理
    ap.on('error', () => {
      console.error('APlayer播放错误')
      Toast.show({ content: '音频播放失败，请检查文件', icon: 'fail' })
    })

    // 保存回调函数引用
    callbacksRef.current = {
      onEnded: async () => {
        try {
          await nextSong(orderId)
          fetchPlayStatus()
        } catch { /* handled */ }
      },
      onVolumeChange: (newVolume) => {
        localStorage.setItem('ktv_volume', newVolume.toString())
      },
      onMute: () => {
        localStorage.setItem('ktv_muted', 'true')
      },
      onUnmute: () => {
        localStorage.setItem('ktv_muted', 'false')
      }
    }

    // 监听播放结束，自动切歌
    ap.on('ended', callbacksRef.current.onEnded)

    // 保存音量到localStorage
    ap.on('volumechange', callbacksRef.current.onVolumeChange)

    // 保存静音状态
    ap.on('mute', callbacksRef.current.onMute)
    ap.on('unmute', callbacksRef.current.onUnmute)

    playerRef.current = ap

    return () => {
      // 清理销毁定时器
      if (destroyTimerRef.current) {
        clearTimeout(destroyTimerRef.current)
        destroyTimerRef.current = null
      }
      // 清理重唱定时器
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
        replayTimerRef.current = null
      }
      // 组件卸载时销毁播放器，防止内存泄漏
      if (playerRef.current) {
        try {
          // 移除事件监听（APlayer 不支持 off，但销毁时会清理）
          playerRef.current.destroy()
        } catch (e) {
          console.warn('销毁播放器失败', e)
        }
        playerRef.current = null
      }
    }
  }, [playInfo?.songId, onVideoPlay, orderId, fetchPlayStatus])

  // 使用 ref 保存 playInfo，避免闭包陷阱
  const playInfoRef = useRef(playInfo)
  playInfoRef.current = playInfo

  // 同步后端播放状态
  useEffect(() => {
    if (!playerRef.current || !playInfoRef.current) return

    const syncPlayState = () => {
      const currentPlayInfo = playInfoRef.current
      if (!currentPlayInfo) return
      if (currentPlayInfo.playStatus === 'PLAYING') {
        playerRef.current.play().catch(() => {})
      } else if (currentPlayInfo.playStatus === 'PAUSED') {
        playerRef.current.pause()
      }
    }

    syncPlayState()
  }, [playInfo?.playStatus])

  // 暂停/继续
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
    } catch { /* handled */ }
    finally { setOperating(false) }
  }

  // 切歌
  const handleNext = async () => {
    if (operating) return
    setOperating(true)
    try {
      await nextSong(orderId)
      Toast.show({ content: '已切歌', icon: 'success' })
      fetchPlayStatus()
    } catch { /* handled */ }
    finally { setOperating(false) }
  }

  // 重唱
  const handleReplay = async () => {
    if (operating || !playInfo) return
    setOperating(true)
    try {
      await replaySong(orderId)
      Toast.show({ content: '重唱中', icon: 'success' })
      // 清理之前的重唱定时器
      if (replayTimerRef.current) {
        clearTimeout(replayTimerRef.current)
      }
      // 重唱后等待短暂延迟确保音频已准备好，再seek到开头
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
    } catch { /* handled */ }
    finally { setOperating(false) }
  }

  // 无歌曲状态：必须同时有 songId，避免 playStatus=PLAYING 但 songId=null 时渲染 undefined
  const hasSong = playInfo && playInfo.songId && playInfo.playStatus && playInfo.playStatus !== 'NONE'
  const isPlaying = playInfo?.playStatus === 'PLAYING'

  return (
    <div className="play-bar">
      {!hasSong ? (
        <div className="play-bar-empty">
          <span>🎤</span>
          <span>暂无歌曲，快去点歌吧</span>
        </div>
      ) : (
        <div className="play-bar-active">
          {/* 左侧：歌曲信息 */}
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

          {/* 中间：APlayer播放器容器 */}
          {!isVideoMode && (
            <div className="player-container" ref={playerContainerRef} />
          )}

          {/* 右侧：控制按钮 */}
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
              {isPlaying ? <SoundMuteOutline fontSize={30} /> : <PlayOutline fontSize={30} />}
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
