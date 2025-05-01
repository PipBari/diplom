<template>
  <div class="editor-layout">
    <div class="file-tree">
      <ul>
        <li v-for="entry in fileTree" :key="entry.name">
          <span @click="loadEntry(entry)">
            📄 {{ entry.name }}
          </span>
        </li>
      </ul>
    </div>

    <div class="editor-pane">
      <div class="commit-header" v-if="commits.length > 0">
        <div class="commit-main">
          <span class="commit-message">💬 {{ commits[0].message }}</span>
          <span class="commit-meta"> — {{ commits[0].author }}, {{ formatDate(commits[0].date) }}</span>
        </div>

        <details class="commit-history">
          <summary>История коммитов</summary>
          <ul>
            <li v-for="c in commits" :key="c.date">
              <b>{{ formatDate(c.date) }}</b> — {{ c.author }}: {{ c.message }}
            </li>
          </ul>
        </details>
      </div>

      <div class="editor-header">
        <span>{{ currentFileName || 'Выберите файл слева' }}</span>
        <button v-if="currentFileContent !== null" @click="saveFile">Сохранить</button>
      </div>

      <textarea v-if="currentFileContent !== null" v-model="currentFileContent" class="editor-textarea" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()
const app = ref(null)
const fileTree = ref([])
const currentFileName = ref('')
const currentFileContent = ref(null)
const repo = ref(null)
const commits = ref([])

onMounted(async () => {
  const appsRes = await api.get('/applications')
  app.value = appsRes.data.find(a => a.name === route.params.name)

  const reposRes = await api.get('/settings/git')
  repo.value = reposRes.data.find(r => r.name === app.value.repoName)

  const treeRes = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/tree`, {
    params: { path: app.value.path }
  })
  fileTree.value = treeRes.data
})

const loadEntry = async (entry) => {
  const path = `${app.value.path}/${entry.name}`
  const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/file`, {
    params: { path }
  })
  currentFileContent.value = res.data
  currentFileName.value = entry.name

  await loadCommits(path)
}

const loadCommits = async (path) => {
  try {
    const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/commits`, {
      params: { path, limit: 5 }
    })
    commits.value = res.data
  } catch (e) {
    console.error('Ошибка при загрузке коммитов:', e)
  }
}

const formatDate = (raw) => {
  const normalized = raw.replace('MSK', '+03:00')
  const d = new Date(normalized)
  return d.toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const saveFile = async () => {
  const path = `${app.value.path}/${currentFileName.value}`
  await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/save`, {
    path,
    content: currentFileContent.value,
    commitMessage: `Обновление файла ${currentFileName.value}`
  })
  alert('Файл сохранён и отправлен в Git')
}
</script>

<style scoped>
.editor-layout {
  display: flex;
  height: calc(100vh - 60px);
}

.file-tree {
  width: 250px;
  background: #f3f4f6;
  border-right: 1px solid #ddd;
  padding: 1rem;
  overflow-y: auto;
}

.editor-pane {
  flex: 1;
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

.commit-header {
  margin-bottom: 0.8rem;
  font-size: 0.9rem;
  color: #444;
}

.commit-main {
  margin-bottom: 0.3rem;
}

.commit-message {
  font-weight: 500;
}

.commit-meta {
  color: #666;
  font-size: 0.85rem;
}

.commit-history summary {
  cursor: pointer;
  margin-bottom: 0.3rem;
  color: #3b82f6;
}

.commit-history ul {
  padding-left: 1rem;
  font-size: 0.85rem;
  line-height: 1.4;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.editor-textarea {
  flex: 1;
  width: 100%;
  font-family: monospace;
  font-size: 14px;
  padding: 1rem;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  resize: none;
}
</style>
