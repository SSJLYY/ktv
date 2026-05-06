import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useRoomStore = create(
  persist(
    (set) => ({
      orderId: null,
      hasHydrated: false,
      setOrderId: (orderId) => set({ orderId }),
      clearOrderId: () => set({ orderId: null, queueVersion: 0 }),
      setHasHydrated: (hasHydrated) => set({ hasHydrated }),
      queueVersion: 0,
      bumpQueueVersion: () => set((state) => ({ queueVersion: state.queueVersion + 1 })),
    }),
    {
      name: 'ktv-room-store',
      partialize: (state) => ({ orderId: state.orderId }),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated?.(true)
      },
    }
  )
)

export default useRoomStore
