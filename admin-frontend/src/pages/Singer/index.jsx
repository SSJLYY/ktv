import React, { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import {
  addSinger,
  deleteSinger,
  getSingerById,
  getSingerList,
  updateSinger,
} from '../../api/singer'
import { useUserStore } from '../../store/userStore'

const regionOptions = [
  { value: '', label: '全部' },
  { value: '内地', label: '内地' },
  { value: '港台', label: '港台' },
  { value: '欧美', label: '欧美' },
  { value: '日韩', label: '日韩' },
  { value: '其他', label: '其他' },
]

const genderOptions = [
  { value: 0, label: '未知' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
  { value: 3, label: '组合' },
]

const genderMap = {
  0: { label: '未知', color: 'default' },
  1: { label: '男', color: 'blue' },
  2: { label: '女', color: 'pink' },
  3: { label: '组合', color: 'purple' },
}

const defaultQueryParams = {
  pageNum: 1,
  pageSize: 10,
  name: '',
  region: '',
}

const Singer = () => {
  const userInfo = useUserStore((state) => state.userInfo)
  const isSuperAdmin = userInfo?.role === 'super_admin'

  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [queryParams, setQueryParams] = useState(defaultQueryParams)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingSinger, setEditingSinger] = useState(null)
  const [form] = Form.useForm()

  const loadSingerList = useCallback(async () => {
    try {
      setLoading(true)
      const res = await getSingerList(queryParams)
      setDataSource(res.data?.records || [])
      setTotal(res.data?.total || 0)
    } catch (error) {
      console.error('Load singer list failed:', error)
    } finally {
      setLoading(false)
    }
  }, [queryParams])

  useEffect(() => {
    loadSingerList()
  }, [loadSingerList])

  const closeModal = () => {
    setModalVisible(false)
    setEditingSinger(null)
    form.resetFields()
  }

  const handleSearch = () => {
    setQueryParams((prev) => ({ ...prev, pageNum: 1 }))
  }

  const handleReset = () => {
    setQueryParams(defaultQueryParams)
  }

  const handleAdd = () => {
    setEditingSinger(null)
    form.resetFields()
    form.setFieldsValue({
      gender: 0,
      region: '内地',
      status: 1,
    })
    setModalVisible(true)
  }

  const handleEdit = async (record) => {
    try {
      const res = await getSingerById(record.id)
      setEditingSinger(res.data)
      form.resetFields()
      form.setFieldsValue({
        ...res.data,
        gender: res.data?.gender ?? 0,
        status: res.data?.status ?? 1,
      })
      setModalVisible(true)
    } catch (error) {
      console.error('Load singer detail failed:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteSinger(id)
      message.success('歌手删除成功')
      await loadSingerList()
    } catch (error) {
      console.error('Delete singer failed:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingSinger) {
        await updateSinger(editingSinger.id, values)
        message.success('歌手更新成功')
      } else {
        await addSinger(values)
        message.success('歌手创建成功')
      }
      closeModal()
      await loadSingerList()
    } catch (error) {
      console.error('Submit singer failed:', error)
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '歌手名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
    },
    {
      title: '性别',
      dataIndex: 'gender',
      key: 'gender',
      width: 100,
      render: (gender) => {
        const current = genderMap[gender] || genderMap[0]
        return <Tag color={current.color}>{current.label}</Tag>
      },
    },
    {
      title: '地区',
      dataIndex: 'region',
      key: 'region',
      width: 120,
      render: (value) => value || '-',
    },
    {
      title: '歌曲数量',
      dataIndex: 'songCount',
      key: 'songCount',
      width: 120,
      render: (value) => value ?? 0,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'error'}>{status === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          {isSuperAdmin && (
            <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
              编辑
            </Button>
          )}
          {isSuperAdmin && (
            <Popconfirm
              title="确定删除这个歌手吗？"
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
            placeholder="歌手名称"
            value={queryParams.name}
            onChange={(e) => setQueryParams((prev) => ({ ...prev, name: e.target.value }))}
            onPressEnter={handleSearch}
            style={{ width: 200 }}
          />
          <Select
            placeholder="地区"
            value={queryParams.region || undefined}
            onChange={(value) => setQueryParams((prev) => ({ ...prev, region: value || '' }))}
            options={regionOptions}
            allowClear
            style={{ width: 140 }}
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
            新增歌手
          </Button>
        </div>
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
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
        title={editingSinger ? '编辑歌手' : '新增歌手'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={closeModal}
        okText="提交"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <Form.Item
            name="name"
            label="歌手名称"
            rules={[{ required: true, message: '请输入歌手名称' }]}
          >
            <Input placeholder="请输入歌手名称" />
          </Form.Item>

          <Form.Item
            name="gender"
            label="性别"
            rules={[{ required: true, message: '请选择性别' }]}
          >
            <Select placeholder="请选择性别" options={genderOptions} />
          </Form.Item>

          <Form.Item
            name="region"
            label="地区"
            rules={[{ required: true, message: '请选择地区' }]}
          >
            <Select
              placeholder="请选择地区"
              options={regionOptions.filter((item) => item.value)}
            />
          </Form.Item>

          <Form.Item name="avatar" label="头像 URL">
            <Input placeholder="请输入头像 URL" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select
              placeholder="请选择状态"
              options={[
                { value: 1, label: '启用' },
                { value: 0, label: '禁用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Singer
