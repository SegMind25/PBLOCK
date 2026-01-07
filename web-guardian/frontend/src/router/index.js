import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/dashboard'
    },
    {
      path: '/setup',
      name: 'setup',
      component: () => import('@/views/InitialSetup.vue'),
      meta: { requiresNoAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/ParentLogin.vue'),
      meta: { requiresNoAuth: true }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { requiresAuth: true, requiresParent: true }
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/Settings.vue'),
      meta: { requiresAuth: true, requiresParent: true }
    },
    {
      path: '/locked',
      name: 'locked',
      component: () => import('@/views/Locked.vue'),
      meta: { public: true }
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Check if setup is required
  if (to.name !== 'setup' && !authStore.setupChecked) {
    try {
      await authStore.checkSetup()
      if (authStore.setupRequired && to.name !== 'setup') {
        return next('/setup')
      }
    } catch (error) {
      console.error('Setup check failed:', error)
    }
  }

  // Handle authentication
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      return next('/login')
    }
    
    if (to.meta.requiresParent && !authStore.user?.is_parent) {
      return next('/locked')
    }
  }

  // Redirect authenticated users away from login/setup
  if (to.meta.requiresNoAuth && authStore.isAuthenticated) {
    return next('/dashboard')
  }

  next()
})

export default router