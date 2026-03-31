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
  getSingerList,
  getSingerById,
  addSinger,
  updateSinger,
  deleteSinger,
} from '../../api/singer'

const Singer = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [queryParams, setQueryParams] = useState({
    pageNum: 1,
    pageSize: 10,
    name: '',
    region: '',
  })
  const [modalVisible, setModalVisible] = useState(false)
  const [editingSinger, setEditingSinger] = useState(null)
  const [form] = Form.useForm()

  // 地区选项
  const regionOptions = [
    { value: '', label: '全部' },
    { value: '内地', label: '内地' },
    { value: '港台', label: '港台' },
    { value: '欧美', label: '欧美' },
    { value: '日韩', label: '日韩' },
    { value: '其他', label: '其他' },
  ]

  // 性别选项
  const genderOptions = [
    { value: 0, label: '未知' },
    { value: 1, label: '男' },
    { value: 2, label: '女' },
    { value: 3, label: '组合' },
  ]

  // 加载歌手列表
  const loadSingerList = async () => {
    try {
      setLoading(true)
      const res = await getSingerList(queryParams)
      // Bug B8修复：request 拦截器已保证只有成功时才 resolve，无需再判断 res.code
      setDataSource(res.data.records || [])
      setTotal(res.data.total || 0)
    } catch (error) {
      console.error('加载歌手列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSingerList()
  }, [queryParams])

  // 搜索
  const handleSearch = () => {
    setQueryParams({ ...queryParams, pageNum: 1 })
  }

  // 重置
  const handleReset = () => {
    setQueryParams({
      pageNum: 1,
      pageSize: 10,
      name: '',
      region: '',
    })
  }

  // 打开新增弹窗
  const handleAdd = () => {
    setEditingSinger(null)
    form.resetFields()
    setModalVisible(true)
  }

  // 打开编辑弹窗
  const handleEdit = async (record) => {
    try {
      // 获取详情
      const res = await getSingerById(record.id)
      setEditingSinger(res.data)
      // BugC1修复：先 resetFields 清空上一次的表单值，再 setFieldsValue 填充新数据
      // Ant Design Form 的 setFieldsValue 对 null/undefined 字段不做清空，
      // 若先后编辑两个歌手且第二个某字段为 null，会残留第一个的值
      form.resetFields()
      form.setFieldsValue(res.data)
      setModalVisible(true)
    } catch (error) {
      console.error('获取歌手详情失败:', error)
    }
  }

  // 删除
  const handleDelete = async (id) => {
    try {
      await deleteSinger(id)
      message.success('删除成功')
      loadSingerList()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      if (editingSinger) {
        await updateSinger(editingSinger.id, values)
      } else {
        await addSinger(values)
      }
      
      message.success(editingSinger ? '修改成功' : '新增成功')
      setModalVisible(false)
      loadSingerList()
    } catch (error) {
      console.error('提交失败:', error)
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
      title: '歌手名',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '性别',
      dataIndex: 'gender',
      key: 'gender',
      width: 100,
      render: (gender) => {
        const genderMap = { 0: '未知', 1: '男', 2: '女', 3: '组合' }
        const colorMap = { 0: 'default', 1: 'blue', 2: 'pink', 3: 'purple' }
        return (
          <Tag color={colorMap[gender]}>
            {genderMap[gender]}
          </Tag>
        )
      },
    },
    {
      title: '地区',
      dataIndex: 'region',
      key: 'region',
      width: 100,
    },
    {
      title: '歌曲数量',
      dataIndex: 'songCount',
      key: 'songCount',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'error'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除该歌手吗?"
            description="删除后不可恢复"
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
          <Input
            placeholder="歌手名"
            value={queryParams.name}
            onChange={(e) =>
              setQueryParams({ ...queryParams, name: e.target.value })
            }
            style={{ width: 200 }}
          />
          <Select
            placeholder="地区"
            value={queryParams.region}
            onChange={(value) =>
              setQueryParams({ ...queryParams, region: value })
            }
            options={regionOptions}
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
          新增歌手
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

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingSinger ? '编辑歌手' : '新增歌手'}
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
            label="歌手名"
            rules={[{ required: true, message: '请输入歌手名' }]}
          >
            <Input placeholder="请输入歌手名" />
          </Form.Item>

          <Form.Item
            name="gender"
            label="性别"
            rules={[{ required: true, message: '请选择性别' }]}
          >
            <Select
              placeholder="请选择性别"
              options={genderOptions}
            />
          </Form.Item>

          <Form.Item
            name="region"
            label="地区"
            rules={[{ required: true, message: '请选择地区' }]}
          >
            <Select
              placeholder="请选择地区"
              options={regionOptions.filter((item) => item.value !== '')  }
            />
          </Form.Item>

          <Form.Item
            name="avatar"
            label="头像URL"
          >
            <Input placeholder="请输入头像URL" />
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
