import { createRouter, createWebHistory } from 'vue-router'
import Setup from '../views/Setup.vue'
import Blocked from '../views/Blocked.vue'
import axios from 'axios'

const routes = [
  {
    path: '/',
    name: 'Setup',
    component: Setup
  },
  {
    path: '/blocked',
    name: 'Blocked',
    component: Blocked
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to, from, next) => {
  try {
    const response = await axios.get('/api/status')
    const { activated } = response.data
    
    if (activated && to.name === 'Setup') {
      // If already activated, redirect to blocked page
      next({ name: 'Blocked' })
    } else if (!activated && to.name === 'Blocked') {
      // If not activated, stay on setup
      next({ name: 'Setup' })
    } else {
      next()
    }
  } catch (error) {
    console.error('Navigation guard error:', error)
    next()
  }
})

export default router
