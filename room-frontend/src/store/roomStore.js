import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useRoomStore = create(
  persist(
    (set) => ({
      // 当前订单ID（开台后由URL或扫码带入）
      orderId: null,
      setOrderId: (orderId) => set({ orderId }),

      // 队列数量变化标记，用于触发列表刷新
      queueVersion: 0,
      bumpQueueVersion: () => set((state) => ({ queueVersion: state.queueVersion + 1 })),
    }),
    {
      name: 'ktv-room-store', // localStorage key
      partialize: (state) => ({ orderId: state.orderId }), // 只持久化 orderId
    }
  )
)

export default useRoomStore
