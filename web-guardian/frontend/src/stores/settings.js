import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  const filterEnabled = ref(true)
  const tamperProtectionEnabled = ref(true)
  const notifications = ref(true)
  const theme = ref('light')

  function toggleFilter() {
    filterEnabled.value = !filterEnabled.value
    saveToLocalStorage()
  }

  function toggleTamperProtection() {
    tamperProtectionEnabled.value = !tamperProtectionEnabled.value
    saveToLocalStorage()
  }

  function toggleNotifications() {
    notifications.value = !notifications.value
    saveToLocalStorage()
  }

  function setTheme(newTheme) {
    theme.value = newTheme
    saveToLocalStorage()
  }

  function saveToLocalStorage() {
    const settings = {
      filterEnabled: filterEnabled.value,
      tamperProtectionEnabled: tamperProtectionEnabled.value,
      notifications: notifications.value,
      theme: theme.value
    }
    localStorage.setItem('guardian_settings', JSON.stringify(settings))
  }

  function loadFromLocalStorage() {
    const stored = localStorage.getItem('guardian_settings')
    if (stored) {
      const settings = JSON.parse(stored)
      filterEnabled.value = settings.filterEnabled ?? true
      tamperProtectionEnabled.value = settings.tamperProtectionEnabled ?? true
      notifications.value = settings.notifications ?? true
      theme.value = settings.theme ?? 'light'
    }
  }

  // Load settings on store creation
  loadFromLocalStorage()

  return {
    filterEnabled,
    tamperProtectionEnabled,
    notifications,
    theme,
    toggleFilter,
    toggleTamperProtection,
    toggleNotifications,
    setTheme
  }
})