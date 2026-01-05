<template>
  <div class="container">
    <div class="blocked-card">
      <div class="blocked-icon">🚫</div>
      <h1>Content Blocked</h1>
      <p class="message">
        This website has been blocked by the Adult Content Blocker.
      </p>
      <p class="sub-message">
        The blocker is active and protecting your system.
      </p>
      
      <div class="stats" v-if="blockedCount">
        <p>🛡️ Blocking {{ blockedCount }} domains</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Blocked',
  data() {
    return {
      blockedCount: 0
    }
  },
  async mounted() {
    try {
      const response = await axios.get('/api/status')
      this.blockedCount = response.data.blocked_count
    } catch (error) {
      console.error('Error fetching status:', error)
    }
  }
}
</script>
