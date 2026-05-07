import React, { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Form,
  Input,
  InputNumber,
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
  addRoom,
  deleteRoom,
  getRoomById,
  getRoomList,
  updateRoom,
  updateRoomStatus,
} from '../../api/room'
import { useUserStore } from '../../store/userStore'

const roomTypeOptions = [
  { value: '', label: '全部' },
  { value: '小包', label: '小包' },
  { value: '中包', label: '中包' },
  { value: '大包', label: '大包' },
  { value: '豪华包', label: '豪华包' },
]

const statusOptions = [
  { value: '', label: '全部' },
  { value: 0, label: '空闲' },
  { value: 1, label: '使用中' },
  { value: 2, label: '清洁中' },
  { value: 3, label: '维修中' },
]

const roomStatusMeta = {
  0: { color: 'success', text: '空闲' },
  1: { color: 'error', text: '使用中' },
  2: { color: 'warning', text: '清洁中' },
  3: { color: 'default', text: '维修中' },
}

const emptyFilters = {
  type: '',
  status: '',
}

const Room = () => {
  const userInfo = useUserStore((state) => state.userInfo)
  const isSuperAdmin = userInfo?.role === 'super_admin'

  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [queryParams, setQueryParams] = useState(emptyFilters)
  const [modalVisible, setModalVisible] = useState(false)
  const [statusModalVisible, setStatusModalVisible] = useState(false)
  const [editingRoom, setEditingRoom] = useState(null)
  const [form] = Form.useForm()
  const [statusForm] = Form.useForm()

  const loadRoomList = useCallback(async () => {
    try {
      setLoading(true)
      const res = await getRoomList(queryParams)
      setDataSource(res.data || [])
    } catch (error) {
      console.error('Load room list failed:', error)
    } finally {
      setLoading(false)
    }
  }, [queryParams])

  useEffect(() => {
    loadRoomList()
  }, [loadRoomList])

  const closeFormModal = () => {
    setModalVisible(false)
    setEditingRoom(null)
    form.resetFields()
  }

  const closeStatusModal = () => {
    setStatusModalVisible(false)
    setEditingRoom(null)
    statusForm.resetFields()
  }

  const handleReset = () => {
    setQueryParams(emptyFilters)
  }

  const handleAdd = () => {
    setEditingRoom(null)
    form.resetFields()
    form.setFieldsValue({
      type: '小包',
      capacity: 1,
      pricePerHour: 0,
      minConsumption: 0,
    })
    setModalVisible(true)
  }

  const handleEdit = async (record) => {
    try {
      const res = await getRoomById(record.id)
      setEditingRoom(res.data)
      form.resetFields()
      form.setFieldsValue({
        ...res.data,
        minConsumption: res.data?.minConsumption ?? undefined,
      })
      setModalVisible(true)
    } catch (error) {
      console.error('Load room detail failed:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteRoom(id)
      message.success('包厢删除成功')
      await loadRoomList()
    } catch (error) {
      console.error('Delete room failed:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingRoom) {
        await updateRoom(editingRoom.id, values)
        message.success('包厢更新成功')
      } else {
        await addRoom(values)
        message.success('包厢创建成功')
      }
      closeFormModal()
      await loadRoomList()
    } catch (error) {
      console.error('Submit room failed:', error)
    }
  }

  const handleOpenStatusModal = (record) => {
    setEditingRoom(record)
    statusForm.setFieldsValue({ status: record.status })
    setStatusModalVisible(true)
  }

  const handleSubmitStatus = async () => {
    try {
      const values = await statusForm.validateFields()
      await updateRoomStatus(editingRoom.id, values.status)
      message.success('包厢状态更新成功')
      closeStatusModal()
      await loadRoomList()
    } catch (error) {
      console.error('Update room status failed:', error)
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
      title: '包厢名称',
      dataIndex: 'name',
      key: 'name',
      width: 160,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
    },
    {
      title: '容纳人数',
      dataIndex: 'capacity',
      key: 'capacity',
      width: 100,
    },
    {
      title: '每小时价格',
      dataIndex: 'pricePerHour',
      key: 'pricePerHour',
      width: 120,
      render: (value) => `￥${value ?? 0}`,
    },
    {
      title: '最低消费',
      dataIndex: 'minConsumption',
      key: 'minConsumption',
      width: 120,
      render: (value) => (value == null ? '-' : `￥${value}`),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const meta = roomStatusMeta[status] || { color: 'default', text: '未知' }
        return <Tag color={meta.color}>{meta.text}</Tag>
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
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
            <Button type="link" onClick={() => handleOpenStatusModal(record)}>
              修改状态
            </Button>
          )}
          {isSuperAdmin && (
            <Popconfirm
              title="确定删除这个包厢吗？"
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
          <Select
            placeholder="类型"
            value={queryParams.type || undefined}
            onChange={(value) => setQueryParams((prev) => ({ ...prev, type: value || '' }))}
            options={roomTypeOptions}
            allowClear
            style={{ width: 140 }}
          />
          <Select
            placeholder="状态"
            value={queryParams.status === '' ? undefined : queryParams.status}
            onChange={(value) =>
              setQueryParams((prev) => ({ ...prev, status: value === undefined ? '' : value }))
            }
            options={statusOptions}
            allowClear
            style={{ width: 140 }}
          />
          <Button type="primary" onClick={loadRoomList}>
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </div>

      {isSuperAdmin && (
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增包厢
          </Button>
        </div>
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1100 }}
      />

      <Modal
        title={editingRoom ? '编辑包厢' : '新增包厢'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={closeFormModal}
        okText="提交"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <Form.Item
            name="name"
            label="包厢名称"
            rules={[{ required: true, message: '请输入包厢名称' }]}
          >
            <Input placeholder="请输入包厢名称" />
          </Form.Item>

          <Form.Item
            name="type"
            label="类型"
            rules={[{ required: true, message: '请选择包厢类型' }]}
          >
            <Select
              placeholder="请选择包厢类型"
              options={roomTypeOptions.filter((item) => item.value)}
            />
          </Form.Item>

          <Form.Item
            name="capacity"
            label="容纳人数"
            rules={[{ required: true, message: '请输入容纳人数' }]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="pricePerHour"
            label="每小时价格"
            rules={[{ required: true, message: '请输入每小时价格' }]}
          >
            <InputNumber min={0} precision={2} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="minConsumption" label="最低消费">
            <InputNumber
              min={0}
              precision={2}
              style={{ width: '100%' }}
              placeholder="留空表示不限"
            />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入包厢描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改包厢状态"
        open={statusModalVisible}
        onOk={handleSubmitStatus}
        onCancel={closeStatusModal}
        okText="确定"
        cancelText="取消"
      >
        <Form form={statusForm} layout="vertical">
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择包厢状态' }]}
          >
            <Select
              placeholder="请选择包厢状态"
              options={statusOptions.filter((item) => item.value !== '')}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Room
