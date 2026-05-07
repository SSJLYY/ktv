import React, { useCallback, useEffect, useState } from 'react'
import {
  Table,
  Button,
  Space,
  Select,
  Modal,
  Form,
  message,
  Tag,
  DatePicker,
  Descriptions,
  Input,
} from 'antd'
import { PlusOutlined, CheckOutlined, EyeOutlined } from '@ant-design/icons'
import {
  getOrderList,
  getOrderById,
  openOrder,
  closeOrder,
  cancelOrder,
} from '../../api/order'
import { getAvailableRooms } from '../../api/room'
import { useUserStore } from '../../store/userStore'

const { RangePicker } = DatePicker

const statusOptions = [
  { value: '', label: '全部' },
  { value: 1, label: '消费中' },
  { value: 2, label: '已结账' },
  { value: 3, label: '已取消' },
]

const statusColorMap = {
  1: 'processing',
  2: 'success',
  3: 'default',
}

const statusTextMap = {
  1: '消费中',
  2: '已结账',
  3: '已取消',
}

const Order = () => {
  const userInfo = useUserStore((state) => state.userInfo)
  const isSuperAdmin = userInfo?.role === 'super_admin'
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [queryParams, setQueryParams] = useState({
    pageNum: 1,
    pageSize: 10,
    startDate: '',
    endDate: '',
    status: '',
  })
  const [dateRange, setDateRange] = useState(null)
  const [openModalVisible, setOpenModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [openForm] = Form.useForm()
  const [availableRooms, setAvailableRooms] = useState([])

  const loadAvailableRooms = useCallback(async () => {
    try {
      const res = await getAvailableRooms()
      setAvailableRooms(res.data || [])
    } catch (error) {
      console.error('Load available rooms failed:', error)
    }
  }, [])

  useEffect(() => {
    loadAvailableRooms()
  }, [loadAvailableRooms])

  const loadOrderList = useCallback(async () => {
    try {
      setLoading(true)
      const res = await getOrderList(queryParams)
      setDataSource(res.data?.records || [])
      setTotal(res.data?.total || 0)
    } catch (error) {
      console.error('Load order list failed:', error)
    } finally {
      setLoading(false)
    }
  }, [queryParams])

  useEffect(() => {
    loadOrderList()
  }, [loadOrderList])

  const handleSearch = () => {
    setQueryParams((prev) => ({ ...prev, pageNum: 1 }))
  }

  const handleReset = () => {
    setDateRange(null)
    setQueryParams({
      pageNum: 1,
      pageSize: 10,
      startDate: '',
      endDate: '',
      status: '',
    })
  }

  const handleDateChange = (dates) => {
    setDateRange(dates)
    if (dates && dates.length === 2) {
      setQueryParams((prev) => ({
        ...prev,
        pageNum: 1,
        startDate: `${dates[0].format('YYYY-MM-DD')} 00:00:00`,
        endDate: `${dates[1].format('YYYY-MM-DD')} 23:59:59`,
      }))
      return
    }
    setQueryParams((prev) => ({
      ...prev,
      pageNum: 1,
      startDate: '',
      endDate: '',
    }))
  }

  const handleOpenOrder = async () => {
    openForm.resetFields()
    await loadAvailableRooms()
    setOpenModalVisible(true)
  }

  const handleSubmitOpen = async () => {
    try {
      const values = await openForm.validateFields()
      await openOrder({
        roomId: values.roomId,
        remark: values.remark,
      })
      message.success('开台成功')
      setOpenModalVisible(false)
      loadOrderList()
      loadAvailableRooms()
    } catch (error) {
      console.error('Open order failed:', error)
    }
  }

  const handleCloseOrder = async (id) => {
    Modal.confirm({
      title: '确认结账',
      content: '确定要结账吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await closeOrder(id)
          message.success('结账成功')
          loadOrderList()
          loadAvailableRooms()
        } catch (error) {
          console.error('Close order failed:', error)
        }
      },
    })
  }

  const handleCancelOrder = async (id) => {
    Modal.confirm({
      title: '确认取消',
      content: '确定要取消该订单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await cancelOrder(id)
          message.success('取消成功')
          loadOrderList()
          loadAvailableRooms()
        } catch (error) {
          console.error('Cancel order failed:', error)
        }
      },
    })
  }

  const handleViewDetail = async (id) => {
    try {
      const res = await getOrderById(id)
      setCurrentOrder(res.data)
      setDetailModalVisible(true)
    } catch (error) {
      console.error('Load order detail failed:', error)
    }
  }

  const columns = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 180,
    },
    {
      title: '包厢名称',
      dataIndex: 'roomName',
      key: 'roomName',
      width: 120,
    },
    {
      title: '包厢类型',
      dataIndex: 'roomType',
      key: 'roomType',
      width: 100,
    },
    {
      title: '开台时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 160,
    },
    {
      title: '结账时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 160,
      render: (time) => time || '-',
    },
    {
      title: '消费时长',
      dataIndex: 'durationDesc',
      key: 'durationDesc',
      width: 120,
      render: (text) => text || '-',
    },
    {
      title: '总费用',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 100,
      render: (amount) => (amount != null ? `￥${amount}` : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => <Tag color={statusColorMap[status]}>{statusTextMap[status]}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EyeOutlined />} onClick={() => handleViewDetail(record.id)}>
            详情
          </Button>
          {record.status === 1 && isSuperAdmin && (
            <Button
              type="link"
              style={{ color: '#52c41a' }}
              icon={<CheckOutlined />}
              onClick={() => handleCloseOrder(record.id)}
            >
              结账
            </Button>
          )}
          {record.status === 1 && isSuperAdmin && (
            <Button type="link" danger onClick={() => handleCancelOrder(record.id)}>
              取消
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <RangePicker value={dateRange} onChange={handleDateChange} style={{ width: 260 }} />
          <Select
            placeholder="状态"
            value={queryParams.status}
            onChange={(value) => setQueryParams((prev) => ({ ...prev, pageNum: 1, status: value }))}
            options={statusOptions}
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
          <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenOrder}>
            开台
          </Button>
        </div>
      )}

      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey="id"
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
        title="开台"
        open={openModalVisible}
        onOk={handleSubmitOpen}
        onCancel={() => setOpenModalVisible(false)}
        okText="确定"
        cancelText="取消"
      >
        <Form form={openForm} layout="vertical">
          <Form.Item
            name="roomId"
            label="选择包厢"
            rules={[{ required: true, message: '请选择包厢' }]}
          >
            <Select
              placeholder="请选择空闲包厢"
              options={availableRooms.map((room) => ({
                value: room.id,
                label: `${room.name} (${room.type} - ￥${room.pricePerHour}/小时)`,
              }))}
            />
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="请输入备注" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="订单详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={600}
      >
        {currentOrder && (
          <Descriptions column={2} bordered>
            <Descriptions.Item label="订单号" span={2}>
              {currentOrder.orderNo}
            </Descriptions.Item>
            <Descriptions.Item label="包厢名称">{currentOrder.roomName}</Descriptions.Item>
            <Descriptions.Item label="包厢类型">{currentOrder.roomType}</Descriptions.Item>
            <Descriptions.Item label="开台时间">{currentOrder.startTime}</Descriptions.Item>
            <Descriptions.Item label="结账时间">{currentOrder.endTime || '-'}</Descriptions.Item>
            <Descriptions.Item label="消费时长">{currentOrder.durationDesc || '-'}</Descriptions.Item>
            <Descriptions.Item label="总费用">
              {currentOrder.totalAmount != null ? `￥${currentOrder.totalAmount}` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="包厢费用">
              {currentOrder.roomAmount != null ? `￥${currentOrder.roomAmount}` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusColorMap[currentOrder.status]}>{statusTextMap[currentOrder.status]}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="操作员">{currentOrder.operatorName || '-'}</Descriptions.Item>
            {(currentOrder.status === 2 || currentOrder.status === 3) && (
              <Descriptions.Item label={currentOrder.status === 2 ? '结账操作员' : '取消操作员'}>
                {currentOrder.closerName || '-'}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="备注" span={2}>
              {currentOrder.remark || '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default Order
