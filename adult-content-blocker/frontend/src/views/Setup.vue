<template>
  <div class="container">
    <div class="setup-card">
      <div class="icon">🛡️</div>
      <h1>Adult Content Blocker</h1>
      <p class="warning">⚠️ WARNING: Once activated, this blocker CANNOT be disabled!</p>
      
      <div v-if="!loading && !activated" class="setup-form">
        <p class="description">
          This application will permanently block access to adult content websites.
          After activation, the blocker will run in the background and cannot be turned off.
        </p>
        
        <div class="form-group">
          <label for="password">Set Admin Password:</label>
          <input 
            type="password" 
            id="password" 
            v-model="password"
            placeholder="Enter password (min 6 characters)"
            @keyup.enter="activateBlocker"
          />
        </div>
        
        <div class="form-group">
          <label>
            <input type="checkbox" v-model="understood" />
            I understand this action is permanent and irreversible
          </label>
        </div>
        
        <button 
          @click="activateBlocker" 
          :disabled="!canActivate"
          class="btn-activate"
        >
          Activate Blocker Permanently
        </button>
        
        <p v-if="error" class="error">{{ error }}</p>
      </div>
      
      <div v-else-if="activated" class="activated-message">
        <div class="success-icon">✅</div>
        <h2>Blocker is Active</h2>
        <p>The adult content blocker is running and protecting your system.</p>
      </div>
      
      <div v-else class="loading">
        <div class="spinner"></div>
        <p>Loading...</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Setup',
  data() {
    return {
      password: '',
      understood: false,
      loading: true,
      activated: false,
      error: ''
    }
  },
  computed: {
    canActivate() {
      return this.password.length >= 6 && this.understood
    }
  },
  async mounted() {
    await this.checkStatus()
  },
  methods: {
    async checkStatus() {
      try {
        const response = await axios.get('/api/status')
        this.activated = response.data.activated
        this.loading = false
      } catch (error) {
        console.error('Error checking status:', error)
        this.loading = false
      }
    },
    async activateBlocker() {
      if (!this.canActivate) return
      
      this.error = ''
      this.loading = true
      
      try {
        const response = await axios.post('/api/activate', {
          password: this.password
        })
        
        if (response.data.success) {
          this.activated = true
          alert('✅ Blocker activated successfully! The application will now run permanently.')
          this.$router.push('/blocked')
        } else {
          this.error = response.data.message
        }
      } catch (error) {
        this.error = error.response?.data?.message || 'Failed to activate blocker. Make sure you run as administrator.'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
