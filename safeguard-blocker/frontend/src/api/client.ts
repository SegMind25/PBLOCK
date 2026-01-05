import axios from 'axios'

const client = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
})

export default {
  async getStatus() {
    const { data } = await client.get('/status')
    return data
  },

  async setup(payload: { master_password: string; auto_start: boolean }) {
    const { data } = await client.post('/setup', payload)
    return data
  },

  async verifyPassword(password: string) {
    const { data } = await client.post('/auth/verify', { password })
    return data
  },

  async getStats() {
    const { data } = await client.get('/stats')
    return data
  },

  async getBlocklist() {
    const { data } = await client.get('/blocklist')
    return data
  },

  async updateBlocklist(token: string, domains: string[]) {
    const { data } = await client.post('/blocklist', { token, domains })
    return data
  }
}
