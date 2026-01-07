<template>
  <div class="card">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900">Recent Activity</h3>
      <select
        v-model="selectedLimit"
        class="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        @change="handleLimitChange"
      >
        <option value="50">Last 50</option>
        <option value="100">Last 100</option>
        <option value="200">Last 200</option>
      </select>
    </div>

    <div v-if="loading" class="py-8">
      <Loader message="Loading activity logs..." />
    </div>

    <div v-else-if="logs.length === 0" class="py-8 text-center">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <p class="text-gray-500">No activity yet</p>
      <p class="text-sm text-gray-400 mt-1">Activity will appear here when websites are accessed</p>
    </div>

    <div v-else class="space-y-2 max-h-96 overflow-y-auto">
      <div
        v-for="log in logs"
        :key="log.id"
        class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
      >
        <div class="flex items-center gap-3 flex-1">
          <div :class="['w-2 h-2 rounded-full', log.blocked ? 'bg-red-500' : 'bg-green-500']"></div>
          <div class="flex-1">
            <p class="font-medium text-gray-900">{{ log.domain }}</p>
            <p class="text-xs text-gray-500">{{ formatRelativeTime(log.timestamp) }}</p>
          </div>
        </div>
        <span :class="['badge', log.blocked ? 'badge-danger' : 'badge-success']">
          {{ log.blocked ? 'Blocked' : 'Allowed' }}
        </span>
      </div>
    </div>

    <div v-if="logs.length > 0" class="mt-4 pt-4 border-t border-gray-200">
      <button
        @click="handleRefresh"
        class="w-full px-4 py-2 text-sm text-primary-600 hover:bg-primary-50 rounded-lg transition-colors font-medium"
      >
        Refresh Activity
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useFilter } from '@/composables/useFilter'
import { formatRelativeTime } from '@/utils/helpers'
import Loader from '@/components/common/Loader.vue'

const { accessLogs, loading, loadAccessLogs } = useFilter()

const logs = computed(() => accessLogs.value)
const selectedLimit = ref(100)

onMounted(() => {
  loadAccessLogs(selectedLimit.value)
})

function handleLimitChange() {
  loadAccessLogs(selectedLimit.value)
}

function handleRefresh() {
  loadAccessLogs(selectedLimit.value)
}
</script>