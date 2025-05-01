<template>
  <div class="server-settings">
    <div class="breadcrumbs">Настройки / Серверы</div>

    <div class="top-bar">
      <button @click="showForm = true">Добавить сервер</button>
      <button @click="loadServers">Обновить список</button>
    </div>

    <div class="table-header">
      <div>ИМЯ</div>
      <div>ХОСТ</div>
      <div>RAM</div>
      <div>CPU</div>
      <div>СТАТУС ПОДКЛЮЧЕНИЯ</div>
      <div></div>
    </div>

    <div v-if="servers.length">
      <div v-for="server in servers" :key="server.name" class="repo-row">
        <div class="cell">{{ server.name }}</div>
        <div class="cell">{{ server.host }}</div>
        <div class="cell">{{ server.ram || '-' }}</div>
        <div class="cell">{{ server.cpu || '-' }}</div>
        <div class="cell">
          <span :class="statusClass(server.status)">{{ server.status || 'Unknown' }}</span>
        </div>
        <div class="cell actions">
          <div class="dropdown">
            <button class="menu-btn" @click="toggleDropdown(server.name)">⋮</button>
            <div class="menu" v-if="openedMenu === server.name">
              <div class="menu-item" @click="updateLoad(server.name)">Обновить нагрузку</div>
              <div class="menu-item" @click="recheckStatus(server.name)">Обновить статус</div>
              <div class="menu-item" @click="removeServer(server.name)">Удалить</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-row">Нет серверов</div>

    <div v-if="showForm" class="side-panel">
      <div class="side-panel-header">
        <span>Добавить сервер</span>
        <button class="close-btn" @click="closeForm">×</button>
      </div>
      <div class="side-panel-content">
        <form @submit.prevent="addServer">
          <label>Имя:</label>
          <input v-model="form.name" required />

          <label>Хост:</label>
          <input v-model="form.host" required />

          <label>Имя пользователя:</label>
          <input v-model="form.specify_username" required />

          <label>Порт:</label>
          <input v-model="form.port" type="number" required />

          <label>Пароль:</label>
          <input v-model="form.password" type="password" required />

          <div class="form-actions">
            <button type="submit">Сохранить</button>
            <button type="button" @click="closeForm">Отмена</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import api from '@/api/axios'
import '../../assets/styles/settings/ServersSettings.css'

const form = ref({
  name: '',
  host: '',
  specify_username: '',
  port: 22,
  password: ''
})

const servers = ref([])
const showForm = ref(false)
const openedMenu = ref(null)
let interval = null

const loadServers = async () => {
  try {
    const res = await api.get('/settings/servers')
    servers.value = res.data
  } catch (err) {
    console.error('Ошибка загрузки серверов', err)
  }
}

const addServer = async () => {
  try {
    await api.post('/settings/servers', form.value)
    showForm.value = false
    await loadServers()
  } catch (e) {
    console.error('Ошибка добавления сервера', e)
  }
}

const removeServer = async (name) => {
  try {
    await api.delete(`/settings/servers/${name}`)
    openedMenu.value = null
    await loadServers()
  } catch (e) {
    console.error('Ошибка удаления сервера', e)
  }
}

const updateLoad = async (name) => {
  try {
    await api.post(`/settings/servers/${name}/update-load`)
    await loadServers()
  } catch (e) {
    console.error('Ошибка обновления нагрузки', e)
  }
}

const recheckStatus = async (name) => {
  try {
    await api.post(`/settings/servers/${name}/status`)
    await loadServers()
  } catch (e) {
    console.error('Ошибка обновления статуса сервера', e)
  }
}

const statusClass = (status) => {
  switch (status) {
    case 'Successful':
      return 'status-success'
    case 'Error':
      return 'status-error'
    case 'Unknown':
    default:
      return 'status-unknown'
  }
}

const toggleDropdown = (name) => {
  openedMenu.value = openedMenu.value === name ? null : name
}

const closeForm = () => {
  showForm.value = false
}

onMounted(() => {
  loadServers()
  interval = setInterval(loadServers, 60000)

  window.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown')) {
      openedMenu.value = null
    }
  })
})

onBeforeUnmount(() => {
  clearInterval(interval)
})
</script>
