<template>
  <div class="project-settings">
    <div class="breadcrumbs">Настройки / Проекты</div>

    <div class="top-bar">
      <button @click="openCreateForm">Добавить проект</button>
      <button @click="loadProjects">Обновить список</button>
    </div>

    <div class="table-header">
      <div>ИМЯ</div>
      <div>ОПИСАНИЕ</div>
    </div>

    <div v-if="projects.length">
      <div
          v-for="project in projects"
          :key="project.name"
          class="repo-row"
          @click="editProject(project)"
      >
        <div class="cell">{{ project.name }}</div>
        <div class="cell">{{ project.description }}</div>
        <div class="cell actions" @click.stop>
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
        <span>{{ isEditMode ? 'Редактировать проект' : 'Добавить проект' }}</span>
        <button class="close-btn" @click="closeForm">×</button>
      </div>
      <div class="side-panel-content">
        <form @submit.prevent="saveProject">
          <label>Имя:</label>
          <input v-model="form.name" :readonly="isEditMode" required />

          <label>Описание:</label>
          <input v-model="form.description" />

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
import '../../assets/styles/settings/ProjectSettings.css'

const form = ref({
  name: '',
  description: ''
})

const projects = ref([])
const showForm = ref(false)
const isEditMode = ref(false)
const editName = ref(null)
const openedMenu = ref(null)

const loadProjects = async () => {
  try {
    const res = await api.get('/settings/projects')
    projects.value = res.data
  } catch (err) {
    console.error('Ошибка загрузки', err)
  }
}

const openCreateForm = () => {
  form.value = { name: '', description: '' }
  isEditMode.value = false
  editName.value = null
  showForm.value = true
}

const editProject = (project) => {
  form.value = { ...project }
  isEditMode.value = true
  editName.value = project.name
  showForm.value = true
}

const saveProject = async () => {
  try {
    if (isEditMode.value) {
      await api.put(`/settings/projects/${editName.value}`, form.value)
    } else {
      await api.post('/settings/projects', form.value)
    }
    closeForm()
    await loadProjects()
  } catch (e) {
    console.error('Ошибка сохранения', e)
  }
}

const removeProject = async (name) => {
  try {
    await api.delete(`/settings/projects/${name}`)
    openedMenu.value = null
    await loadProjects()
  } catch (e) {
    console.error('Ошибка удаления', e)
  }
}

const toggleDropdown = (name) => {
  openedMenu.value = openedMenu.value === name ? null : name
}

const closeForm = () => {
  showForm.value = false
  isEditMode.value = false
  editName.value = null
  form.value = { name: '', description: '' }
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
