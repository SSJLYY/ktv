import { createBrowserRouter, Navigate } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout'

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
      },
      {
        path: 'queue',
        lazy: () => import('../pages/Queue/index.jsx'),
      },
    ],
  },
  {
    path: '/join',
    lazy: () => import('../pages/Join/index.jsx'),
  },
])

export default router
