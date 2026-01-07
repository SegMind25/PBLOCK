<template>
  <div :class="containerClasses">
    <div :class="spinnerClasses">
      <svg class="animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>
    <p v-if="message" :class="messageClasses">{{ message }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value)
  },
  message: {
    type: String,
    default: ''
  },
  fullScreen: {
    type: Boolean,
    default: false
  }
})

const containerClasses = computed(() => {
  const classes = ['flex', 'flex-col', 'items-center', 'justify-center', 'gap-3']
  
  if (props.fullScreen) {
    classes.push('fixed', 'inset-0', 'bg-white', 'z-50')
  } else {
    classes.push('py-8')
  }
  
  return classes.join(' ')
})

const spinnerClasses = computed(() => {
  const classes = ['text-primary-600']
  
  if (props.size === 'sm') {
    classes.push('w-8', 'h-8')
  } else if (props.size === 'md') {
    classes.push('w-12', 'h-12')
  } else if (props.size === 'lg') {
    classes.push('w-16', 'h-16')
  }
  
  return classes.join(' ')
})

const messageClasses = computed(() => {
  return 'text-gray-600 text-center'
})
</script>