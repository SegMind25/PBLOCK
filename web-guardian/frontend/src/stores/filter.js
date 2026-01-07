import { defineStore } from 'pinia'
import { ref } from 'vue'
import filterService from '@/services/filter.service'

export const useFilterStore = defineStore('filter', () => {
  const blockedSites = ref([])
  const accessLogs = ref([])
  const stats = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchBlockedSites() {
    loading.value = true
    error.value = null
    try {
      const response = await filterService.getBlockedSites()
      blockedSites.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function addBlockedSite(siteData) {
    loading.value = true
    error.value = null
    try {
      const response = await filterService.addBlockedSite(siteData)
      blockedSites.value.push(response)
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteBlockedSite(siteId) {
    loading.value = true
    error.value = null
    try {
      await filterService.deleteBlockedSite(siteId)
      blockedSites.value = blockedSites.value.filter(site => site.id !== siteId)
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAccessLogs(limit = 100) {
    loading.value = true
    error.value = null
    try {
      const response = await filterService.getAccessLogs(limit)
      accessLogs.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchStats() {
    loading.value = true
    error.value = null
    try {
      const response = await filterService.getStats()
      stats.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function enableFilter() {
    loading.value = true
    error.value = null
    try {
      const response = await filterService.enableFilter()
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function disableFilter() {
    loading.value = true
    error.value = null
    try {
      const response = await filterService.disableFilter()
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    blockedSites,
    accessLogs,
    stats,
    loading,
    error,
    fetchBlockedSites,
    addBlockedSite,
    deleteBlockedSite,
    fetchAccessLogs,
    fetchStats,
    enableFilter,
    disableFilter
  }
})