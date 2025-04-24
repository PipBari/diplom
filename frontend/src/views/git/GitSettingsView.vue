<template>
  <div class="git-settings">
    <div class="breadcrumbs">Настройки / Репозитории</div>

    <div class="top-bar">
      <button @click="showForm = true">Подключить репозиторий</button>
      <button @click="loadRepos">Обновить список</button>
    </div>

    <div class="table-header">
      <div>ТИП</div>
      <div>ИМЯ</div>
      <div>ВЕТКА</div>
      <div>РЕПОЗИТОРИЙ</div>
      <div>СТАТУС ПОДКЛЮЧЕНИЯ</div>
      <div></div>
    </div>

    <div v-if="repos.length">
      <div v-for="repo in repos" :key="repo.name" class="repo-row">
        <div class="cell">{{ repo.type }}</div>
        <div class="cell">{{ repo.name || 'по умолчанию' }}</div>
        <div class="cell">{{ repo.branch }}</div>
        <div class="cell">
          <a :href="repo.repoUrl" target="_blank">{{ repo.repoUrl }}</a>
        </div>
        <div class="cell">
          <span :class="['status', statusClass(repo.status)]">{{ repo.status }}</span>
        </div>
        <div class="cell actions">
          <div class="dropdown">
            <button class="menu-btn" @click="toggleDropdown(repo.name)">⋮</button>
            <div class="menu" v-if="openedMenu === repo.name">
              <div class="menu-item" @click="recheckRepoStatus(repo.name)">Проверить</div>
              <div class="menu-item" @click="removeRepo(repo.name)">Удалить</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-row">Нет подключённых репозиториев</div>

    <div v-if="showForm" class="side-panel">
      <div class="side-panel-header">
        <span>Добавить репозиторий</span>
        <button class="close-btn" @click="showForm = false">×</button>
      </div>
      <div class="side-panel-content">
        <form @submit.prevent="addRepo">
          <label>Имя:</label>
          <input v-model="form.name" required />

          <label>Repo URL:</label>
          <input v-model="form.repoUrl" required />

          <label>Branch:</label>
          <input v-model="form.branch" required />

          <label>Username:</label>
          <input v-model="form.username" required />

          <label>Token:</label>
          <input v-model="form.token" type="password" required />

          <div class="form-actions">
            <button type="submit">Сохранить</button>
            <button type="button" @click="showForm = false">Отмена</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

import '../../assets/styles/git/GitConnectStyle.css'

const form = ref({
  name: '',
  repoUrl: '',
  branch: '',
  username: '',
  token: ''
})

const repos = ref([])
const showForm = ref(false)
const openedMenu = ref(null)

const loadRepos = async () => {
  try {
    const res = await axios.get('http://localhost:8080/settings/git')
    repos.value = res.data
  } catch (err) {
    console.error('Ошибка загрузки', err)
  }
}

const addRepo = async () => {
  try {
    await axios.post('http://localhost:8080/settings/git', form.value)
    showForm.value = false
    await loadRepos()
  } catch (e) {
    console.error('Ошибка добавления', e)
  }
}

const removeRepo = async (name) => {
  try {
    await axios.delete(`http://localhost:8080/settings/git/${name}`)
    openedMenu.value = null
    await loadRepos()
  } catch (e) {
    console.error('Ошибка удаления', e)
  }
}

const recheckRepoStatus = async (name) => {
  try {
    await axios.post(`http://localhost:8080/settings/git/${name}/status`)
    openedMenu.value = null
    await loadRepos()
  } catch (e) {
    console.error('Ошибка обновления статуса', e)
  }
}

const statusClass = (status) => {
  if (status === 'Successful') return 'ok'
  if (status === 'Error') return 'fail'
  return 'unknown'
}

const toggleDropdown = (name) => {
  openedMenu.value = openedMenu.value === name ? null : name
}

onMounted(() => {
  loadRepos()

  window.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown')) {
      openedMenu.value = null
    }
  })
})
</script>
