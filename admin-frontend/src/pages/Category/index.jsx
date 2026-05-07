import React, { useEffect, useState } from 'react'
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
  addCategory,
  deleteCategory,
  getAllCategories,
  getCategoryById,
  updateCategory,
} from '../../api/category'
import { useUserStore } from '../../store/userStore'

const Category = () => {
  const userInfo = useUserStore((state) => state.userInfo)
  const isSuperAdmin = userInfo?.role === 'super_admin'

  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [form] = Form.useForm()

  const loadCategoryList = async () => {
    try {
      setLoading(true)
      const res = await getAllCategories()
      setDataSource(res.data || [])
    } catch (error) {
      console.error('Load category list failed:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategoryList()
  }, [])

  const closeModal = () => {
    setModalVisible(false)
    setEditingCategory(null)
    form.resetFields()
  }

  const handleAdd = () => {
    setEditingCategory(null)
    form.resetFields()
    form.setFieldsValue({
      sortOrder: 0,
      status: 1,
    })
    setModalVisible(true)
  }

  const handleEdit = async (record) => {
    try {
      const res = await getCategoryById(record.id)
      setEditingCategory(res.data)
      form.resetFields()
      form.setFieldsValue({
        ...res.data,
        sortOrder: res.data?.sortOrder ?? 0,
        status: res.data?.status ?? 1,
      })
      setModalVisible(true)
    } catch (error) {
      console.error('Load category detail failed:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteCategory(id)
      message.success('分类删除成功')
      await loadCategoryList()
    } catch (error) {
      console.error('Delete category failed:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingCategory) {
        await updateCategory(editingCategory.id, values)
        message.success('分类更新成功')
      } else {
        await addCategory(values)
        message.success('分类创建成功')
      }
      closeModal()
      await loadCategoryList()
    } catch (error) {
      console.error('Submit category failed:', error)
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
      title: '分类名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 100,
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
              title="确定删除这个分类吗？"
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
      {isSuperAdmin && (
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增分类
          </Button>
        </div>
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingCategory ? '编辑分类' : '新增分类'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={closeModal}
        okText="提交"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <Form.Item
            name="name"
            label="分类名称"
            rules={[{ required: true, message: '请输入分类名称' }]}
          >
            <Input placeholder="请输入分类名称" />
          </Form.Item>

          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
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

export default Category
