<template>
  <div class="editor-layout">
    <div class="file-tree" @contextmenu.prevent="showContextMenu($event, null)">
      <FileTreeNode
          v-for="child in rootEntry?.children || []"
          :key="child.fullPath"
          :node="child"
          :fullPath="child.fullPath"
          :depth="0"
          @open-file="loadEntry"
          @context-menu="showContextMenu"
      />
    </div>

    <div class="editor-pane">
      <div class="commit-header" v-if="commits.length > 0">
        <div class="commit-main">
          💬 {{ commits[0].message }} — {{ commits[0].author }}, {{ formatDate(commits[0].date) }}
        </div>
        <details>
          <summary>▶ История коммитов</summary>
          <ul>
            <li v-for="c in commits" :key="c.date">
              <b>{{ formatDate(c.date) }}</b> — {{ c.author }}: {{ c.message }}
            </li>
          </ul>
        </details>
      </div>

      <div class="editor-header">
        <span>{{ currentFileName || 'Выберите файл' }}</span>
        <button v-if="currentFileContent !== null" @click="saveFile">Сохранить</button>
      </div>

      <textarea
          v-if="currentFileContent !== null"
          v-model="currentFileContent"
          class="editor-textarea"
      />

      <div v-if="validationTip" class="tip-block">💡 {{ validationTip }}</div>
      <div v-if="validationStatus" class="status-block">{{ validationStatus }}</div>
      <div v-if="validationError" class="error-block">❌ {{ validationError }}</div>
    </div>

    <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px` }"
    >
      <div class="context-item" @click="openNewFileDialog">📄 Новый файл</div>
      <div class="context-item" @click="openNewFolderDialog">📁 Новая папка</div>
      <div
          class="context-item danger"
          v-if="contextMenu.node"
          @click="deletePath"
      >🗑 Удалить</div>
    </div>

    <!-- Модалки создания -->
    <div v-if="showNewFileDialog" class="overlay">
      <div class="dialog">
        <h3>Новый файл</h3>
        <input v-model="newFileName" placeholder="example.tf" />
        <div class="dialog-actions">
          <button @click="createNewFile">Создать</button>
          <button @click="showNewFileDialog = false">Отмена</button>
        </div>
      </div>
    </div>

    <div v-if="showNewFolderDialog" class="overlay">
      <div class="dialog">
        <h3>Новая папка</h3>
        <input v-model="newFolderName" placeholder="папка" />
        <div class="dialog-actions">
          <button @click="createNewFolder">Создать</button>
          <button @click="showNewFolderDialog = false">Отмена</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import FileTreeNode from '@/components/FileTreeNode.vue'

// Импорт внешнего CSS-файла
import '@/assets/styles/application/ApplicationEditorView.css'

const route = useRoute()
const app = ref({})
const repo = ref({})
const rootEntry = ref(null)
const commits = ref([])

const currentFileName = ref('')
const currentFileContent = ref(null)

const showNewFileDialog = ref(false)
const showNewFolderDialog = ref(false)
const newFileName = ref('')
const newFolderName = ref('')

const validationError = ref(null)
const validationStatus = ref(null)
const validationTip = ref(null)

const contextMenu = ref({ visible: false, x: 0, y: 0, node: null })

onMounted(async () => {
  const appsRes = await api.get('/applications')
  app.value = appsRes.data.find(a => a.name === route.params.name)
  const reposRes = await api.get('/settings/git')
  repo.value = reposRes.data.find(r => r.name === app.value.repoName)
  await refreshTree()
  document.addEventListener('click', () => (contextMenu.value.visible = false))
})

const formatDate = (raw) => {
  const normalized = raw.replace('MSK', '+03:00')
  return new Date(normalized).toLocaleString('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

const refreshTree = async () => {
  const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/entry`, {
    params: { path: app.value.path }
  })
  rootEntry.value = enrichEntry(res.data, '')
}

const enrichEntry = (entry, parentPath) => {
  entry.fullPath = parentPath ? `${parentPath}/${entry.name}` : entry.name
  entry.fullPath = entry.fullPath.replace(/\/+/g, '/')
  if (entry.children) {
    entry.children = entry.children
        .map(child => enrichEntry(child, entry.fullPath))
        .sort((a, b) => {
          if (a.type === 'folder' && b.type !== 'folder') return -1
          if (a.type !== 'folder' && b.type === 'folder') return 1
          return a.name.localeCompare(b.name)
        })
  }
  return entry
}

const loadEntry = async (path) => {
  if (!path || path.endsWith('/')) return
  try {
    const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/file`, {
      params: { path }
    })
    currentFileContent.value = res.data
    currentFileName.value = path
    await loadCommits(path)
    setTip(path)
  } catch (e) {
    validationError.value = 'Ошибка при загрузке файла'
    currentFileContent.value = null
    currentFileName.value = ''
  }
}

const loadCommits = async (path) => {
  try {
    const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/commits`, {
      params: { path, limit: 5 }
    })
    commits.value = res.data
  } catch {
    commits.value = []
  }
}

const saveFile = async () => {
  const filename = currentFileName.value
  const type = detectType(filename)
  validationStatus.value = '🔄 Проверка синтаксиса...'
  validationError.value = null

  if (type) {
    const res = await api.post(`/validate/${type}`, {
      path: filename,
      content: currentFileContent.value
    })
    if (!res.data.valid) {
      validationError.value = res.data.output
      validationStatus.value = '❌ Ошибка валидации'
      return
    }
    validationStatus.value = '✅ Синтаксис корректен'
  }

  await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/save`, {
    path: filename,
    content: currentFileContent.value,
    commitMessage: `Обновление файла ${filename}`
  })

  await refreshTree()
}

const detectType = (filename) => {
  if (filename.endsWith('.tf')) return 'terraform'
  if (filename.endsWith('.yml') || filename.endsWith('.yaml')) return 'ansible'
  return null
}

const setTip = (filename) => {
  if (filename.endsWith('.tf')) validationTip.value = 'Terraform: будет применена terraform validate'
  else if (filename.endsWith('.yml') || filename.endsWith('.yaml')) validationTip.value = 'Ansible: будет применён ansible-lint'
  else validationTip.value = null
}

const openNewFileDialog = () => {
  showNewFileDialog.value = true
  contextMenu.value.visible = false
}
const openNewFolderDialog = () => {
  showNewFolderDialog.value = true
  contextMenu.value.visible = false
}
const showContextMenu = (event, node) => {
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    node
  }
}

const createNewFile = () => {
  const name = newFileName.value.trim()
  if (!name) {
    validationError.value = 'Имя файла не может быть пустым'
    return
  }
  currentFileName.value = `${app.value.path}/${name}`.replace(/\/+/g, '/')
  currentFileContent.value = ''
  showNewFileDialog.value = false
  setTip(name)
}

const createNewFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name) {
    validationError.value = 'Имя папки не может быть пустым'
    return
  }

  await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/create-folder`, {
    path: `${app.value.path}/${name}`.replace(/\/+/g, '/'),
    commitMessage: `Создание папки ${name}`
  })
  showNewFolderDialog.value = false
  await refreshTree()
}

const deletePath = async () => {
  if (!contextMenu.value.node) return
  const confirmed = confirm(`Удалить ${contextMenu.value.node.name}?`)
  if (!confirmed) return

  await api.delete(`/git/writer/${repo.value.name}/branch/${app.value.branch}/delete`, {
    params: {
      path: contextMenu.value.node.fullPath,
      commitMessage: `Удаление ${contextMenu.value.node.name}`
    }
  })
  contextMenu.value.visible = false
  await refreshTree()
}
</script>
