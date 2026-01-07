import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { computed } from 'vue'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()

  const user = computed(() => authStore.user)
  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const isParent = computed(() => authStore.isParent)
  const loading = computed(() => authStore.loading)
  const error = computed(() => authStore.error)

  async function login(credentials) {
    try {
      await authStore.login(credentials)
      await router.push('/dashboard')
    } catch (err) {
      throw err
    }
  }

  async function register(userData) {
    try {
      await authStore.register(userData)
      // After registration, automatically log in
      await login({
        username: userData.username,
        password: userData.password
      })
    } catch (err) {
      throw err
    }
  }

  async function logout() {
    authStore.logout()
    await router.push('/login')
  }

  return {
    user,
    isAuthenticated,
    isParent,
    loading,
    error,
    login,
    register,
    logout
  }
}