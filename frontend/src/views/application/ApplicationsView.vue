<template>
  <div>
    <div class="top-bar">
      <button @click="showModal = true">+ Новое приложение</button>
    </div>

    <div v-if="applications.length === 0" class="empty">Нет приложений</div>

    <div class="app-list">
      <AppCard
          v-for="app in applications"
          :key="app.name"
          :app="app"
          @sync="syncApp"
          @delete="deleteApp"
      />
    </div>

    <ApplicationModal
        v-if="showModal"
        @submit="addApp"
        @close="showModal = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import AppCard from './AppCard.vue'
import ApplicationModal from './ApplicationModal.vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const applications = ref([])
const showModal = ref(false)
let stompClient = null

const loadApps = async () => {
  const res = await api.get('/applications')
  applications.value = res.data
}

const connectWebSocket = () => {
  const wsUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/ws-status'

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 5000,
    onConnect: () => {
      stompClient.subscribe('/topic/status', (msg) => {
        const updated = JSON.parse(msg.body)
        const index = applications.value.findIndex(a => a.name === updated.name)
        if (index !== -1) {
          applications.value[index].status = updated.status
        }
      })
    }
  })

  stompClient.activate()
}

onMounted(async () => {
  await loadApps()
  connectWebSocket()
})

const addApp = async (app) => {
  await api.post('/applications', app)
  await loadApps()
  showModal.value = false
}

const syncApp = async (name) => {
  await api.post(`/applications/${name}/status`)
}

const deleteApp = async (name) => {
  await api.delete(`/applications/${name}`)
  await loadApps()
}
</script>

<style scoped>
.top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}
.app-list {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}
.empty {
  text-align: center;
  color: #888;
  margin-top: 2rem;
}
</style>
