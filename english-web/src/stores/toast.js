import { defineStore } from 'pinia'

export const useToastStore = defineStore('toast', {
  state: () => ({ visible: false, timer: null }),
  actions: {
    show() {
      if (this.timer) clearTimeout(this.timer)
      this.visible = true
      this.timer = setTimeout(() => {
        this.visible = false
        this.timer = null
      }, 2500)
    },
  },
})
