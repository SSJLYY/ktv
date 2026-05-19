import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useRoomStore = create(
  persist(
    (set) => ({
      orderId: null,
      roomId: null,
      hasHydrated: false,
      queueVersion: 0,
      setSession: ({ orderId, roomId }) => set({ orderId, roomId }),
      setOrderId: (orderId) => set({ orderId }),
      setRoomId: (roomId) => set({ roomId }),
      clearOrderId: () => set({ orderId: null, queueVersion: 0 }),
      clearSession: () => set({ orderId: null, roomId: null, queueVersion: 0 }),
      setHasHydrated: (hasHydrated) => set({ hasHydrated }),
      bumpQueueVersion: () => set((state) => ({ queueVersion: state.queueVersion + 1 })),
    }),
    {
      name: 'ktv-room-store',
      partialize: (state) => ({ orderId: state.orderId, roomId: state.roomId }),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated?.(true)
      },
    }
  )
)

export default useRoomStore
