<template>
  <footer class="bg-white border-t border-gray-200 mt-auto">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
      <div class="flex flex-col sm:flex-row justify-between items-center gap-2">
        <p class="text-sm text-gray-600">
          © 2024 Web Guardian. All rights reserved.
        </p>
        <div class="flex items-center gap-4 text-sm text-gray-600">
          <span>Version {{ version }}</span>
          <span class="text-gray-400">|</span>
          <a href="#" class="hover:text-primary-600 transition-colors">Privacy Policy</a>
          <span class="text-gray-400">|</span>
          <a href="#" class="hover:text-primary-600 transition-colors">Support</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { APP_VERSION } from '@/utils/constants'

const version = APP_VERSION
</script>