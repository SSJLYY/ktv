import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Input,
  Select,
  Modal,
  Form,
  InputNumber,
  message,
  Popconfirm,
  Tag,
  Upload,
  Progress,
  Divider,
  Tooltip,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UploadOutlined,
  SoundOutlined,
  VideoCameraOutlined,
  PictureOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import {
  getSongList,
  getSongById,
  addSong,
  updateSong,
  deleteSong,
  uploadSongFile,
  uploadCoverImage,
} from '../../api/song'
import { getAllCategories } from '../../api/category'
import { getSingerList } from '../../api/singer'

// 允许上传的文件类型
const AUDIO_TYPES = ['mp3', 'flac', 'wav', 'ogg', 'm4a']
const VIDEO_TYPES = ['mp4', 'avi', 'mkv', 'webm']
const IMAGE_TYPES = ['jpg', 'jpeg', 'png', 'gif', 'webp']
const ALL_MEDIA_TYPES = [...AUDIO_TYPES, ...VIDEO_TYPES]

const getFileExt = (filename) => {
  if (!filename) return ''
  return filename.split('.').pop().toLowerCase()
}

const isAudio = (filename) => AUDIO_TYPES.includes(getFileExt(filename))
const isVideo = (filename) => VIDEO_TYPES.includes(getFileExt(filename))

// 读取音频时长（秒）
const getAudioDuration = (file) => {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file)
    const media = document.createElement(isVideo(file.name) ? 'video' : 'audio')
    media.src = url
    media.onloadedmetadata = () => {
      URL.revokeObjectURL(url)
      resolve(Math.round(media.duration))
    }
    media.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(null)
    }
  })
}

const Song = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [queryParams, setQueryParams] = useState({
    pageNum: 1,
    pageSize: 10,
    name: '',
    singerId: '',
    categoryId: '',
    language: '',
    status: '',
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [editingSong, setEditingSong] = useState(null)
  const [form] = Form.useForm()
  const [singers, setSingers] = useState([])
  const [categories, setCategories] = useState([])

  // 上传弹窗状态
  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [uploadingSong, setUploadingSong] = useState(null) // 正在上传文件的歌曲
  const [mediaFileList, setMediaFileList] = useState([])
  const [coverFileList, setCoverFileList] = useState([])
  const [uploadProgress, setUploadProgress] = useState(0)
  const [coverProgress, setCoverProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const [uploadDone, setUploadDone] = useState({ media: false, cover: false })

  // 语种选项
  const languageOptions = [
    { value: '', label: '全部' },
    { value: '国语', label: '国语' },
    { value: '粤语', label: '粤语' },
    { value: '英语', label: '英语' },
    { value: '日语', label: '日语' },
    { value: '韩语', label: '韩语' },
    { value: '其他', label: '其他' },
  ]

  // 加载歌手和分类列表
  const loadOptions = async () => {
    try {
      const singerRes = await getSingerList({ pageNum: 1, pageSize: 1000 })
      setSingers(singerRes.data.records || [])
      const categoryRes = await getAllCategories()
      setCategories(categoryRes.data || [])
    } catch (error) {
      console.error('加载选项失败:', error)
    }
  }

  useEffect(() => {
    loadOptions()
  }, [])

  const loadSongList = async () => {
    try {
      setLoading(true)
      const res = await getSongList(queryParams)
      setDataSource(res.data.records || [])
      setTotal(res.data.total || 0)
    } catch (error) {
      console.error('加载歌曲列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSongList()
  }, [queryParams])

  const handleSearch = () => {
    setQueryParams({ ...queryParams, pageNum: 1 })
  }

  const handleReset = () => {
    setQueryParams({
      pageNum: 1,
      pageSize: 10,
      name: '',
      singerId: '',
      categoryId: '',
      language: '',
      status: '',
    })
  }

  const handleAdd = () => {
    setEditingSong(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = async (record) => {
    try {
      const res = await getSongById(record.id)
      setEditingSong(res.data)
      // BugC1修复：先 resetFields 清空上一次的表单值，再 setFieldsValue 填充
      // Ant Design Form 的 setFieldsValue 对 null 字段不清空，会导致先后编辑两首歌时字段残留
      form.resetFields()
      form.setFieldsValue(res.data)
      setModalVisible(true)
    } catch (error) {
      console.error('获取歌曲详情失败:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteSong(id)
      message.success('删除成功')
      loadSongList()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingSong) {
        await updateSong(editingSong.id, values)
      } else {
        await addSong(values)
      }
      message.success(editingSong ? '修改成功' : '新增成功')
      setModalVisible(false)
      loadSongList()
    } catch (error) {
      console.error('提交失败:', error)
    }
  }

  // ========== 文件上传逻辑 ==========

  const openUploadModal = (record) => {
    setUploadingSong(record)
    setMediaFileList([])
    setCoverFileList([])
    setUploadProgress(0)
    setCoverProgress(0)
    setUploadDone({ media: false, cover: false })
    setUploading(false)
    setUploadModalVisible(true)
  }

  // 媒体文件选择前校验（不自动上传）
  const beforeMediaUpload = async (file) => {
    const ext = getFileExt(file.name)
    if (!ALL_MEDIA_TYPES.includes(ext)) {
      message.error(`不支持的格式，请上传 ${ALL_MEDIA_TYPES.join('/')} 文件`)
      return Upload.LIST_IGNORE
    }
    const sizeOk = file.size / 1024 / 1024 <= 100
    if (!sizeOk) {
      message.error('文件大小不能超过 100MB')
      return Upload.LIST_IGNORE
    }

    // 尝试读取时长
    try {
      const duration = await getAudioDuration(file)
      if (duration) {
        file._duration = duration
      }
    } catch { /* ignore */ }

    setMediaFileList([file])
    return false // 阻止自动上传
  }

  // 封面图选择前校验
  const beforeCoverUpload = (file) => {
    const ext = getFileExt(file.name)
    if (!IMAGE_TYPES.includes(ext)) {
      message.error(`请上传图片文件 (${IMAGE_TYPES.join('/')})`)
      return Upload.LIST_IGNORE
    }
    if (file.size / 1024 / 1024 > 10) {
      message.error('封面图不能超过 10MB')
      return Upload.LIST_IGNORE
    }
    setCoverFileList([file])
    return false
  }

  // 执行上传
  const handleDoUpload = async () => {
    if (!mediaFileList.length && !coverFileList.length) {
      message.warning('请至少选择一个文件进行上传')
      return
    }
    if (!uploadingSong) return

    setUploading(true)
    const done = { media: uploadDone.media, cover: uploadDone.cover }

    // 上传媒体文件
    if (mediaFileList.length) {
      try {
        const file = mediaFileList[0]
        const res = await uploadSongFile(uploadingSong.id, file, (e) => {
          if (e.total) setUploadProgress(Math.round((e.loaded / e.total) * 100))
        })
        message.success(`媒体文件上传成功：${res.data.fileName}`)
        done.media = true
        setUploadDone((prev) => ({ ...prev, media: true }))

        // Bug13修复：更新时长时需携带歌曲完整字段，否则 singerId=null 会触发后端业务异常
        if (file._duration) {
          try {
            await updateSong(uploadingSong.id, {
              name: uploadingSong.name,
              singerId: uploadingSong.singerId,
              categoryId: uploadingSong.categoryId,
              language: uploadingSong.language,
              status: uploadingSong.status,
              isHot: uploadingSong.isHot,
              isNew: uploadingSong.isNew,
              duration: file._duration,
            })
          } catch { /* ignore */ }
        }
      } catch (error) {
        message.error('媒体文件上传失败')
        console.error(error)
      }
    }

    // 上传封面图
    if (coverFileList.length) {
      try {
        const file = coverFileList[0]
        const res = await uploadCoverImage(uploadingSong.id, file, (e) => {
          if (e.total) setCoverProgress(Math.round((e.loaded / e.total) * 100))
        })
        message.success('封面图上传成功')
        done.cover = true
        setUploadDone((prev) => ({ ...prev, cover: true }))
      } catch (error) {
        message.error('封面图上传失败')
        console.error(error)
      }
    }

    setUploading(false)
    // Bug B6修复：上传完成后清空已选文件列表，防止用户重复提交同一文件
    if (done.media) setMediaFileList([])
    if (done.cover) setCoverFileList([])
    loadSongList()
  }

  // ========== 表格列定义 ==========

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
    },
    {
      title: '歌曲名',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '歌手',
      dataIndex: 'singerName',
      key: 'singerName',
      width: 130,
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 100,
    },
    {
      title: '语种',
      dataIndex: 'language',
      key: 'language',
      width: 70,
    },
    {
      title: '时长(秒)',
      dataIndex: 'duration',
      key: 'duration',
      width: 90,
    },
    {
      title: '播放次数',
      dataIndex: 'playCount',
      key: 'playCount',
      width: 90,
    },
    {
      title: '文件',
      dataIndex: 'filePath',
      key: 'filePath',
      width: 80,
      render: (filePath) => {
        if (!filePath) return <Tag color="default">未上传</Tag>
        if (isAudio(filePath)) return <Tag color="blue" icon={<SoundOutlined />}>音频</Tag>
        if (isVideo(filePath)) return <Tag color="purple" icon={<VideoCameraOutlined />}>视频</Tag>
        return <Tag color="green">已上传</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'error'}>
          {status === 1 ? '上架' : '下架'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<UploadOutlined />}
            onClick={() => openUploadModal(record)}
          >
            上传
          </Button>
          <Popconfirm
            title="确定删除该歌曲吗?"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      {/* 搜索栏 */}
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="歌曲名"
            value={queryParams.name}
            onChange={(e) =>
              setQueryParams({ ...queryParams, name: e.target.value })
            }
            style={{ width: 150 }}
            onPressEnter={handleSearch}
          />
          <Select
            placeholder="歌手"
            value={queryParams.singerId || undefined}
            onChange={(value) =>
              setQueryParams({ ...queryParams, singerId: value || '' })
            }
            options={singers.map((item) => ({
              value: item.id,
              label: item.name,
            }))}
            style={{ width: 150 }}
            allowClear
            showSearch
            filterOption={(input, option) =>
              (option.label || '').toLowerCase().includes(input.toLowerCase())
            }
          />
          <Select
            placeholder="分类"
            value={queryParams.categoryId || undefined}
            onChange={(value) =>
              setQueryParams({ ...queryParams, categoryId: value || '' })
            }
            options={categories.map((item) => ({
              value: item.id,
              label: item.name,
            }))}
            style={{ width: 120 }}
            allowClear
          />
          <Select
            placeholder="语种"
            value={queryParams.language || undefined}
            onChange={(value) =>
              setQueryParams({ ...queryParams, language: value || '' })
            }
            options={languageOptions.filter((item) => item.value !== '')}
            style={{ width: 100 }}
            allowClear
          />
          {/* BugB1配套：管理端现在可以看到下架歌曲，需提供状态筛选 */}
          <Select
            placeholder="状态"
            value={queryParams.status !== '' ? queryParams.status : undefined}
            onChange={(value) =>
              setQueryParams({ ...queryParams, status: value !== undefined ? value : '' })
            }
            options={[
              { value: 1, label: '上架' },
              { value: 0, label: '下架' },
            ]}
            style={{ width: 100 }}
            allowClear
          />
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </div>

      {/* 操作按钮 */}
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增歌曲
        </Button>
      </div>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey="id"
        loading={loading}
        scroll={{ x: 1100 }}
        pagination={{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (pageNum, pageSize) => {
            setQueryParams({ ...queryParams, pageNum, pageSize })
          },
        }}
      />

      {/* ========== 新增/编辑弹窗 ========== */}
      <Modal
        title={editingSong ? '编辑歌曲' : '新增歌曲'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        okText="提交"
        cancelText="取消"
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          autoComplete="off"
        >
          <Form.Item
            name="name"
            label="歌曲名"
            rules={[{ required: true, message: '请输入歌曲名' }]}
          >
            <Input placeholder="请输入歌曲名" />
          </Form.Item>

          <Form.Item
            name="singerId"
            label="歌手"
            rules={[{ required: true, message: '请选择歌手' }]}
          >
            <Select
              placeholder="请选择歌手"
              options={singers.map((item) => ({
                value: item.id,
                label: item.name,
              }))}
              showSearch
              filterOption={(input, option) =>
                (option.label || '').toLowerCase().indexOf(input.toLowerCase()) >= 0
              }
            />
          </Form.Item>

          <Form.Item name="categoryId" label="分类">
            <Select
              placeholder="请选择分类"
              options={categories.map((item) => ({
                value: item.id,
                label: item.name,
              }))}
              allowClear
            />
          </Form.Item>

          <Form.Item
            name="language"
            label="语种"
            rules={[{ required: true, message: '请选择语种' }]}
          >
            <Select
              placeholder="请选择语种"
              options={languageOptions.filter((item) => item.value !== '')}
            />
          </Form.Item>

          <Form.Item name="duration" label="时长(秒)">
            <InputNumber
              placeholder="上传文件后自动填充"
              min={0}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item name="filePath" label="文件路径">
            <Input placeholder="上传文件后自动填充" readOnly />
          </Form.Item>

          <Form.Item name="coverUrl" label="封面URL">
            <Input placeholder="上传封面后自动填充，或手动填写URL" />
          </Form.Item>

          {/* BugA8修复：SongDTO/SongVO 均有 lyricPath 字段（歌词文件路径），
              表单中缺少此字段，导致管理员无法手动填写或查看歌词路径 */}
          <Form.Item name="lyricPath" label="歌词路径">
            <Input placeholder="歌词文件路径（可选）" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
            initialValue={1}
          >
            <Select
              placeholder="请选择状态"
              options={[
                { value: 1, label: '上架' },
                { value: 0, label: '下架' },
              ]}
            />
          </Form.Item>

          <Form.Item name="isHot" label="是否热门" initialValue={0}>
            <Select
              options={[
                { value: 0, label: '否' },
                { value: 1, label: '是' },
              ]}
            />
          </Form.Item>

          <Form.Item name="isNew" label="是否新歌" initialValue={0}>
            <Select
              options={[
                { value: 0, label: '否' },
                { value: 1, label: '是' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* ========== 文件上传弹窗 ========== */}
      <Modal
        title={
          <Space>
            <UploadOutlined />
            <span>
              上传文件
              {uploadingSong && (
                <span style={{ color: '#1677ff', marginLeft: 8 }}>
                  — {uploadingSong.name}
                </span>
              )}
            </span>
          </Space>
        }
        open={uploadModalVisible}
        onCancel={() => {
          if (uploading) return
          setUploadModalVisible(false)
        }}
        footer={[
          <Button
            key="cancel"
            onClick={() => setUploadModalVisible(false)}
            disabled={uploading}
          >
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
        width={560}
        maskClosable={false}
      >
        {/* 媒体文件上传区域 */}
        <div style={{ marginBottom: 24 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            <SoundOutlined style={{ marginRight: 6, color: '#1677ff' }} />
            音视频文件
            <span style={{ color: '#999', fontWeight: 400, marginLeft: 8, fontSize: 12 }}>
              支持 MP3 / FLAC / WAV / OGG / M4A / MP4（最大 500MB）
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
            <p className="ant-upload-text">点击或拖拽文件到此处</p>
            <p className="ant-upload-hint">上传后将自动读取并填充歌曲时长</p>
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

        <Divider style={{ margin: '8px 0 20px' }} />

        {/* 封面图上传区域 */}
        <div>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            <PictureOutlined style={{ marginRight: 6, color: '#722ed1' }} />
            封面图片
            <span style={{ color: '#999', fontWeight: 400, marginLeft: 8, fontSize: 12 }}>
              支持 JPG / PNG / GIF / WebP（最大 10MB）
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
            <p className="ant-upload-text">点击或拖拽封面图到此处</p>
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
              <CheckCircleOutlined /> 封面图上传成功
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}

export default Song
