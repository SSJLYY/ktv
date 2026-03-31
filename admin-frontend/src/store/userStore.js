import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// 用户状态管理
// Bug修复：
// 1. setToken/setUserInfo 去掉手动 localStorage 操作，Zustand persist 会自动处理，双写会导致混乱
// 2. logout 去掉手动 localStorage 操作，同上
// 3. isLoggedIn 改为普通 getter selector，不应在 action 内部递归调用 useUserStore.getState()
export const useUserStore = create(
  persist(
    (set) => ({
      token: null,
      userInfo: null,
      
      // 设置Token（persist 会自动同步到 localStorage，无需手动操作）
      setToken: (token) => {
        set({ token })
      },
      
      // 设置用户信息（persist 会自动同步到 localStorage，无需手动操作）
      setUserInfo: (userInfo) => {
        set({ userInfo })
      },
      
      // 登出 - 清除所有用户信息（persist 会自动移除对应 key，无需手动 removeItem）
      logout: () => {
        set({ token: null, userInfo: null })
      },
    }),
    {
      name: 'user-storage', // localStorage key
      partialize: (state) => ({ token: state.token, userInfo: state.userInfo }),
    }
  )
)
