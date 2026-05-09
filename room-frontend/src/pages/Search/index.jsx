import { useEffect, useRef, useState } from 'react'
import { DotLoading, SearchBar, Tabs, Tag, Toast } from 'antd-mobile'
import { AddOutline } from 'antd-mobile-icons'
import {
  getAllCategories,
  getHotSongs,
  getSongsByCategory,
  getSongsBySinger,
  searchSongs,
} from '../../api/song'
import { addSongToQueue } from '../../api/queue'
import useRoomStore from '../../store/roomStore'
import SingerList from './SingerList'
import './index.css'

const tabItems = [
  { key: 'search', title: '搜索' },
  { key: 'singer', title: '歌手' },
  { key: 'category', title: '分类' },
  { key: 'hot', title: '热门' },
]

export default function Search() {
  const orderId = useRoomStore((state) => state.orderId)
  const bumpQueueVersion = useRoomStore((state) => state.bumpQueueVersion)
  const [activeTab, setActiveTab] = useState('search')

  return (
    <div className="search-page">
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

      <div className="search-content">
        {activeTab === 'search' && <SearchTab orderId={orderId} bumpQueueVersion={bumpQueueVersion} />}
        {activeTab === 'singer' && <SingerTab orderId={orderId} bumpQueueVersion={bumpQueueVersion} />}
        {activeTab === 'category' && <CategoryTab orderId={orderId} bumpQueueVersion={bumpQueueVersion} />}
        {activeTab === 'hot' && <HotTab orderId={orderId} bumpQueueVersion={bumpQueueVersion} />}
      </div>
    </div>
  )
}

function SearchTab({ orderId, bumpQueueVersion }) {
  const [keyword, setKeyword] = useState('')
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const timerRef = useRef(null)
  const searchRequestIdRef = useRef(0)
  const mountedRef = useRef(true)

  const doSearch = async (value) => {
    const trimmed = value.trim()
    if (!trimmed) {
      searchRequestIdRef.current += 1
      setSongs([])
      setSearched(false)
      setLoading(false)
      return
    }

    const requestId = searchRequestIdRef.current + 1
    searchRequestIdRef.current = requestId
    setLoading(true)
    try {
      const res = await searchSongs(trimmed, 1, 50)
      if (!mountedRef.current || requestId !== searchRequestIdRef.current) {
        return
      }
      setSongs(res.data?.records || [])
      setSearched(true)
    } catch {
      // handled by interceptor
    } finally {
      if (mountedRef.current && requestId === searchRequestIdRef.current) {
        setLoading(false)
      }
    }
  }

  const handleSearch = (value) => {
    setKeyword(value)
    if (timerRef.current) {
      clearTimeout(timerRef.current)
    }
    timerRef.current = setTimeout(() => {
      doSearch(value)
    }, 300)
  }

  useEffect(() => {
    return () => {
      mountedRef.current = false
      if (timerRef.current) {
        clearTimeout(timerRef.current)
      }
    }
  }, [])

  return (
    <div className="search-tab">
      <SearchBar
        placeholder="输入歌名或拼音首字母"
        value={keyword}
        onChange={handleSearch}
        onClear={() => {
          if (timerRef.current) {
            clearTimeout(timerRef.current)
            timerRef.current = null
          }
          searchRequestIdRef.current += 1
          setKeyword('')
          setSongs([])
          setSearched(false)
          setLoading(false)
        }}
        style={{
          '--font-size': '16px',
          '--height': '48px',
        }}
      />

      {loading && <div className="loading-wrapper"><DotLoading /> 搜索中...</div>}
      {!loading && songs.length > 0 && (
        <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
      )}
      {!loading && searched && songs.length === 0 && <div className="empty-text">未找到相关歌曲</div>}
    </div>
  )
}

function SingerTab({ orderId, bumpQueueVersion }) {
  const [selectedSinger, setSelectedSinger] = useState(null)

  if (selectedSinger) {
    return (
      <div className="singer-songs-view">
        <div className="view-header" onClick={() => setSelectedSinger(null)}>
          返回 {selectedSinger.name} 的歌曲
        </div>
        <SingerSongs singerId={selectedSinger.id} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
      </div>
    )
  }

  return <SingerList onSelect={(singer) => setSelectedSinger(singer)} />
}

function SingerSongs({ singerId, orderId, bumpQueueVersion }) {
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let isMounted = true
    setLoading(true)

    getSongsBySinger(singerId, 1, 100)
      .then((res) => {
        if (isMounted) {
          setSongs(res.data?.records || [])
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
  }, [singerId])

  if (loading) {
    return <div className="loading-wrapper"><DotLoading /> 加载中...</div>
  }
  if (songs.length === 0) {
    return <div className="empty-text">暂无歌曲</div>
  }
  return <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
}

function CategoryTab({ orderId, bumpQueueVersion }) {
  const [categories, setCategories] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState(null)
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let isMounted = true

    getAllCategories()
      .then((res) => {
        if (!isMounted) {
          return
        }
        const list = res.data || []
        setCategories(list)
        if (list.length > 0) {
          setSelectedCategoryId(list[0].id)
        } else {
          setLoading(false)
        }
      })
      .catch(() => {
        if (isMounted) {
          setLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    if (!selectedCategoryId) {
      return
    }

    let isMounted = true
    setLoading(true)

    getSongsByCategory(selectedCategoryId, 1, 100)
      .then((res) => {
        if (isMounted) {
          setSongs(res.data?.records || [])
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
  }, [selectedCategoryId])

  return (
    <div className="category-tab">
      <div className="category-tags">
        {categories.map((category) => (
          <Tag
            key={category.id}
            round
            color={selectedCategoryId === category.id ? 'primary' : 'default'}
            onClick={() => setSelectedCategoryId(category.id)}
            style={{
              '--font-size': '14px',
              padding: '6px 16px',
              marginRight: '8px',
              marginBottom: '8px',
            }}
          >
            {category.name}
          </Tag>
        ))}
      </div>

      {loading ? (
        <div className="loading-wrapper"><DotLoading /> 加载中...</div>
      ) : songs.length > 0 ? (
        <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
      ) : (
        <div className="empty-text">该分类下暂无歌曲</div>
      )}
    </div>
  )
}

function HotTab({ orderId, bumpQueueVersion }) {
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let isMounted = true

    getHotSongs(50)
      .then((res) => {
        if (isMounted) {
          setSongs(res.data || [])
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
  if (songs.length === 0) {
    return <div className="empty-text">暂无热门歌曲</div>
  }
  return <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} showRank />
}

function SongList({ songs, orderId, bumpQueueVersion, showRank = false }) {
  const [addingId, setAddingId] = useState(null)

  const handleAdd = async (song) => {
    if (!orderId) {
      Toast.show({ content: '当前未加入包厢，请先返回重新入房', icon: 'fail' })
      return
    }
    if (addingId) {
      return
    }

    setAddingId(song.id)
    try {
      await addSongToQueue(orderId, song.id)
      Toast.show({ content: `已点歌：${song.name}`, icon: 'success' })
      bumpQueueVersion()
    } catch {
      // handled by interceptor
    } finally {
      setAddingId(null)
    }
  }

  return (
    <div className="song-list">
      {songs.map((song, index) => (
        <div className="song-item" key={song.id}>
          <div className="song-info">
            {showRank && <span className={`song-rank ${index < 3 ? 'top-rank' : ''}`}>{index + 1}</span>}
            <div className="song-text">
              <div className="song-name">{song.name}</div>
              <div className="song-meta">
                {song.singerName || '未知歌手'}
                {song.language ? <span className="song-lang">{song.language}</span> : null}
              </div>
            </div>
          </div>
          <button
            className={`btn-add ${addingId === song.id ? 'adding' : ''}`}
            onClick={() => handleAdd(song)}
            disabled={addingId === song.id}
          >
            <AddOutline fontSize={22} />
          </button>
        </div>
      ))}
    </div>
  )
}
