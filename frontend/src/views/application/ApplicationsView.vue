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

const applications = ref([])
const showModal = ref(false)

const loadApps = async () => {
  const res = await api.get('/applications')
  applications.value = res.data
}

onMounted(loadApps)

const addApp = async (app) => {
  await api.post('/applications', app)
  await loadApps()
  showModal.value = false
}

const syncApp = (name) => {
  alert(`Синхронизация ${name} пока не реализована`)
}

const deleteApp = (name) => {
  applications.value = applications.value.filter(a => a.name !== name)
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
