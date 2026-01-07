import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authService from '@/services/auth.service'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || null)
  const setupRequired = ref(false)
  const setupChecked = ref(false)
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => !!token.value)
  const isParent = computed(() => user.value?.is_parent || false)

  async function checkSetup() {
    try {
      const response = await authService.checkSetup()
      setupRequired.value = response.setup_required
      setupChecked.value = true
      return response
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  async function register(userData) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.register(userData)
      setupRequired.value = false
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function login(credentials) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.login(credentials)
      token.value = response.token
      user.value = response.user
      localStorage.setItem('token', response.token)
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    if (!token.value) return

    try {
      const response = await authService.getCurrentUser()
      user.value = response
    } catch (err) {
      logout()
      throw err
    }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  return {
    user,
    token,
    setupRequired,
    setupChecked,
    loading,
    error,
    isAuthenticated,
    isParent,
    checkSetup,
    register,
    login,
    fetchCurrentUser,
    logout
  }
})