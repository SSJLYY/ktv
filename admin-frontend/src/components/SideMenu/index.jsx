import React from 'react'
import { Menu } from 'antd'
import {
  UserOutlined,
  CustomerServiceOutlined,
  AppstoreOutlined,
  HomeOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'

const menuItems = [
  {
    key: '/singer',
    icon: <UserOutlined />,
    label: '歌手管理',
  },
  {
    key: '/song',
    icon: <CustomerServiceOutlined />,
    label: '歌曲管理',
  },
  {
    key: '/category',
    icon: <AppstoreOutlined />,
    label: '分类管理',
  },
  {
    key: '/room',
    icon: <HomeOutlined />,
    label: '包厢管理',
  },
  {
    key: '/order',
    icon: <ShoppingCartOutlined />,
    label: '订单管理',
  },
]

const SideMenu = () => {
  const navigate = useNavigate()
  const location = useLocation()

  const getSelectedKeys = () => {
    const { pathname } = location
    if (pathname === '/') {
      return ['/singer']
    }
    return [pathname]
  }

  return (
    <Menu
      theme="dark"
      mode="inline"
      selectedKeys={getSelectedKeys()}
      items={menuItems}
      onClick={({ key }) => navigate(key)}
    />
  )
}

export default SideMenu
