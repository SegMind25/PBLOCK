// frontend/src/views/Setup.vue
<template>
  <div class="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-500 to-purple-600">
    <div class="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-800 mb-2">🛡️ Safeguard Setup</h1>
        <p class="text-gray-600">Protect yourself from harmful content</p>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Master Password
          </label>
          <input
            v-model="password"
            type="password"
            required
            minlength="8"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter a strong password"
          />
          <p class="text-xs text-gray-500 mt-1">
            This password will be required to modify settings
          </p>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Confirm Password
          </label>
          <input
            v-model="confirmPassword"
            type="password"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Confirm your password"
          />
        </div>

        <div class="flex items-center">
          <input
            v-model="autoStart"
            type="checkbox"
            id="autoStart"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
          <label for="autoStart" class="ml-2 block text-sm text-gray-700">
            Start automatically with system
          </label>
        </div>

        <div v-if="error" class="p-3 bg-red-100 text-red-700 rounded-lg text-sm">
          {{ error }}
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-50"
        >
          {{ loading ? 'Setting up...' : 'Complete Setup' }}
        </button>
      </form>

      <div class="mt-6 p-4 bg-yellow-50 rounded-lg">
        <p class="text-sm text-yellow-800">
          ⚠️ <strong>Important:</strong> Once activated, this protection cannot be disabled without the master password.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useBlockerStore } from '../stores/blocker'

const router = useRouter()
const blockerStore = useBlockerStore()

const password = ref('')
const confirmPassword = ref('')
const autoStart = ref(true)
const loading = ref(false)
const error = ref('')

async function handleSubmit() {
  error.value = ''

  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match'
    return
  }

  if (password.value.length < 8) {
    error.value = 'Password must be at least 8 characters'
    return
  }

  loading.value = true

  const success = await blockerStore.completeSetup(password.value, autoStart.value)

  loading.value = false

  if (success) {
    router.push('/dashboard')
  } else {
    error.value = 'Setup failed. Please try again.'
  }
}
</script>

// frontend/src/views/Dashboard.vue
<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div class="flex justify-between items-center">
          <h1 class="text-2xl font-bold text-gray-800">🛡️ Safeguard Dashboard</h1>
          <RouterLink
            to="/settings"
            class="text-blue-600 hover:text-blue-700 font-medium"
          >
            Settings
          </RouterLink>
        </div>
      </div>
    </nav>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="flex-1">
              <p class="text-sm font-medium text-gray-600">Protection Status</p>
              <p class="text-2xl font-bold" :class="isActive ? 'text-green-600' : 'text-red-600'">
                {{ isActive ? 'Active' : 'Inactive' }}
              </p>
            </div>
            <div :class="isActive ? 'text-green-500' : 'text-red-500'" class="text-4xl">
              {{ isActive ? '✓' : '✗' }}
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <p class="text-sm font-medium text-gray-600">Blocked Today</p>
          <p class="text-2xl font-bold text-gray-800">{{ stats.blocked_today }}</p>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <p class="text-sm font-medium text-gray-600">Total Blocked</p>
          <p class="text-2xl font-bold text-gray-800">{{ stats.total_blocked }}</p>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <h2 class="text-xl font-bold text-gray-800 mb-4">Blocked Domains</h2>
        <div class="max-h-96 overflow-y-auto">
          <div v-if="blocklist.length === 0" class="text-gray-500 text-center py-8">
            No domains in blocklist
          </div>
          <div v-else class="space-y-2">
            <div
              v-for="domain in blocklist"
              :key="domain"
              class="flex items-center justify-between p-3 bg-gray-50 rounded"
            >
              <span class="font-mono text-sm">{{ domain }}</span>
              <span class="text-red-500">🚫</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useBlockerStore } from '../stores/blocker'

const blockerStore = useBlockerStore()

const isActive = computed(() => blockerStore.isActive)
const stats = computed(() => blockerStore.stats)
const blocklist = computed(() => blockerStore.blocklist)

onMounted(async () => {
  await blockerStore.loadStats()
  await blockerStore.loadBlocklist()
})
</script>
