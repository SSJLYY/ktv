import { useEffect, useRef, useState } from 'react'
import { DotLoading } from 'antd-mobile'
import { getAllSingers } from '../../api/song'
import './SingerList.css'

function SingerAvatar({ singer }) {
  const [hasError, setHasError] = useState(false)

  if (!singer.avatar || hasError) {
    const initial = singer.name ? singer.name.charAt(0).toUpperCase() : '?'
    return <span className="avatar-placeholder">{initial}</span>
  }

  return <img src={singer.avatar} alt={singer.name} onError={() => setHasError(true)} />
}

export default function SingerList({ onSelect }) {
  const [singers, setSingers] = useState([])
  const [activeLetter, setActiveLetter] = useState(null)
  const [loading, setLoading] = useState(true)
  const listContentRef = useRef(null)

  useEffect(() => {
    let isMounted = true

    getAllSingers()
      .then((res) => {
        if (isMounted) {
          setSingers(res.data || [])
        }
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) {
          setLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  if (loading) {
    return <div className="loading-wrapper"><DotLoading /> 加载中...</div>
  }

  if (singers.length === 0) {
    return <div className="empty-text">暂无歌手</div>
  }

  const grouped = singers.reduce((acc, singer) => {
    const letter = (singer.pinyinInitial || '#').toUpperCase()
    if (!acc[letter]) {
      acc[letter] = []
    }
    acc[letter].push(singer)
    return acc
  }, {})

  const sortedLetters = Object.keys(grouped).sort((a, b) => {
    if (a === '#') return 1
    if (b === '#') return -1
    return a.localeCompare(b)
  })

  return (
    <div className="singer-list-container">
      <div className="singer-list-content" ref={listContentRef}>
        {sortedLetters.map((letter) => (
          <div key={letter} className="singer-group" id={`group-${letter}`}>
            <div className="group-letter">{letter}</div>
            {grouped[letter].map((singer) => (
              <div key={singer.id} className="singer-item" onClick={() => onSelect?.(singer)}>
                <div className="singer-avatar">
                  <SingerAvatar singer={singer} />
                </div>
                <div className="singer-info">
                  <div className="singer-name">{singer.name}</div>
                  <div className="singer-meta">
                    {singer.songCount > 0 ? `${singer.songCount} 首歌` : '暂无歌曲'}
                    {singer.region ? ` · ${singer.region}` : ''}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>

      <div className="letter-index">
        {sortedLetters.map((letter) => (
          <div
            key={letter}
            className={`letter-item ${activeLetter === letter ? 'active' : ''}`}
            onClick={() => {
              setActiveLetter(letter)
              const targetElement = document.getElementById(`group-${letter}`)
              const container = listContentRef.current
              if (targetElement && container) {
                const targetTop = targetElement.offsetTop - container.offsetTop
                container.scrollTo({ top: targetTop, behavior: 'smooth' })
              }
            }}
          >
            {letter}
          </div>
        ))}
      </div>
    </div>
  )
}
