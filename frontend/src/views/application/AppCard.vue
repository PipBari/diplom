<template>
  <div class="card" @click="goToDetails">
    <div class="card-header">
      <h3>{{ app.name }}</h3>
      <div class="dropdown" @click.stop>
        <button class="menu-btn" @click.stop="toggleMenu">⋮</button>
        <div v-if="menuOpen" class="menu">
          <div class="menu-item" @click="$emit('sync', app.name)">Синхронизировать</div>
          <div class="menu-item" @click="$emit('delete', app.name)">Удалить</div>
        </div>
      </div>
    </div>
    <div class="card-body">
      <p><b>Проект:</b> {{ app.projectName || '—' }}</p>
      <p><b>Состояние:</b>
        <span :class="['status', statusClass]">{{ app.status || 'Not Synced' }}</span>
      </p>

      <p class="truncate-url">
        <b>Репозиторий:</b>
        <a :href="getRepoUrl(app.repoName)" target="_blank" @click.stop>
          {{ getRepoUrl(app.repoName) }}
        </a>
      </p>

      <p><b>Ветка:</b> {{ app.branch || '—' }}</p>
      <p><b>Путь:</b> {{ app.path || '—' }}</p>
      <p><b>Сервер:</b> {{ app.serverName || '—' }}</p>
      <p><b>Синхронизация:</b> {{ app.syncStrategy || 'manual' }}</p>
      <p><b>Создано:</b> {{ formatDate(app.createdAt) }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const props = defineProps({ app: Object })
const emit = defineEmits(['sync', 'delete'])

const router = useRouter()
const menuOpen = ref(false)
const repos = ref([])

const toggleMenu = () => menuOpen.value = !menuOpen.value

const goToDetails = () => {
  router.push(`/applications/${props.app.name}`)
}

onMounted(async () => {
  const res = await api.get('/settings/git')
  repos.value = res.data
})

const getRepoUrl = (repoName) => {
  const repo = repos.value.find(r => r.name === repoName)
  return repo ? repo.repoUrl : repoName
}

const statusClass = computed(() => {
  if (props.app.status === 'Successful' || props.app.status === 'Synced') return 'ok'
  if (props.app.status === 'Error') return 'fail'
  return 'unknown'
})

const formatDate = (iso) => {
  const date = new Date(iso)
  return date.toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

window.addEventListener('click', (e) => {
  if (!e.target.closest('.dropdown')) {
    menuOpen.value = false
  }
})
</script>

<style scoped>
.card {
  border: 1px solid #ccc;
  padding: 1rem;
  border-radius: 8px;
  background-color: #fff;
  min-width: 300px;
  max-width: 400px;
  cursor: pointer;
  transition: background 0.2s;
}
.card:hover {
  background: #f5f5f5;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-body p {
  margin: 0.4rem 0;
  word-break: break-word;
}
.truncate-url a {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status {
  font-size: 0.85rem;
  padding: 3px 6px;
  border-radius: 4px;
  margin-left: 0.4rem;
}
.status.ok {
  background: #d4edda;
  color: #155724;
}
.status.fail {
  background: #f8d7da;
  color: #721c24;
}
.status.unknown {
  background: #e2e3e5;
  color: #383d41;
}
.dropdown {
  position: relative;
}
.menu-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
}
.menu {
  position: absolute;
  top: 1.5rem;
  right: 0;
  background: #fff;
  border: 1px solid #ccc;
  min-width: 140px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 10;
}
.menu-item {
  padding: 8px 12px;
  cursor: pointer;
}
.menu-item:hover {
  background: #f0f0f0;
}
</style>
