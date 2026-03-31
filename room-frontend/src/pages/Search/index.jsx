import { useState, useEffect, useCallback, useRef } from 'react'
import { Tabs, SearchBar, Toast, DotLoading, Tag } from 'antd-mobile'
import { AddOutline } from 'antd-mobile-icons'
import { searchSongs, getAllSingers, getSongsBySinger, getAllCategories, getSongsByCategory, getHotSongs } from '../../api/song'
import { addSongToQueue } from '../../api/queue'
import useRoomStore from '../../store/roomStore'
import SingerList from './SingerList'
import './index.css'

/** 顶部搜索标签页 */
const tabItems = [
  { key: 'search', title: '搜索' },
  { key: 'singer', title: '歌手' },
  { key: 'category', title: '分类' },
  { key: 'hot', title: '热门' },
]

export default function Search() {
  const orderId = useRoomStore((s) => s.orderId)
  const bumpQueueVersion = useRoomStore((s) => s.bumpQueueVersion)
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

// ==================== 搜索 Tab ====================
function SearchTab({ orderId, bumpQueueVersion }) {
  const [keyword, setKeyword] = useState('')
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const timerRef = useRef(null)

  // F-S2：useCallback 依赖数组为空是故意的：doSearch 只在 handleSearch 中通过 timerRef 调用，
  // 内部通过参数 kw 接收搜索关键词，不依赖任何外部 state，避免不必要地重建函数
  const doSearch = useCallback(async (kw) => {
    if (!kw.trim()) {
      setSongs([])
      setSearched(false)
      return
    }
    setLoading(true)
    try {
      const res = await searchSongs(kw.trim(), 1, 50)
      setSongs(res.data?.records || [])
      setSearched(true)
    } catch { /* handled by interceptor */ }
    finally { setLoading(false) }
  }, [])

  const handleSearch = (val) => {
    setKeyword(val)
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => doSearch(val), 300)
  }

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
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
        onClear={() => { setKeyword(''); setSongs([]); setSearched(false) }}
        style={{
          '--font-size': '16px',
          '--height': '48px',
        }}
      />
      {loading && <div className="loading-wrapper"><DotLoading /> 搜索中...</div>}
      {!loading && songs.length > 0 && (
        <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
      )}
      {!loading && searched && songs.length === 0 && (
        <div className="empty-text">未找到相关歌曲</div>
      )}
    </div>
  )
}

// ==================== 歌手 Tab ====================
function SingerTab({ orderId, bumpQueueVersion }) {
  const [selectedSinger, setSelectedSinger] = useState(null)

  if (selectedSinger) {
    return (
      <div className="singer-songs-view">
        <div className="view-header" onClick={() => setSelectedSinger(null)}>
          ← {selectedSinger.name} 的歌曲
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
        if (isMounted) setSongs(res.data?.records || [])
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false)
      })
    return () => {
      isMounted = false
    }
  }, [singerId])

  if (loading) return <div className="loading-wrapper"><DotLoading /> 加载中...</div>
  if (songs.length === 0) return <div className="empty-text">暂无歌曲</div>
  return <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
}

// ==================== 分类 Tab ====================
function CategoryTab({ orderId, bumpQueueVersion }) {
  const [categories, setCategories] = useState([])
  const [selectedCat, setSelectedCat] = useState(null)
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let isMounted = true
    getAllCategories()
      .then((res) => {
        if (!isMounted) return
        setCategories(res.data || [])
        if (res.data?.length > 0) {
          setSelectedCat(res.data[0].id)
        }
      })
      .catch(() => {})
    return () => { isMounted = false }
  }, [])

  useEffect(() => {
    if (!selectedCat) return
    let isMounted = true
    setLoading(true)
    getSongsByCategory(selectedCat, 1, 100)
      .then((res) => {
        if (isMounted) setSongs(res.data?.records || [])
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false)
      })
    return () => { isMounted = false }
  }, [selectedCat])

  return (
    <div className="category-tab">
      <div className="category-tags">
        {categories.map((cat) => (
          <Tag
            key={cat.id}
            round
            color={selectedCat === cat.id ? 'primary' : 'default'}
            onClick={() => setSelectedCat(cat.id)}
            style={{ '--font-size': '14px', padding: '6px 16px', marginRight: '8px', marginBottom: '8px' }}
          >
            {cat.name}
          </Tag>
        ))}
      </div>
      {loading ? (
        <div className="loading-wrapper"><DotLoading /> 加载中...</div>
      ) : songs.length > 0 ? (
        <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} />
      ) : (
        <div className="empty-text">该分类暂无歌曲</div>
      )}
    </div>
  )
}

// ==================== 热门 Tab ====================
function HotTab({ orderId, bumpQueueVersion }) {
  const [songs, setSongs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let isMounted = true
    setLoading(true)
    getHotSongs(50)
      .then((res) => {
        if (isMounted) setSongs(res.data || [])
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false)
      })
    return () => { isMounted = false }
  }, [])

  if (loading) return <div className="loading-wrapper"><DotLoading /> 加载中...</div>
  if (songs.length === 0) return <div className="empty-text">暂无热门歌曲</div>
  return <SongList songs={songs} orderId={orderId} bumpQueueVersion={bumpQueueVersion} showRank />
}

// ==================== 通用歌曲列表 ====================
function SongList({ songs, orderId, bumpQueueVersion, showRank }) {
  const [addingId, setAddingId] = useState(null)

  const handleAdd = async (song) => {
    if (addingId) return
    setAddingId(song.id)
    try {
      await addSongToQueue(orderId, song.id)
      Toast.show({ content: `已点歌：${song.name}`, icon: 'success' })
      bumpQueueVersion()
    } catch { /* handled */ }
    finally { setAddingId(null) }
  }

  return (
    <div className="song-list">
      {songs.map((song, idx) => (
        <div className="song-item" key={song.id}>
          <div className="song-info">
            {showRank && (
              <span className={`song-rank ${idx < 3 ? 'top-rank' : ''}`}>
                {idx + 1}
              </span>
            )}
            <div className="song-text">
              <div className="song-name">{song.name}</div>
              <div className="song-meta">
                {song.singerName}
                {song.language && <span className="song-lang">{song.language}</span>}
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
