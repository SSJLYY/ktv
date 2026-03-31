import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { Spin } from 'antd'
import AdminLayout from '../layouts/AdminLayout'
import { useUserStore } from '../store/userStore'

// 懒加载页面组件
const Login = lazy(() => import('../pages/Login'))
const Singer = lazy(() => import('../pages/Singer'))
const Song = lazy(() => import('../pages/Song'))
const Category = lazy(() => import('../pages/Category'))
const Room = lazy(() => import('../pages/Room'))
const Order = lazy(() => import('../pages/Order'))

// 路由守卫 - 检查登录状态
// Bug修复：从 Zustand store 读取 token，而不是直接读 localStorage
// 原因：store 在 persist 恢复前 token 为 null，直接读 localStorage 绕过了 Zustand 的状态管理
const PrivateRoute = ({ children }) => {
  const token = useUserStore((state) => state.token)
  
  if (!token) {
    // 未登录,重定向到登录页
    return <Navigate to="/login" replace />
  }
  
  return children
}

// 公共路由 - 已登录用户访问登录页时重定向到首页
const PublicRoute = ({ children }) => {
  const token = useUserStore((state) => state.token)
  
  if (token) {
    // 已登录,重定向到首页
    return <Navigate to="/" replace />
  }
  
  return children
}

// 路由配置
const Router = () => {
  return (
    <BrowserRouter>
      <Suspense
        fallback={
          <div style={{ 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center', 
            height: '100vh' 
          }}>
            <Spin size="large" tip="加载中..." />
          </div>
        }
      >
        <Routes>
          {/* 登录页 */}
          <Route
            path="/login"
            element={
              <PublicRoute>
                <Login />
              </PublicRoute>
            }
          />
          
          {/* 后台管理布局 */}
          <Route
            path="/"
            element={
              <PrivateRoute>
                <AdminLayout />
              </PrivateRoute>
            }
          >
            {/* 默认重定向到歌手管理 */}
            <Route index element={<Navigate to="/singer" replace />} />
            
            {/* 歌手管理 */}
            <Route path="singer" element={<Singer />} />
            
            {/* 歌曲管理 */}
            <Route path="song" element={<Song />} />
            
            {/* 分类管理 */}
            <Route path="category" element={<Category />} />
            
            {/* 包厢管理 */}
            <Route path="room" element={<Room />} />
            
            {/* 订单管理 */}
            <Route path="order" element={<Order />} />
          </Route>
          
          {/* 404 - 重定向到首页 */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default Router
