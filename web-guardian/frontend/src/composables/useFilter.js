import { useFilterStore } from '@/stores/filter'
import { computed } from 'vue'

export function useFilter() {
  const filterStore = useFilterStore()

  const blockedSites = computed(() => filterStore.blockedSites)
  const accessLogs = computed(() => filterStore.accessLogs)
  const stats = computed(() => filterStore.stats)
  const loading = computed(() => filterStore.loading)
  const error = computed(() => filterStore.error)

  async function loadBlockedSites() {
    return await filterStore.fetchBlockedSites()
  }

  async function addSite(siteData) {
    return await filterStore.addBlockedSite(siteData)
  }

  async function removeSite(siteId) {
    return await filterStore.deleteBlockedSite(siteId)
  }

  async function loadAccessLogs(limit = 100) {
    return await filterStore.fetchAccessLogs(limit)
  }

  async function loadStats() {
    return await filterStore.fetchStats()
  }

  async function enableFilter() {
    return await filterStore.enableFilter()
  }

  async function disableFilter() {
    return await filterStore.disableFilter()
  }

  return {
    blockedSites,
    accessLogs,
    stats,
    loading,
    error,
    loadBlockedSites,
    addSite,
    removeSite,
    loadAccessLogs,
    loadStats,
    enableFilter,
    disableFilter
  }
}