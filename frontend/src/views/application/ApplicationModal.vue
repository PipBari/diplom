<template>
  <div class="overlay">
    <div class="side-panel">
      <div class="side-panel-header">
        <span>Создать приложение</span>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>
      <div class="side-panel-content">
        <form @submit.prevent="submitApp">
          <label>Имя:</label>
          <input v-model="app.name" required />

          <label>Репозиторий:</label>
          <select v-model="app.repoName" required>
            <option disabled value="">-- выберите репозиторий --</option>
            <option v-for="r in repos" :key="r.name" :value="r.name">
              {{ r.name }} — {{ r.repoUrl }}
            </option>
          </select>

          <label>Ветка:</label>
          <input v-model="app.branch" required />

          <label>Путь:</label>
          <input v-model="app.path" required />

          <label>Проект:</label>
          <select v-model="app.projectName">
            <option disabled value="">-- выберите проект --</option>
            <option v-for="p in projects" :key="p.name" :value="p.name">
              {{ p.name }}
            </option>
          </select>

          <label>Сервер (необязательно):</label>
          <select v-model="app.serverName">
            <option value="">— не выбрано —</option>
            <option v-for="s in servers" :key="s.name" :value="s.name">
              {{ s.name }} — {{ s.host }}
            </option>
          </select>

          <label>Синхронизация:</label>
          <select v-model="app.syncStrategy">
            <option value="manual">Manual</option>
            <option value="auto">Auto</option>
          </select>

          <div class="form-actions">
            <button type="submit">Создать</button>
            <button type="button" @click="$emit('close')">Отмена</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import api from '@/api/axios'

const emit = defineEmits(['submit', 'close'])

const app = reactive({
  name: '',
  repoName: '',
  branch: '',
  path: '',
  projectName: '',
  serverName: '',
  syncStrategy: 'manual'
})

const repos = ref([])
const projects = ref([])
const servers = ref([])

onMounted(async () => {
  const [r, p, s] = await Promise.all([
    api.get('/settings/git'),
    api.get('/settings/projects'),
    api.get('/settings/servers')
  ])
  repos.value = r.data
  projects.value = p.data
  servers.value = s.data
})

const validateBeforeSubmit = async () => {
  const { repoName, branch, path } = app
  try {
    const [branchRes, pathRes] = await Promise.all([
      api.get(`/git/writer/${repoName}/branch/${branch}/exists`),
      api.get(`/git/writer/${repoName}/branch/${branch}/path/exists`, {
        params: { path }
      })
    ])

    if (!branchRes.data) {
      alert(`Ветка '${branch}' не найдена`)
      return false
    }
    if (!pathRes.data) {
      alert(`Путь '${path}' не существует или указывает на корень`)
      return false
    }

    return true
  } catch (e) {
    alert('Ошибка валидации')
    return false
  }
}

const submitApp = async () => {
  if (!(await validateBeforeSubmit())) return
  emit('submit', { ...app, createdAt: new Date().toISOString() })
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
}
.side-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 420px;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
}
.side-panel-header {
  display: flex;
  justify-content: space-between;
  padding: 1rem;
  font-weight: bold;
  border-bottom: 1px solid #ccc;
}
.side-panel-content {
  padding: 1rem;
  overflow-y: auto;
}
input, select {
  display: block;
  width: 100%;
  margin-bottom: 0.8rem;
  padding: 6px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
