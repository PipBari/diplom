<template>
  <div class="project-settings">
    <div class="breadcrumbs">Настройки / Проекты</div>

    <div class="top-bar">
      <button @click="showForm = true">Добавить проект</button>
      <button @click="loadProjects">Обновить список</button>
    </div>

    <div class="table-header">
      <div>ИМЯ</div>
      <div>ОПИСАНИЕ</div>
    </div>

    <div v-if="projects.length">
      <div v-for="project in projects" :key="project.name" class="repo-row">
        <div class="cell">{{ project.name }}</div>
        <div class="cell">{{ project.description }}</div>
        <div class="cell actions">
          <div class="dropdown">
            <button class="menu-btn" @click="toggleDropdown(project.name)">⋮</button>
            <div class="menu" v-if="openedMenu === project.name">
              <div class="menu-item" @click="removeProject(project.name)">Удалить</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-row">Нет проектов</div>

    <div v-if="showForm" class="side-panel">
      <div class="side-panel-header">
        <span>Добавить проект</span>
        <button class="close-btn" @click="showForm = false">×</button>
      </div>
      <div class="side-panel-content">
        <form @submit.prevent="addProject">
          <label>Имя:</label>
          <input v-model="form.name" required />

          <label>Описание:</label>
          <input v-model="form.description" />

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
import '../../assets/styles/settings/ProjectSettings.css'

const form = ref({
  name: '',
  description: ''
})

const projects = ref([])
const showForm = ref(false)
const openedMenu = ref(null)

const loadProjects = async () => {
  try {
    const res = await axios.get('http://localhost:8080/settings/projects')
    projects.value = res.data
  } catch (err) {
    console.error('Ошибка загрузки', err)
  }
}

const addProject = async () => {
  try {
    await axios.post('http://localhost:8080/settings/projects', form.value)
    showForm.value = false
    await loadProjects()
  } catch (e) {
    console.error('Ошибка добавления', e)
  }
}

const removeProject = async (name) => {
  try {
    await axios.delete(`http://localhost:8080/settings/projects/${name}`)
    openedMenu.value = null
    await loadProjects()
  } catch (e) {
    console.error('Ошибка удаления', e)
  }
}

const toggleDropdown = (name) => {
  openedMenu.value = openedMenu.value === name ? null : name
}

onMounted(() => {
  loadProjects()
  window.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown')) {
      openedMenu.value = null
    }
  })
})
</script>
