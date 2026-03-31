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

const SideMenu = () => {
  const navigate = useNavigate()
  const location = useLocation()

  // 菜单项配置
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

  // 点击菜单项
  const handleMenuClick = ({ key }) => {
    navigate(key)
  }

  // 获取当前选中的菜单项
  const getSelectedKeys = () => {
    const { pathname } = location
    // 如果是根路径,默认选中歌手管理
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
      onClick={handleMenuClick}
    />
  )
}

export default SideMenu
