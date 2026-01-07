<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center p-4">
    <div class="max-w-md w-full">
      <div class="bg-white rounded-2xl shadow-2xl p-8">
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-primary-100 rounded-full mb-4">
            <svg class="w-8 h-8 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-gray-900 mb-2">Parent Login</h1>
          <p class="text-gray-600">Enter your credentials to access the dashboard</p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-4">
          <Input
            v-model="formData.username"
            label="Username"
            type="text"
            placeholder="Enter your username"
            required
            :error="formErrors.username"
          />

          <Input
            v-model="formData.password"
            label="Password"
            type="password"
            placeholder="Enter your password"
            required
            :error="formErrors.password"
          />

          <Button
            type="submit"
            variant="primary"
            :loading="submitting"
            full-width
          >
            Login
          </Button>
        </form>

        <div v-if="error" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-800">{{ error }}</p>
        </div>

        <div class="mt-6 text-center">
          <p class="text-sm text-gray-600">
            Forgot your password? 
            <a href="#" class="text-primary-600 hover:text-primary-700 font-medium">
              Contact support
            </a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'
import Input from '@/components/common/Input.vue'
import Button from '@/components/common/Button.vue'

const { login } = useAuth()

const formData = reactive({
  username: '',
  password: ''
})

const formErrors = reactive({
  username: '',
  password: ''
})

const submitting = ref(false)
const error = ref('')

async function handleSubmit() {
  // Reset errors
  Object.keys(formErrors).forEach(key => formErrors[key] = '')
  error.value = ''

  // Basic validation
  if (!formData.username) {
    formErrors.username = 'Username is required'
    return
  }

  if (!formData.password) {
    formErrors.password = 'Password is required'
    return
  }

  submitting.value = true

  try {
    await login(formData)
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>