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
  getAllCategories,
  getCategoryById,
  addCategory,
  updateCategory,
  deleteCategory,
} from '../../api/category'

const Category = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [form] = Form.useForm()

  // 加载分类列表
  const loadCategoryList = async () => {
    try {
      setLoading(true)
      const res = await getAllCategories()
      setDataSource(res.data || [])
    } catch (error) {
      console.error('加载分类列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategoryList()
  }, [])

  // 打开新增弹窗
  const handleAdd = () => {
    setEditingCategory(null)
    form.resetFields()
    setModalVisible(true)
  }

  // 打开编辑弹窗
  const handleEdit = async (record) => {
    try {
      const res = await getCategoryById(record.id)
      setEditingCategory(res.data)
      // BugC1修复：先 resetFields 清空上一次的表单值，再 setFieldsValue 填充
      form.resetFields()
      form.setFieldsValue(res.data)
      setModalVisible(true)
    } catch (error) {
      console.error('获取分类详情失败:', error)
    }
  }

  // 删除
  const handleDelete = async (id) => {
    try {
      await deleteCategory(id)
      message.success('删除成功')
      loadCategoryList()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      if (editingCategory) {
        await updateCategory(editingCategory.id, values)
      } else {
        await addCategory(values)
      }

      message.success(editingCategory ? '修改成功' : '新增成功')
      setModalVisible(false)
      loadCategoryList()
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
      title: '分类名',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '排序',
      // BugA2修复：CategoryVO 字段名是 sortOrder，不是 sort
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80,
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
            title="确定删除该分类吗?"
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
      {/* 操作按钮 */}
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增分类
        </Button>
      </div>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingCategory ? '编辑分类' : '新增分类'}
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
            label="分类名"
            rules={[{ required: true, message: '请输入分类名' }]}
          >
            <Input placeholder="请输入分类名" />
          </Form.Item>

          {/* BugA2修复：CategoryDTO 字段名是 sortOrder，不是 sort */}
          <Form.Item name="sortOrder" label="排序" initialValue={0}>
            <InputNumber
              placeholder="数字越小越靠前"
              min={0}
              style={{ width: '100%' }}
            />
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

export default Category
