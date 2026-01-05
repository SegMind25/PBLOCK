import { createRouter, createWebHistory } from 'vue-router'
import { useBlockerStore } from '../stores/blocker'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/setup',
      name: 'Setup',
      component: () => import('../views/Setup.vue')
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('../views/Dashboard.vue')
    },
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('../views/Settings.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/blocked',
      name: 'Blocked',
      component: () => import('../views/Blocked.vue')
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const blockerStore = useBlockerStore()
  
  // Check if first run
  if (!blockerStore.isSetupComplete && to.name !== 'Setup') {
    next('/setup')
    return
  }
  
  // Check auth for protected routes
  if (to.meta.requiresAuth && !blockerStore.isAuthenticated) {
    next('/dashboard')
    return
  }
  
  next()
})

export default router

