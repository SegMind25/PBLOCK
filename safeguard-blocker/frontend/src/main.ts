import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './assets/styles/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

// frontend/src/App.vue
<template>
  <div id="app" class="min-h-screen bg-gray-50">
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useBlockerStore } from './stores/blocker'

const blockerStore = useBlockerStore()

onMounted(async () => {
  await blockerStore.checkStatus()
})
</script>
