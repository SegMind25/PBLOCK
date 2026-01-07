<template>
  <button
    :type="type"
    :class="buttonClasses"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="mr-2">
      <svg class="animate-spin h-4 w-4 inline" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </span>
    <slot></slot>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'danger', 'success', 'outline'].includes(value)
  },
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value)
  },
  type: {
    type: String,
    default: 'button'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  fullWidth: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const buttonClasses = computed(() => {
  const classes = ['btn', 'transition-all', 'duration-200', 'font-medium', 'rounded-lg', 'focus:outline-none', 'focus:ring-2', 'focus:ring-offset-2']
  
  // Variant styles
  if (props.variant === 'primary') {
    classes.push('bg-primary-600', 'text-white', 'hover:bg-primary-700', 'focus:ring-primary-500', 'disabled:bg-primary-300')
  } else if (props.variant === 'secondary') {
    classes.push('bg-gray-200', 'text-gray-800', 'hover:bg-gray-300', 'focus:ring-gray-400', 'disabled:bg-gray-100')
  } else if (props.variant === 'danger') {
    classes.push('bg-red-600', 'text-white', 'hover:bg-red-700', 'focus:ring-red-500', 'disabled:bg-red-300')
  } else if (props.variant === 'success') {
    classes.push('bg-green-600', 'text-white', 'hover:bg-green-700', 'focus:ring-green-500', 'disabled:bg-green-300')
  } else if (props.variant === 'outline') {
    classes.push('border-2', 'border-primary-600', 'text-primary-600', 'hover:bg-primary-50', 'focus:ring-primary-500')
  }
  
  // Size styles
  if (props.size === 'sm') {
    classes.push('px-3', 'py-1.5', 'text-sm')
  } else if (props.size === 'md') {
    classes.push('px-4', 'py-2', 'text-base')
  } else if (props.size === 'lg') {
    classes.push('px-6', 'py-3', 'text-lg')
  }
  
  // Full width
  if (props.fullWidth) {
    classes.push('w-full')
  }
  
  // Disabled state
  if (props.disabled || props.loading) {
    classes.push('cursor-not-allowed', 'opacity-60')
  }
  
  return classes.join(' ')
})

function handleClick(event) {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>