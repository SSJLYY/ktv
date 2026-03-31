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
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import {
  getRoomList,
  getRoomById,
  addRoom,
  updateRoom,
  deleteRoom,
  updateRoomStatus,
} from '../../api/room'

const Room = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  // BugA1修复：后端 RoomController.getRoomList 返回 List<RoomVO>（非分页），无需 total/pageNum/pageSize
  // 去掉分页参数，只保留筛选参数 type/status
  const [queryParams, setQueryParams] = useState({
    type: '',
    status: '',
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [statusModalVisible, setStatusModalVisible] = useState(false)
  const [editingRoom, setEditingRoom] = useState(null)
  const [form] = Form.useForm()
  const [statusForm] = Form.useForm()

  // 包厢类型选项
  const typeOptions = [
    { value: '', label: '全部' },
    { value: '小包', label: '小包' },
    { value: '中包', label: '中包' },
    { value: '大包', label: '大包' },
    { value: '豪华包', label: '豪华包' },
  ]

  // 状态选项
  const statusOptions = [
    { value: '', label: '全部' },
    { value: 0, label: '空闲' },
    { value: 1, label: '使用中' },
    { value: 2, label: '清洁中' },
    { value: 3, label: '维修中' },
  ]

  // 状态颜色映射
  const statusColorMap = {
    0: 'success',
    1: 'error',
    2: 'warning',
    3: 'default',
  }

  // 状态文本映射
  const statusTextMap = {
    0: '空闲',
    1: '使用中',
    2: '清洁中',
    3: '维修中',
  }

  // 加载包厢列表
  // BugA1修复：后端返回 List<RoomVO>（res.data 是数组），不是 {records, total} 的分页结构
  const loadRoomList = async () => {
    try {
      setLoading(true)
      const res = await getRoomList(queryParams)
      setDataSource(res.data || [])
    } catch (error) {
      console.error('加载包厢列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRoomList()
  }, [queryParams])

  // 搜索
  const handleSearch = () => {
    // BugA1修复：后端不支持分页，搜索只需刷新列表
    loadRoomList()
  }

  // 重置
  const handleReset = () => {
    setQueryParams({
      type: '',
      status: '',
    })
  }

  // 打开新增弹窗
  const handleAdd = () => {
    setEditingRoom(null)
    form.resetFields()
    setModalVisible(true)
  }

  // 打开编辑弹窗
  const handleEdit = async (record) => {
    try {
      const res = await getRoomById(record.id)
      setEditingRoom(res.data)
      // BugC1修复：先 resetFields 清空上一次的表单值，再 setFieldsValue 填充
      form.resetFields()
      form.setFieldsValue(res.data)
      setModalVisible(true)
    } catch (error) {
      console.error('获取包厢详情失败:', error)
    }
  }

  // 删除
  const handleDelete = async (id) => {
    try {
      await deleteRoom(id)
      message.success('删除成功')
      loadRoomList()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      if (editingRoom) {
        await updateRoom(editingRoom.id, values)
      } else {
        await addRoom(values)
      }
      
      message.success(editingRoom ? '修改成功' : '新增成功')
      setModalVisible(false)
      loadRoomList()
    } catch (error) {
      console.error('提交失败:', error)
    }
  }

  // 打开状态修改弹窗
  const handleChangeStatus = (record) => {
    setEditingRoom(record)
    statusForm.setFieldsValue({ status: record.status })
    setStatusModalVisible(true)
  }

  // 提交状态修改
  const handleSubmitStatus = async () => {
    try {
      const values = await statusForm.validateFields()
      await updateRoomStatus(editingRoom.id, values.status)
      message.success('状态修改成功')
      setStatusModalVisible(false)
      loadRoomList()
    } catch (error) {
      console.error('状态修改失败:', error)
    }
  }

  // 表格列定义
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '包厢名',
      dataIndex: 'name',
      key: 'name',
      width: 150,
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
      render: (price) => `¥${price}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => (
        <Tag color={statusColorMap[status]}>
          {statusTextMap[status]}
        </Tag>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
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
            onClick={() => handleChangeStatus(record)}
          >
            修改状态
          </Button>
          <Popconfirm
            title="确定删除该包厢吗?"
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
        <Space>
          <Select
            placeholder="类型"
            value={queryParams.type}
            onChange={(value) =>
              setQueryParams({ ...queryParams, type: value })
            }
            options={typeOptions}
            style={{ width: 120 }}
          />
          <Select
            placeholder="状态"
            value={queryParams.status}
            onChange={(value) =>
              setQueryParams({ ...queryParams, status: value })
            }
            options={statusOptions}
            style={{ width: 120 }}
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
          新增包厢
        </Button>
      </div>

      {/* 表格 */}
      {/* BugA1修复：后端返回 List（非分页），不使用分页组件；数据量通常不大，直接展示全部 */}
      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingRoom ? '编辑包厢' : '新增包厢'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        okText="提交"
        cancelText="取消"
      >
        <Form
          form={form}
          layout="vertical"
          autoComplete="off"
        >
          <Form.Item
            name="name"
            label="包厢名"
            rules={[{ required: true, message: '请输入包厢名' }]}
          >
            <Input placeholder="请输入包厢名" />
          </Form.Item>

          <Form.Item
            name="type"
            label="类型"
            rules={[{ required: true, message: '请选择类型' }]}
          >
            <Select
              placeholder="请选择类型"
              options={typeOptions.filter((item) => item.value !== '')}
            />
          </Form.Item>

          <Form.Item
            name="capacity"
            label="容纳人数"
            rules={[{ required: true, message: '请输入容纳人数' }]}
          >
            <InputNumber
              placeholder="请输入容纳人数"
              min={1}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            name="pricePerHour"
            label="每小时价格"
            rules={[{ required: true, message: '请输入价格' }]}
          >
            <InputNumber
              placeholder="请输入价格"
              min={0}
              precision={2}
              style={{ width: '100%' }}
            />
          </Form.Item>

          {/* BugA7修复：t_room 表有 min_consumption 列，RoomDTO/RoomVO 均有 minConsumption 字段
              编辑弹窗缺少此字段，导致管理员无法设置或修改最低消费金额 */}
          <Form.Item
            name="minConsumption"
            label="最低消费（元）"
          >
            <InputNumber
              placeholder="请输入最低消费，留空表示不限"
              min={0}
              precision={2}
              style={{ width: '100%' }}
            />
          </Form.Item>

          {/* BugA10修复：编辑弹窗移除 status 字段
              理由：状态变更有专门的"修改状态"按钮和流程，
              若在编辑信息时同时允许改状态会绕过状态流转检查（如把"使用中"改回"空闲"）
              状态应通过右侧"修改状态"按钮操作 */}

          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea
              placeholder="请输入描述"
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 状态修改弹窗 */}
      <Modal
        title="修改包厢状态"
        open={statusModalVisible}
        onOk={handleSubmitStatus}
        onCancel={() => setStatusModalVisible(false)}
        okText="确定"
        cancelText="取消"
      >
        <Form
          form={statusForm}
          layout="vertical"
        >
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select
              placeholder="请选择状态"
              options={statusOptions.filter((item) => item.value !== '')}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Room
