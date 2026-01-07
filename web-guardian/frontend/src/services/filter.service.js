import api from './api'

const filterService = {
  async getBlockedSites() {
    const response = await api.get('/api/filter/blocked-sites')
    return response.data
  },

  async addBlockedSite(siteData) {
    const response = await api.post('/api/filter/blocked-sites', siteData)
    return response.data
  },

  async deleteBlockedSite(siteId) {
    const response = await api.delete(`/api/filter/blocked-sites/${siteId}`)
    return response.data
  },

  async checkDomain(domain) {
    const response = await api.get(`/api/filter/check/${domain}`)
    return response.data
  },

  async getAccessLogs(limit = 100) {
    const response = await api.get(`/api/filter/logs?limit=${limit}`)
    return response.data
  },

  async getStats() {
    const response = await api.get('/api/filter/stats')
    return response.data
  },

  async enableFilter() {
    const response = await api.put('/api/filter/enable')
    return response.data
  },

  async disableFilter() {
    const response = await api.put('/api/filter/disable')
    return response.data
  }
}

export default filterService