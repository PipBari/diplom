<template>
  <div class="git-settings">
    <div class="breadcrumbs">Настройки / Репозитории</div>

    <div class="top-bar">
      <button @click="openCreateForm">Подключить репозиторий</button>
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
      <div
          v-for="repo in repos"
          :key="repo.name"
          class="repo-row"
          @click="editRepo(repo)"
      >
        <div class="cell">{{ repo.type }}</div>
        <div class="cell">{{ repo.name || 'по умолчанию' }}</div>
        <div class="cell">{{ repo.branch }}</div>
        <div class="cell">
          <a :href="repo.repoUrl" target="_blank" @click.stop>{{ repo.repoUrl }}</a>
        </div>
        <div class="cell">
          <span :class="['status', statusClass(repo.status)]">{{ repo.status }}</span>
        </div>
        <div class="cell actions" @click.stop>
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
        <span>{{ isEditMode ? 'Редактировать репозиторий' : 'Подключить репозиторий' }}</span>
        <button class="close-btn" @click="closeForm">×</button>
      </div>
      <div class="side-panel-content">
        <form @submit.prevent="saveRepo">
          <label>Имя:</label>
          <input v-model="form.name" :readonly="isEditMode" required />

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
            <button type="button" @click="closeForm">Отмена</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
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
const isEditMode = ref(false)
const editName = ref(null)
const openedMenu = ref(null)

const loadRepos = async () => {
  try {
    const res = await api.get('/settings/git')
    repos.value = res.data
  } catch (err) {
    console.error('Ошибка загрузки', err)
  }
}

const openCreateForm = () => {
  form.value = { name: '', repoUrl: '', branch: '', username: '', token: '' }
  isEditMode.value = false
  editName.value = null
  showForm.value = true
}

const editRepo = (repo) => {
  form.value = { ...repo }
  isEditMode.value = true
  editName.value = repo.name
  showForm.value = true
}

const saveRepo = async () => {
  try {
    if (isEditMode.value) {
      await api.put(`/settings/git/${editName.value}`, form.value)
    } else {
      await api.post('/settings/git', form.value)
    }
    closeForm()
    await loadRepos()
  } catch (e) {
    console.error('Ошибка сохранения', e)
  }
}

const removeRepo = async (name) => {
  try {
    await api.delete(`/settings/git/${name}`)
    openedMenu.value = null
    await loadRepos()
  } catch (e) {
    console.error('Ошибка удаления', e)
  }
}

const recheckRepoStatus = async (name) => {
  try {
    await api.post(`/settings/git/${name}/status`)
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

const closeForm = () => {
  showForm.value = false
  isEditMode.value = false
  editName.value = null
  form.value = { name: '', repoUrl: '', branch: '', username: '', token: '' }
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
