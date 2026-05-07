import React from 'react'
import { Layout, Dropdown, Avatar, Space, message } from 'antd'
import { UserOutlined, LogoutOutlined } from '@ant-design/icons'
import { Outlet, useNavigate } from 'react-router-dom'
import SideMenu from '../components/SideMenu'
import { useUserStore } from '../store/userStore'
import { logout as logoutApi } from '../api/auth'

const { Content, Header, Sider } = Layout

const AdminLayout = () => {
  const navigate = useNavigate()
  const { userInfo, logout } = useUserStore()

  const handleLogout = async () => {
    try {
      await logoutApi()
    } catch {
      // Ignore logout API failure and continue local logout.
    } finally {
      logout()
      message.success('已退出登录')
      navigate('/login')
    }
  }

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={220}
        style={{
          background: '#001529',
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: 20,
            fontWeight: 'bold',
          }}
        >
          KTV 管理系统
        </div>
        <SideMenu />
      </Sider>

      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)',
          }}
        >
          <div style={{ fontSize: 18, fontWeight: 500 }}>
            后台管理
          </div>

          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar
                style={{ backgroundColor: '#1890ff' }}
                icon={<UserOutlined />}
              />
              <span style={{ fontSize: 14 }}>
                {userInfo?.realName || userInfo?.username || '管理员'}
              </span>
            </Space>
          </Dropdown>
        </Header>

        <Content
          style={{
            margin: '24px',
            padding: '24px',
            background: '#fff',
            borderRadius: '8px',
            minHeight: 280,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AdminLayout
