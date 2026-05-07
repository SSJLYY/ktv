import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { Spin } from 'antd'
import AdminLayout from '../layouts/AdminLayout'
import { useUserStore } from '../store/userStore'

const Login = lazy(() => import('../pages/Login'))
const Singer = lazy(() => import('../pages/Singer'))
const Song = lazy(() => import('../pages/Song'))
const Category = lazy(() => import('../pages/Category'))
const Room = lazy(() => import('../pages/Room'))
const Order = lazy(() => import('../pages/Order'))

const LoadingScreen = () => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      height: '100vh',
    }}
  >
    <Spin size="large" tip="加载中..." />
  </div>
)

const PrivateRoute = ({ children }) => {
  const token = useUserStore((state) => state.token)
  const hasHydrated = useUserStore((state) => state.hasHydrated)

  if (!hasHydrated) {
    return <LoadingScreen />
  }

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return children
}

const PublicRoute = ({ children }) => {
  const token = useUserStore((state) => state.token)
  const hasHydrated = useUserStore((state) => state.hasHydrated)

  if (!hasHydrated) {
    return <LoadingScreen />
  }

  if (token) {
    return <Navigate to="/" replace />
  }

  return children
}

const Router = () => {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingScreen />}>
        <Routes>
          <Route
            path="/login"
            element={(
              <PublicRoute>
                <Login />
              </PublicRoute>
            )}
          />

          <Route
            path="/"
            element={(
              <PrivateRoute>
                <AdminLayout />
              </PrivateRoute>
            )}
          >
            <Route index element={<Navigate to="/singer" replace />} />
            <Route path="singer" element={<Singer />} />
            <Route path="song" element={<Song />} />
            <Route path="category" element={<Category />} />
            <Route path="room" element={<Room />} />
            <Route path="order" element={<Order />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default Router
