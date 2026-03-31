import React, { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { useUserStore } from '../../store/userStore'
import './index.css'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setToken, setUserInfo } = useUserStore()

  const onFinish = async (values) => {
    try {
      setLoading(true)
      
      // 调用登录接口
      // Bug修复：request 拦截器已处理非 200 情况（会 reject），此处无需再判断 res.code
      const res = await login({
        username: values.username,
        password: values.password,
      })
      
      // 保存Token和用户信息
      setToken(res.data.token)
      setUserInfo(res.data)
      
      message.success('登录成功')
      
      // 跳转到首页
      navigate('/')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <Card className="login-card" title="KTV管理系统">
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
            >
              登录
            </Button>
          </Form.Item>
        </Form>

        <div className="login-tips">
          <p>默认账号: admin</p>
          <p>默认密码: admin123</p>
        </div>
      </Card>
    </div>
  )
}

export default Login
