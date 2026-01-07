<template>
  <div class="card">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900">Blocked Sites</h3>
      <Button variant="primary" size="sm" @click="showAddModal = true">
        <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Add Site
      </Button>
    </div>

    <div v-if="loading" class="py-8">
      <Loader message="Loading blocked sites..." />
    </div>

    <div v-else-if="sites.length === 0" class="py-8 text-center">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
      </svg>
      <p class="text-gray-500">No blocked sites yet</p>
      <p class="text-sm text-gray-400 mt-1">Add your first blocked site to get started</p>
    </div>

    <div v-else class="space-y-2 max-h-96 overflow-y-auto">
      <div
        v-for="site in sites"
        :key="site.id"
        class="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
      >
        <div class="flex-1">
          <p class="font-medium text-gray-900">{{ site.domain }}</p>
          <div class="flex items-center gap-2 mt-1">
            <span :class="['badge', getCategoryColor(site.category)]">
              {{ site.category }}
            </span>
            <span class="text-xs text-gray-500">
              Added {{ formatDate(site.created_at) }}
            </span>
          </div>
        </div>
        <button
          @click="handleDelete(site.id)"
          class="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          title="Remove"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Add Site Modal -->
    <Modal :show="showAddModal" title="Add Blocked Site" @close="showAddModal = false">
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <Input
          v-model="formData.domain"
          label="Domain"
          placeholder="example.com"
          required
          :error="formErrors.domain"
        />
        
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
          <select
            v-model="formData.category"
            class="input w-full"
            required
          >
            <option value="">Select category</option>
            <option v-for="cat in categories" :key="cat.value" :value="cat.value">
              {{ cat.label }}
            </option>
          </select>
        </div>

        <Input
          v-model="formData.reason"
          label="Reason (optional)"
          placeholder="Why are you blocking this site?"
        />

        <template #footer>
          <Button variant="secondary" @click="showAddModal = false">Cancel</Button>
          <Button variant="primary" type="submit" :loading="submitting">Add Site</Button>
        </template>
      </form>
    </Modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useFilter } from '@/composables/useFilter'
import { CATEGORIES, CATEGORY_COLORS } from '@/utils/constants'
import { formatDate } from '@/utils/helpers'
import { sanitizeDomain, validateDomain } from '@/utils/validators'
import Button from '@/components/common/Button.vue'
import Input from '@/components/common/Input.vue'
import Modal from '@/components/common/Modal.vue'
import Loader from '@/components/common/Loader.vue'

const { blockedSites, loading, loadBlockedSites, addSite, removeSite } = useFilter()

const sites = computed(() => blockedSites.value)
const categories = CATEGORIES

const showAddModal = ref(false)
const submitting = ref(false)
const formData = ref({
  domain: '',
  category: '',
  reason: ''
})
const formErrors = ref({
  domain: ''
})

onMounted(() => {
  loadBlockedSites()
})

function getCategoryColor(category) {
  return CATEGORY_COLORS[category] || CATEGORY_COLORS.custom
}

async function handleSubmit() {
  formErrors.value = { domain: '' }
  
  const sanitized = sanitizeDomain(formData.value.domain)
  if (!validateDomain(sanitized)) {
    formErrors.value.domain = 'Please enter a valid domain'
    return
  }

  submitting.value = true
  try {
    await addSite({
      domain: sanitized,
      category: formData.value.category,
      reason: formData.value.reason || null
    })
    
    showAddModal.value = false
    formData.value = { domain: '', category: '', reason: '' }
  } catch (error) {
    formErrors.value.domain = error.message
  } finally {
    submitting.value = false
  }
}

async function handleDelete(siteId) {
  if (confirm('Are you sure you want to remove this blocked site?')) {
    try {
      await removeSite(siteId)
    } catch (error) {
      alert('Failed to remove site: ' + error.message)
    }
  }
}
</script>