import React, { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Upload,
  message,
} from 'antd'
import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  PictureOutlined,
  PlusOutlined,
  SoundOutlined,
  UploadOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import {
  addSong,
  deleteSong,
  getSongById,
  getSongList,
  updateSong,
  uploadCoverImage,
  uploadSongFile,
} from '../../api/song'
import { getAllCategories } from '../../api/category'
import { getSingerList } from '../../api/singer'
import { useUserStore } from '../../store/userStore'

const AUDIO_TYPES = ['mp3', 'flac', 'wav', 'ogg', 'm4a']
const VIDEO_TYPES = ['mp4', 'avi', 'mkv', 'webm']
const IMAGE_TYPES = ['jpg', 'jpeg', 'png', 'gif', 'webp']
const MEDIA_TYPES = [...AUDIO_TYPES, ...VIDEO_TYPES]

const languageOptions = [
  { value: '国语', label: '国语' },
  { value: '粤语', label: '粤语' },
  { value: '英语', label: '英语' },
  { value: '日语', label: '日语' },
  { value: '韩语', label: '韩语' },
  { value: '其他', label: '其他' },
]

const defaultQueryParams = {
  pageNum: 1,
  pageSize: 10,
  name: '',
  singerId: '',
  categoryId: '',
  language: '',
  status: '',
}

const getFileExt = (filename) => {
  if (!filename) return ''
  const parts = filename.split('.')
  return parts.length > 1 ? parts.pop().toLowerCase() : ''
}

const isAudio = (filename) => AUDIO_TYPES.includes(getFileExt(filename))
const isVideo = (filename) => VIDEO_TYPES.includes(getFileExt(filename))

const getMediaDuration = (file) =>
  new Promise((resolve) => {
    const objectUrl = URL.createObjectURL(file)
    const tagName = isVideo(file.name) ? 'video' : 'audio'
    const media = document.createElement(tagName)
    media.preload = 'metadata'
    media.src = objectUrl
    media.onloadedmetadata = () => {
      URL.revokeObjectURL(objectUrl)
      resolve(Number.isFinite(media.duration) ? Math.round(media.duration) : null)
    }
    media.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      resolve(null)
    }
  })

const Song = () => {
  const userInfo = useUserStore((state) => state.userInfo)
  const isSuperAdmin = userInfo?.role === 'super_admin'

  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [queryParams, setQueryParams] = useState(defaultQueryParams)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingSong, setEditingSong] = useState(null)
  const [form] = Form.useForm()

  const [singers, setSingers] = useState([])
  const [categories, setCategories] = useState([])

  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [uploadingSong, setUploadingSong] = useState(null)
  const [mediaFileList, setMediaFileList] = useState([])
  const [coverFileList, setCoverFileList] = useState([])
  const [uploadProgress, setUploadProgress] = useState(0)
  const [coverProgress, setCoverProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const [uploadDone, setUploadDone] = useState({ media: false, cover: false })

  const loadOptions = useCallback(async () => {
    try {
      const [singerRes, categoryRes] = await Promise.all([
        getSingerList({ pageNum: 1, pageSize: 1000, status: 1 }),
        getAllCategories(),
      ])
      setSingers(singerRes.data?.records || [])
      setCategories(categoryRes.data || [])
    } catch (error) {
      console.error('Load song options failed:', error)
    }
  }, [])

  const loadSongList = useCallback(async () => {
    try {
      setLoading(true)
      const res = await getSongList(queryParams)
      setDataSource(res.data?.records || [])
      setTotal(res.data?.total || 0)
    } catch (error) {
      console.error('Load song list failed:', error)
    } finally {
      setLoading(false)
    }
  }, [queryParams])

  useEffect(() => {
    loadOptions()
  }, [loadOptions])

  useEffect(() => {
    loadSongList()
  }, [loadSongList])

  const resetUploadState = () => {
    setMediaFileList([])
    setCoverFileList([])
    setUploadProgress(0)
    setCoverProgress(0)
    setUploadDone({ media: false, cover: false })
    setUploading(false)
  }

  const closeEditModal = () => {
    setModalVisible(false)
    setEditingSong(null)
    form.resetFields()
  }

  const closeUploadModal = () => {
    if (uploading) {
      return
    }
    setUploadModalVisible(false)
    setUploadingSong(null)
    resetUploadState()
  }

  const handleSearch = () => {
    setQueryParams((prev) => ({ ...prev, pageNum: 1 }))
  }

  const handleReset = () => {
    setQueryParams({ ...defaultQueryParams })
  }

  const handleAdd = () => {
    setEditingSong(null)
    form.resetFields()
    form.setFieldsValue({
      status: 1,
      isHot: 0,
      isNew: 0,
      language: '国语',
    })
    setModalVisible(true)
  }

  const handleEdit = async (record) => {
    try {
      const res = await getSongById(record.id)
      setEditingSong(res.data)
      form.resetFields()
      form.setFieldsValue({
        ...res.data,
        singerId: res.data?.singerId ?? undefined,
        categoryId: res.data?.categoryId ?? undefined,
        duration: res.data?.duration ?? undefined,
        status: res.data?.status ?? 1,
        isHot: res.data?.isHot ?? 0,
        isNew: res.data?.isNew ?? 0,
      })
      setModalVisible(true)
    } catch (error) {
      console.error('Load song detail failed:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteSong(id)
      message.success('歌曲删除成功')
      await loadSongList()
    } catch (error) {
      console.error('Delete song failed:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingSong) {
        await updateSong(editingSong.id, values)
        message.success('歌曲更新成功')
      } else {
        await addSong(values)
        message.success('歌曲创建成功')
      }
      closeEditModal()
      await loadSongList()
    } catch (error) {
      console.error('Submit song failed:', error)
    }
  }

  const openUploadModal = (record) => {
    setUploadingSong(record)
    resetUploadState()
    setUploadModalVisible(true)
  }

  const beforeMediaUpload = async (file) => {
    const ext = getFileExt(file.name)
    if (!MEDIA_TYPES.includes(ext)) {
      message.error(`仅支持 ${MEDIA_TYPES.join('/')} 格式的媒体文件`)
      return Upload.LIST_IGNORE
    }
    if (file.size / 1024 / 1024 > 100) {
      message.error('媒体文件大小不能超过 100MB')
      return Upload.LIST_IGNORE
    }

    const durationSeconds = await getMediaDuration(file)
    if (durationSeconds) {
      file.durationSeconds = durationSeconds
    }

    setMediaFileList([file])
    setUploadProgress(0)
    setUploadDone((prev) => ({ ...prev, media: false }))
    return false
  }

  const beforeCoverUpload = (file) => {
    const ext = getFileExt(file.name)
    if (!IMAGE_TYPES.includes(ext)) {
      message.error(`仅支持 ${IMAGE_TYPES.join('/')} 格式的封面图片`)
      return Upload.LIST_IGNORE
    }
    if (file.size / 1024 / 1024 > 10) {
      message.error('封面图片大小不能超过 10MB')
      return Upload.LIST_IGNORE
    }

    setCoverFileList([file])
    setCoverProgress(0)
    setUploadDone((prev) => ({ ...prev, cover: false }))
    return false
  }

  const syncDurationAfterUpload = async (songId, durationSeconds) => {
    if (!durationSeconds) {
      return
    }

    const latestSongRes = await getSongById(songId)
    const latestSong = latestSongRes.data
    await updateSong(songId, {
      name: latestSong.name,
      singerId: latestSong.singerId,
      categoryId: latestSong.categoryId,
      language: latestSong.language,
      duration: durationSeconds,
      filePath: latestSong.filePath,
      coverUrl: latestSong.coverUrl,
      lyricPath: latestSong.lyricPath,
      status: latestSong.status,
      isHot: latestSong.isHot,
      isNew: latestSong.isNew,
    })
  }

  const handleDoUpload = async () => {
    if (!uploadingSong) {
      message.warning('未找到当前歌曲')
      return
    }
    if (!mediaFileList.length && !coverFileList.length) {
      message.warning('请先选择需要上传的文件')
      return
    }

    setUploading(true)
    const doneState = { ...uploadDone }

    try {
      if (mediaFileList.length) {
        const mediaFile = mediaFileList[0]
        const res = await uploadSongFile(uploadingSong.id, mediaFile, (event) => {
          if (event.total) {
            setUploadProgress(Math.round((event.loaded / event.total) * 100))
          }
        })
        message.success(`媒体文件上传成功：${res.data?.fileName || mediaFile.name}`)
        doneState.media = true
        setUploadDone((prev) => ({ ...prev, media: true }))
        await syncDurationAfterUpload(uploadingSong.id, mediaFile.durationSeconds)
      }

      if (coverFileList.length) {
        const coverFile = coverFileList[0]
        await uploadCoverImage(uploadingSong.id, coverFile, (event) => {
          if (event.total) {
            setCoverProgress(Math.round((event.loaded / event.total) * 100))
          }
        })
        message.success('封面图片上传成功')
        doneState.cover = true
        setUploadDone((prev) => ({ ...prev, cover: true }))
      }
    } catch (error) {
      console.error('Upload failed:', error)
      message.error(error?.message || '上传失败')
    } finally {
      setUploading(false)
    }

    if (doneState.media) {
      setMediaFileList([])
    }
    if (doneState.cover) {
      setCoverFileList([])
    }

    await loadSongList()
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '歌曲名称',
      dataIndex: 'name',
      key: 'name',
      width: 220,
    },
    {
      title: '歌手',
      dataIndex: 'singerName',
      key: 'singerName',
      width: 140,
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 120,
      render: (value) => value || '-',
    },
    {
      title: '语言',
      dataIndex: 'language',
      key: 'language',
      width: 100,
      render: (value) => value || '-',
    },
    {
      title: '时长(秒)',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (value) => value ?? '-',
    },
    {
      title: '播放次数',
      dataIndex: 'playCount',
      key: 'playCount',
      width: 100,
      render: (value) => value ?? 0,
    },
    {
      title: '媒体文件',
      dataIndex: 'filePath',
      key: 'filePath',
      width: 120,
      render: (filePath) => {
        if (!filePath) {
          return <Tag>未上传</Tag>
        }
        if (isAudio(filePath)) {
          return (
            <Tag color="blue" icon={<SoundOutlined />}>
              音频
            </Tag>
          )
        }
        if (isVideo(filePath)) {
          return (
            <Tag color="purple" icon={<VideoCameraOutlined />}>
              视频
            </Tag>
          )
        }
        return <Tag color="green">已上传</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'default'}>{status === 1 ? '上架' : '下架'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space>
          {isSuperAdmin && (
            <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
              编辑
            </Button>
          )}
          {isSuperAdmin && (
            <Button type="link" icon={<UploadOutlined />} onClick={() => openUploadModal(record)}>
              上传
            </Button>
          )}
          {isSuperAdmin && (
            <Popconfirm
              title="确定删除这首歌曲吗？"
              description="删除后不可恢复。"
              onConfirm={() => handleDelete(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="link" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="歌曲名称"
            value={queryParams.name}
            onChange={(e) => setQueryParams((prev) => ({ ...prev, name: e.target.value }))}
            onPressEnter={handleSearch}
            style={{ width: 180 }}
          />
          <Select
            placeholder="歌手"
            value={queryParams.singerId || undefined}
            onChange={(value) => setQueryParams((prev) => ({ ...prev, singerId: value || '' }))}
            options={singers.map((item) => ({ value: item.id, label: item.name }))}
            allowClear
            showSearch
            filterOption={(input, option) =>
              String(option?.label || '').toLowerCase().includes(input.toLowerCase())
            }
            style={{ width: 180 }}
          />
          <Select
            placeholder="分类"
            value={queryParams.categoryId || undefined}
            onChange={(value) =>
              setQueryParams((prev) => ({ ...prev, categoryId: value || '' }))
            }
            options={categories.map((item) => ({ value: item.id, label: item.name }))}
            allowClear
            style={{ width: 140 }}
          />
          <Select
            placeholder="语言"
            value={queryParams.language || undefined}
            onChange={(value) => setQueryParams((prev) => ({ ...prev, language: value || '' }))}
            options={languageOptions}
            allowClear
            style={{ width: 120 }}
          />
          <Select
            placeholder="状态"
            value={queryParams.status === '' ? undefined : queryParams.status}
            onChange={(value) =>
              setQueryParams((prev) => ({ ...prev, status: value === undefined ? '' : value }))
            }
            options={[
              { value: 1, label: '上架' },
              { value: 0, label: '下架' },
            ]}
            allowClear
            style={{ width: 120 }}
          />
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </div>

      {isSuperAdmin && (
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增歌曲
          </Button>
        </div>
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        scroll={{ x: 1440 }}
        pagination={{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (value) => `共 ${value} 条`,
          onChange: (pageNum, pageSize) => {
            setQueryParams((prev) => ({ ...prev, pageNum, pageSize }))
          },
        }}
      />

      <Modal
        title={editingSong ? '编辑歌曲' : '新增歌曲'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={closeEditModal}
        okText="提交"
        cancelText="取消"
        width={640}
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <Form.Item
            name="name"
            label="歌曲名称"
            rules={[{ required: true, message: '请输入歌曲名称' }]}
          >
            <Input placeholder="请输入歌曲名称" />
          </Form.Item>

          <Form.Item
            name="singerId"
            label="歌手"
            rules={[{ required: true, message: '请选择歌手' }]}
          >
            <Select
              placeholder="请选择歌手"
              options={singers.map((item) => ({ value: item.id, label: item.name }))}
              showSearch
              filterOption={(input, option) =>
                String(option?.label || '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>

          <Form.Item name="categoryId" label="分类">
            <Select
              placeholder="请选择分类"
              options={categories.map((item) => ({ value: item.id, label: item.name }))}
              allowClear
            />
          </Form.Item>

          <Form.Item
            name="language"
            label="语言"
            rules={[{ required: true, message: '请选择语言' }]}
          >
            <Select placeholder="请选择语言" options={languageOptions} />
          </Form.Item>

          <Form.Item name="duration" label="时长(秒)">
            <InputNumber
              min={0}
              precision={0}
              style={{ width: '100%' }}
              placeholder="上传媒体后可自动回填"
            />
          </Form.Item>

          <Form.Item name="filePath" label="文件路径">
            <Input readOnly placeholder="上传媒体文件后自动回填" />
          </Form.Item>

          <Form.Item name="coverUrl" label="封面地址">
            <Input placeholder="上传封面后自动回填，也可手动输入 URL" />
          </Form.Item>

          <Form.Item name="lyricPath" label="歌词路径">
            <Input placeholder="可选，填写歌词文件路径" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select
              placeholder="请选择状态"
              options={[
                { value: 1, label: '上架' },
                { value: 0, label: '下架' },
              ]}
            />
          </Form.Item>

          <Form.Item name="isHot" label="热门歌曲">
            <Select
              options={[
                { value: 0, label: '否' },
                { value: 1, label: '是' },
              ]}
            />
          </Form.Item>

          <Form.Item name="isNew" label="新歌">
            <Select
              options={[
                { value: 0, label: '否' },
                { value: 1, label: '是' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          <Space>
            <UploadOutlined />
            <span>
              上传文件
              {uploadingSong ? (
                <span style={{ color: '#1677ff', marginLeft: 8 }}>《{uploadingSong.name}》</span>
              ) : null}
            </span>
          </Space>
        }
        open={uploadModalVisible}
        onCancel={closeUploadModal}
        maskClosable={false}
        width={600}
        footer={[
          <Button key="cancel" onClick={closeUploadModal} disabled={uploading}>
            关闭
          </Button>,
          <Button
            key="upload"
            type="primary"
            icon={<UploadOutlined />}
            loading={uploading}
            onClick={handleDoUpload}
            disabled={!mediaFileList.length && !coverFileList.length}
          >
            开始上传
          </Button>,
        ]}
      >
        <div style={{ marginBottom: 24 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            <SoundOutlined style={{ marginRight: 6, color: '#1677ff' }} />
            音频或视频文件
            <span style={{ color: '#999', fontWeight: 400, marginLeft: 8, fontSize: 12 }}>
              支持 MP3 / FLAC / WAV / OGG / M4A / MP4 / AVI / MKV / WebM，最大 100MB
            </span>
          </div>

          <Upload.Dragger
            accept=".mp3,.flac,.wav,.ogg,.m4a,.mp4,.avi,.mkv,.webm"
            beforeUpload={beforeMediaUpload}
            fileList={mediaFileList}
            onRemove={() => setMediaFileList([])}
            maxCount={1}
            showUploadList={{ showRemoveIcon: !uploading }}
          >
            <p className="ant-upload-drag-icon">
              <UploadOutlined style={{ fontSize: 32 }} />
            </p>
            <p className="ant-upload-text">点击或拖拽文件到这里</p>
            <p className="ant-upload-hint">上传后会自动尝试读取媒体时长并回填到歌曲信息。</p>
          </Upload.Dragger>

          {uploadProgress > 0 && (
            <div style={{ marginTop: 8 }}>
              <Progress
                percent={uploadProgress}
                status={uploadDone.media ? 'success' : 'active'}
                strokeColor={{ from: '#108ee9', to: '#87d068' }}
              />
            </div>
          )}

          {uploadDone.media && (
            <div style={{ color: '#52c41a', marginTop: 4 }}>
              <CheckCircleOutlined /> 媒体文件上传成功
            </div>
          )}
        </div>

        <Divider style={{ margin: '12px 0 20px' }} />

        <div>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            <PictureOutlined style={{ marginRight: 6, color: '#722ed1' }} />
            封面图片
            <span style={{ color: '#999', fontWeight: 400, marginLeft: 8, fontSize: 12 }}>
              支持 JPG / JPEG / PNG / GIF / WebP，最大 10MB
            </span>
          </div>

          <Upload.Dragger
            accept=".jpg,.jpeg,.png,.gif,.webp"
            beforeUpload={beforeCoverUpload}
            fileList={coverFileList}
            onRemove={() => setCoverFileList([])}
            maxCount={1}
            showUploadList={{ showRemoveIcon: !uploading }}
            listType="picture"
          >
            <p className="ant-upload-drag-icon">
              <PictureOutlined style={{ fontSize: 32, color: '#722ed1' }} />
            </p>
            <p className="ant-upload-text">点击或拖拽封面图片到这里</p>
          </Upload.Dragger>

          {coverProgress > 0 && (
            <div style={{ marginTop: 8 }}>
              <Progress
                percent={coverProgress}
                status={uploadDone.cover ? 'success' : 'active'}
                strokeColor={{ from: '#722ed1', to: '#eb2f96' }}
              />
            </div>
          )}

          {uploadDone.cover && (
            <div style={{ color: '#52c41a', marginTop: 4 }}>
              <CheckCircleOutlined /> 封面图片上传成功
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}

export default Song
