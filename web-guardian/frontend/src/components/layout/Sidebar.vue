<template>
  <aside class="w-64 bg-white border-r border-gray-200 min-h-screen p-6">
    <nav class="space-y-2">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-700 hover:bg-primary-50 hover:text-primary-600 transition-colors"
        active-class="bg-primary-100 text-primary-700 font-medium"
      >
        <component :is="item.icon" class="w-5 h-5" />
        <span>{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="mt-8 pt-6 border-t border-gray-200">
      <div class="px-4 py-3 bg-green-50 rounded-lg">
        <div class="flex items-center gap-2 mb-1">
          <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
          <span class="text-sm font-medium text-green-900">Protection Active</span>
        </div>
        <p class="text-xs text-green-700">{{ blockedCount }} sites blocked</p>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useFilterStore } from '@/stores/filter'

const filterStore = useFilterStore()

const blockedCount = computed(() => filterStore.blockedSites.length)

const menuItems = [
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: 'IconDashboard'
  },
  {
    path: '/settings',
    label: 'Settings',
    icon: 'IconSettings'
  }
]
</script>