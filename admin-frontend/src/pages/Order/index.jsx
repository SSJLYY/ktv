import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Input,
  Select,
  Modal,
  Form,
  message,
  Tag,
  DatePicker,
  Descriptions,
} from 'antd' // Bug18修复：补充 Input 导入，弹窗备注字段使用了 Input.TextArea
import { PlusOutlined, CheckOutlined, EyeOutlined } from '@ant-design/icons'
import {
  getOrderList,
  getOrderById,
  openOrder,
  closeOrder,
  cancelOrder,
} from '../../api/order'
import { getAvailableRooms } from '../../api/room'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker

const Order = () => {
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
  // Bug B4修复：RangePicker 需要受控 value，重置时同步清空日期选择器显示
  const [dateRange, setDateRange] = useState(null)
  const [openModalVisible, setOpenModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentOrder, setCurrentOrder] = useState(null)
  const [openForm] = Form.useForm()
  const [availableRooms, setAvailableRooms] = useState([])

  // 状态选项
  const statusOptions = [
    { value: '', label: '全部' },
    { value: 1, label: '消费中' },
    { value: 2, label: '已结账' },
    { value: 3, label: '已取消' },
  ]

  // 状态颜色映射
  const statusColorMap = {
    1: 'processing',
    2: 'success',
    3: 'default',
  }

  // 状态文本映射
  const statusTextMap = {
    1: '消费中',
    2: '已结账',
    3: '已取消',
  }

  // 加载空闲包厢列表
  const loadAvailableRooms = async () => {
    try {
      const res = await getAvailableRooms()
      setAvailableRooms(res.data || [])
    } catch (error) {
      console.error('加载空闲包厢失败:', error)
    }
  }

  useEffect(() => {
    loadAvailableRooms()
  }, [])

  // 加载订单列表
  const loadOrderList = async () => {
    try {
      setLoading(true)
      const res = await getOrderList(queryParams)
      setDataSource(res.data.records || [])
      setTotal(res.data.total || 0)
    } catch (error) {
      console.error('加载订单列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadOrderList()
  }, [queryParams])

  // 搜索
  const handleSearch = () => {
    setQueryParams({ ...queryParams, pageNum: 1 })
  }

  // 重置
  const handleReset = () => {
    setDateRange(null) // Bug B4修复：同步清空受控日期选择器
    setQueryParams({
      pageNum: 1,
      pageSize: 10,
      startDate: '',
      endDate: '',
      status: '',
    })
  }

  // 日期范围变化
  // BugA3修复：后端 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") 要求完整日期时间格式
  // 前端只传 YYYY-MM-DD 会导致 400 Bad Request（Spring 无法解析为 LocalDateTime）
  // 修复：开始日期补 00:00:00，结束日期补 23:59:59
  const handleDateChange = (dates) => {
    setDateRange(dates) // Bug B4修复：同步更新受控状态
    if (dates && dates.length === 2) {
      setQueryParams({
        ...queryParams,
        startDate: dates[0].format('YYYY-MM-DD') + ' 00:00:00',
        endDate: dates[1].format('YYYY-MM-DD') + ' 23:59:59',
      })
    } else {
      setQueryParams({
        ...queryParams,
        startDate: '',
        endDate: '',
      })
    }
  }

  // 打开开台弹窗
  // BugA16修复：每次打开弹窗前重新加载空闲包厢列表，
  // 避免其他操作员刚刚开台导致包厢已被占用但下拉仍显示空闲的情况
  const handleOpenOrder = async () => {
    openForm.resetFields()
    await loadAvailableRooms()
    setOpenModalVisible(true)
  }

  // 提交开台
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
      loadAvailableRooms() // 刷新空闲包厢列表
    } catch (error) {
      console.error('开台失败:', error)
    }
  }

  // 结账
  const handleCloseOrder = async (id) => {
    Modal.confirm({
      title: '确认结账',
      content: '确定要结账吗?',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await closeOrder(id)
          message.success('结账成功')
          loadOrderList()
          loadAvailableRooms()
        } catch (error) {
          console.error('结账失败:', error)
        }
      },
    })
  }

  // 取消订单
  const handleCancelOrder = async (id) => {
    Modal.confirm({
      title: '确认取消',
      content: '确定要取消该订单吗?',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await cancelOrder(id)
          message.success('取消成功')
          loadOrderList()
          loadAvailableRooms()
        } catch (error) {
          console.error('取消失败:', error)
        }
      },
    })
  }

  // 查看详情
  const handleViewDetail = async (id) => {
    try {
      const res = await getOrderById(id)
      setCurrentOrder(res.data)
      setDetailModalVisible(true)
    } catch (error) {
      console.error('获取订单详情失败:', error)
    }
  }

  // 表格列定义
  const columns = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 180,
    },
    {
      title: '包厢名',
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
      // Bug修复：消费中的订单 totalAmount 为 null，需做空值保护
      render: (amount) => (amount != null ? `¥${amount}` : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => (
        <Tag color={statusColorMap[status]}>
          {statusTextMap[status]}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.id)}
          >
            详情
          </Button>
          {record.status === 1 && (
            <>
              <Button
                type="link"
                style={{ color: '#52c41a' }}
                icon={<CheckOutlined />}
                onClick={() => handleCloseOrder(record.id)}
              >
                结账
              </Button>
              <Button
                type="link"
                danger
                onClick={() => handleCancelOrder(record.id)}
              >
                取消
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      {/* 搜索栏 */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <RangePicker
            value={dateRange}
            onChange={handleDateChange}
            style={{ width: 260 }}
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenOrder}>
          开台
        </Button>
      </div>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey="id"
        loading={loading}
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

      {/* 开台弹窗 */}
      <Modal
        title="开台"
        open={openModalVisible}
        onOk={handleSubmitOpen}
        onCancel={() => setOpenModalVisible(false)}
        okText="确定"
        cancelText="取消"
      >
        <Form
          form={openForm}
          layout="vertical"
        >
          <Form.Item
            name="roomId"
            label="选择包厢"
            rules={[{ required: true, message: '请选择包厢' }]}
          >
            <Select
              placeholder="请选择空闲包厢"
              options={availableRooms.map((room) => ({
                value: room.id,
                label: `${room.name} (${room.type} - ¥${room.pricePerHour}/小时)`,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="remark"
            label="备注"
          >
            <Input.TextArea
              placeholder="请输入备注"
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 订单详情弹窗 */}
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
            <Descriptions.Item label="包厢名">
              {currentOrder.roomName}
            </Descriptions.Item>
            <Descriptions.Item label="包厢类型">
              {currentOrder.roomType}
            </Descriptions.Item>
            <Descriptions.Item label="开台时间">
              {currentOrder.startTime}
            </Descriptions.Item>
            <Descriptions.Item label="结账时间">
              {currentOrder.endTime || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="消费时长">
              {currentOrder.durationDesc || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="总费用">
              {currentOrder.totalAmount != null ? `¥${currentOrder.totalAmount}` : '-'}
            </Descriptions.Item>
            {/* BugC4修复：补充包厢费用展示，OrderVO 有 roomAmount 字段但未展示 */}
            <Descriptions.Item label="包厢费用">
              {currentOrder.roomAmount != null ? `¥${currentOrder.roomAmount}` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusColorMap[currentOrder.status]}>
                {statusTextMap[currentOrder.status]}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="操作员">
              {currentOrder.operatorName || '-'}
            </Descriptions.Item>
            {/* BugC3修复：详情弹窗遗漏了结账操作员（closerName），OrderVO 有此字段 */}
            {currentOrder.status === 2 && (
              <Descriptions.Item label="结账操作员">
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
