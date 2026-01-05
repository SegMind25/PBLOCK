import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/client'

export const useBlockerStore = defineStore('blocker', () => {
  const isActive = ref(false)
  const isFirstRun = ref(true)
  const isAuthenticated = ref(false)
  const authToken = ref<string | null>(null)
  const stats = ref({
    total_blocked: 0,
    blocked_today: 0,
    last_block_time: null
  })
  const blocklist = ref<string[]>([])

  const isSetupComplete = computed(() => !isFirstRun.value)

  async function checkStatus() {
    try {
      const response = await api.getStatus()
      isActive.value = response.is_active
      isFirstRun.value = response.is_first_run
    } catch (error) {
      console.error('Failed to check status:', error)
    }
  }

  async function completeSetup(password: string, autoStart: boolean) {
    try {
      await api.setup({ master_password: password, auto_start: autoStart })
      isFirstRun.value = false
      isActive.value = true
      return true
    } catch (error) {
      console.error('Setup failed:', error)
      return false
    }
  }

  async function authenticate(password: string) {
    try {
      const response = await api.verifyPassword(password)
      authToken.value = response.token
      isAuthenticated.value = true
      return true
    } catch (error) {
      console.error('Authentication failed:', error)
      return false
    }
  }

  async function loadStats() {
    try {
      stats.value = await api.getStats()
    } catch (error) {
      console.error('Failed to load stats:', error)
    }
  }

  async function loadBlocklist() {
    try {
      blocklist.value = await api.getBlocklist()
    } catch (error) {
      console.error('Failed to load blocklist:', error)
    }
  }

  async function updateBlocklist(domains: string[]) {
    if (!authToken.value) return false
    
    try {
      await api.updateBlocklist(authToken.value, domains)
      blocklist.value = domains
      return true
    } catch (error) {
      console.error('Failed to update blocklist:', error)
      return false
    }
  }

  return {
    isActive,
    isFirstRun,
    isAuthenticated,
    isSetupComplete,
    stats,
    blocklist,
    checkStatus,
    completeSetup,
    authenticate,
    loadStats,
    loadBlocklist,
    updateBlocklist
  }
})

