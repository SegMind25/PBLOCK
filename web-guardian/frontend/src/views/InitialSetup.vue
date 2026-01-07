<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center p-4">
    <div class="max-w-md w-full">
      <div class="bg-white rounded-2xl shadow-2xl p-8">
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-primary-100 rounded-full mb-4">
            <svg class="w-8 h-8 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-gray-900 mb-2">Welcome to Web Guardian</h1>
          <p class="text-gray-600">Set up your parent account to get started</p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-4">
          <Input
            v-model="formData.username"
            label="Username"
            type="text"
            placeholder="Choose a username"
            required
            :error="formErrors.username"
          />

          <Input
            v-model="formData.email"
            label="Email (optional)"
            type="email"
            placeholder="your@email.com"
            :error="formErrors.email"
          />

          <Input
            v-model="formData.password"
            label="Password"
            type="password"
            placeholder="Create a strong password"
            required
            :error="formErrors.password"
            hint="At least 8 characters with uppercase, lowercase, number, and special character"
          />

          <Input
            v-model="formData.confirmPassword"
            label="Confirm Password"
            type="password"
            placeholder="Confirm your password"
            required
            :error="formErrors.confirmPassword"
          />

          <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div class="flex gap-3">
              <svg class="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
              <div>
                <p class="text-sm font-medium text-yellow-800 mb-1">Important Notice</p>
                <p class="text-xs text-yellow-700">
                  Once setup is complete, the protection will be activated and cannot be disabled without parent authentication. 
                  Make sure to remember your credentials!
                </p>
              </div>
            </div>
          </div>

          <div class="flex items-start">
            <input
              id="agree"
              v-model="agreedToTerms"
              type="checkbox"
              class="mt-1 w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
              required
            />
            <label for="agree" class="ml-2 text-sm text-gray-700">
              I understand that this will activate permanent protection on this device
            </label>
          </div>

          <Button
            type="submit"
            variant="primary"
            :loading="submitting"
            :disabled="!agreedToTerms"
            full-width
          >
            Complete Setup & Activate Protection
          </Button>
        </form>

        <div v-if="error" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-800">{{ error }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { validatePassword, validateEmail } from '@/utils/validators'
import Input from '@/components/common/Input.vue'
import Button from '@/components/common/Button.vue'

const { register } = useAuth()

const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const formErrors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const agreedToTerms = ref(false)
const submitting = ref(false)
const error = ref('')

async function handleSubmit() {
  // Reset errors
  Object.keys(formErrors).forEach(key => formErrors[key] = '')
  error.value = ''

  // Validate username
  if (formData.username.length < 4) {
    formErrors.username = 'Username must be at least 4 characters'
    return
  }

  // Validate email if provided
  if (formData.email && !validateEmail(formData.email)) {
    formErrors.email = 'Please enter a valid email address'
    return
  }

  // Validate password
  const passwordValidation = validatePassword(formData.password)
  if (!passwordValidation.valid) {
    formErrors.password = passwordValidation.errors[0]
    return
  }

  // Confirm password
  if (formData.password !== formData.confirmPassword) {
    formErrors.confirmPassword = 'Passwords do not match'
    return
  }

  submitting.value = true

  try {
    await register({
      username: formData.username,
      email: formData.email || null,
      password: formData.password,
      is_parent: true
    })
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>