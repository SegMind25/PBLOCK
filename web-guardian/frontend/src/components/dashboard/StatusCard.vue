<template>
  <div class="card hover:shadow-lg transition-shadow">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-sm font-medium text-gray-600 mb-1">{{ title }}</p>
        <p class="text-3xl font-bold text-gray-900">{{ value }}</p>
        <p v-if="subtitle" class="text-sm text-gray-500 mt-1">{{ subtitle }}</p>
      </div>
      <div :class="['p-3 rounded-lg', iconBgColor]">
        <component :is="icon" :class="['w-6 h-6', iconColor]" />
      </div>
    </div>
    
    <div v-if="change" class="mt-4 flex items-center gap-1">
      <svg v-if="changeType === 'increase'" class="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 7.414V15a1 1 0 11-2 0V7.414L6.707 9.707a1 1 0 01-1.414 0z" clip-rule="evenodd" />
      </svg>
      <svg v-else class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z" clip-rule="evenodd" />
      </svg>
      <span :class="['text-sm font-medium', changeType === 'increase' ? 'text-green-600' : 'text-red-600']">
        {{ change }}
      </span>
      <span class="text-sm text-gray-500">{{ changeLabel }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    required: true
  },
  iconColor: {
    type: String,
    default: 'text-primary-600'
  },
  iconBgColor: {
    type: String,
    default: 'bg-primary-100'
  },
  change: {
    type: String,
    default: ''
  },
  changeType: {
    type: String,
    default: 'increase',
    validator: (value) => ['increase', 'decrease'].includes(value)
  },
  changeLabel: {
    type: String,
    default: 'from last week'
  }
})
</script>