import { createBrowserRouter, Navigate } from 'react-router-dom'
import { Spin } from 'antd-mobile'
import MainLayout from '../layouts/MainLayout'

// 页面加载中的 Loading 组件
const PageLoading = () => (
  <div style={{
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    background: '#f5f5f5',
  }}>
    <div style={{ textAlign: 'center' }}>
      <Spin color='primary' />
      <div style={{ marginTop: 12, color: '#999', fontSize: 14 }}>加载中...</div>
    </div>
  </div>
)

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <Navigate to="/search" replace />,
      },
      {
        path: 'search',
        lazy: () => import('../pages/Search/index.jsx'),
        hydrateFallbackElement: <PageLoading />,
      },
      {
        path: 'queue',
        lazy: () => import('../pages/Queue/index.jsx'),
        hydrateFallbackElement: <PageLoading />,
      },
    ],
  },
  {
    path: '/join',
    lazy: () => import('../pages/Join/index.jsx'),
    hydrateFallbackElement: <PageLoading />,
  },
])

export default router
