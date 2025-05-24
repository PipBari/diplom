<template>
  <div class="editor-layout">
    <div
        class="file-tree"
        @contextmenu.prevent="showContextMenu($event, null)"
        @dragover.prevent="onRootDragOver"
        @dragleave="onRootDragLeave"
        @drop="onRootDrop"
        :class="{ 'drag-over': isRootDragOver }"
    >
      <div class="file-search">
        <input
            v-model="searchQuery"
            @keyup.enter="searchInFiles"
            placeholder="Поиск по файлам..."
            class="search-input"
        />
      </div>

      <div class="search-results" v-if="searchResults.length > 0">
        <div
            v-for="r in searchResults"
            :key="r.file + ':' + r.line"
            class="search-result"
            @click="() => goToSearchResult(r)"
        >
          <strong>{{ r.file }}</strong> — строка {{ r.line }}<br />
          <code>{{ r.content }}</code>
        </div>
      </div>

      <FileTreeNode
          v-for="child in filteredRoot?.children || []"
          :key="child.fullPath"
          :node="child"
          :fullPath="child.fullPath"
          :depth="0"
          @open-file="loadEntry"
          @context-menu="(event, node) => showContextMenu(event, node)"
          @move-node="handleMoveNode"
      />
    </div>

    <div class="editor-pane">
      <div v-if="serverInfo" class="server-status-inline">
        <strong>Сервер:</strong> {{ serverInfo.name }} —
        <strong>Статус:</strong>
        <span :class="getStatusClass(serverInfo.status)">●</span>
        {{ serverInfo.status }} —
        <strong>CPU:</strong> {{ serverInfo.cpu || '—' }} —
        <strong>RAM:</strong> {{ formatRam(serverInfo.ram) }}

        <template v-if="workflowUrl && ciStatus">
          — <strong>Deployment Status:</strong>
          <a :href="workflowUrl" target="_blank">
            <span :class="getCiStatusClass(ciStatus)">●</span> {{ ciStatus }}
          </a>
        </template>
      </div>

      <div class="branch-selector">
        <label>Ветка: </label>
        <select v-model="currentBranch" @change="switchBranch">
          <option v-for="b in availableBranches" :key="b" :value="b">
            {{ b }}
          </option>
        </select>
        <button @click="showCreateBranchDialog = true">+ Ветка</button>
        <button
            @click="deleteBranch"
            :disabled="!currentBranch || currentBranch === 'main'"
        >
          🗑 Удалить
        </button>
      </div>

      <div class="commit-header" v-if="commits.length > 0">
        <div class="commit-main-wrapper">
          💬 {{ commits[0].message }} — {{ commits[0].author }},
          {{ formatDate(commits[0].date) }}
          <button @click="showRevertModal = true">↩️ Откатить</button>
        </div>
      </div>

      <div class="editor-header">
        <span>{{ currentFileName || 'Выберите файл' }}</span>
        <button v-if="currentFileContent !== null" @click="saveFile">Сохранить</button>
        <button v-if="serverInfo" @click="generateGitflow">Workflows</button>
      </div>

      <div style="width: 100%; height: 100%; min-width: 0">
        <MonacoEditor
            v-if="currentFileContent !== null"
            :value="currentFileContent"
            :language="getLanguageForFile(currentFileName)"
            :key="currentFileName"
            theme="vs"
            class="editor-textarea"
            @change="onEditorChange"
            ref="monaco"
        />
      </div>
    </div>

    <div v-if="showRevertModal" class="overlay" @click.self="showRevertModal = false">
      <div class="dialog revert-dialog">
        <h3>Откат к коммиту</h3>
        <input type="date" v-model="revertDateFilter" class="date-input" />

        <ul class="commit-list">
          <li v-for="c in paginatedCommits" :key="c.hash">
            <div class="commit-item">
              <div class="commit-text">
                <b>{{ formatDate(c.date) }}</b> — {{ c.author }}: {{ c.message }}
              </div>
              <button class="revert-button" @click="confirmRevert(c)">↩️</button>
            </div>
          </li>
        </ul>

        <div class="pagination">
          <button @click="prevPage" :disabled="currentPage === 1">← Назад</button>
          <span>Страница {{ currentPage }} / {{ totalPages }}</span>
          <button @click="nextPage" :disabled="currentPage === totalPages">Вперёд →</button>
        </div>

        <div class="dialog-actions">
          <button @click="showRevertModal = false">Закрыть</button>
        </div>
      </div>
    </div>

    <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px` }"
    >
      <div class="context-item" @click="openNewFileDialog">📄 Новый файл</div>
      <div class="context-item" @click="openNewFolderDialog">📁 Новая папка</div>
      <div v-if="contextMenu.node" class="context-item" @click="openRenameDialog">
        ✏️ Переименовать
      </div>
      <div
          v-if="contextMenu.node && canDelete(contextMenu.node.name)"
          class="context-item danger"
          @click="deletePath"
      >
        🗑 Удалить
      </div>
    </div>

    <div v-if="showNewFileDialog" class="overlay" @click.self="showNewFileDialog = false">
      <div class="dialog">
        <h3>Новый файл</h3>
        <input v-model="newFileName" placeholder="example.tf" />
        <div class="dialog-actions">
          <button @click="createNewFile">Создать</button>
          <button @click="showNewFileDialog = false">Отмена</button>
        </div>
      </div>
    </div>

    <div
        v-if="showNewFolderDialog"
        class="overlay"
        @click.self="showNewFolderDialog = false"
    >
      <div class="dialog">
        <h3>Новая папка</h3>
        <input v-model="newFolderName" placeholder="папка" />
        <div class="dialog-actions">
          <button @click="createNewFolder">Создать</button>
          <button @click="showNewFolderDialog = false">Отмена</button>
        </div>
      </div>
    </div>

    <div v-if="showRenameDialog" class="overlay" @click.self="showRenameDialog = false">
      <div class="dialog">
        <h3>Переименовать</h3>
        <input
            v-model="renameNewName"
            :placeholder="contextMenu.node?.name"
            @keyup.enter="renameEntry"
        />
        <div class="dialog-actions">
          <button @click="renameEntry">Переименовать</button>
          <button @click="showRenameDialog = false">Отмена</button>
        </div>
      </div>
    </div>

    <div
        v-if="showCreateBranchDialog"
        class="overlay"
        @click.self="showCreateBranchDialog = false"
    >
      <div class="dialog">
        <h3>Создание новой ветки</h3>
        <input v-model="newBranchName" placeholder="feature/my-branch" />
        <div class="dialog-actions">
          <button @click="createBranch">Создать</button>
          <button @click="showCreateBranchDialog = false">Отмена</button>
        </div>
      </div>
    </div>

    <div class="toast-container">
      <div v-for="(toast, index) in toasts" :key="index" class="toast" :class="toast.type">
        {{ toast.message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import FileTreeNode from '@/components/FileTreeNode.vue'
import { computed } from 'vue'
import MonacoEditor from '@guolao/vue-monaco-editor'
import '@/assets/styles/application/ApplicationEditorView.css'

const server = ref(null)

const showRevertModal = ref(false)
const revertDateFilter = ref('')

const currentPage = ref(1)
const commitsPerPage = 5

const route = useRoute()
const app = ref({})
const repo = ref({})
const rootEntry = ref(null)
const commits = ref([])
const serverInfo = ref(null)

const monaco = ref(null)

const currentFileName = ref('')
const currentFileContent = ref('')

const showNewFileDialog = ref(false)
const showNewFolderDialog = ref(false)
const showRenameDialog = ref(false)

const searchQuery = ref('')
const searchResults = ref([])

const newFileName = ref('')
const newFolderName = ref('')
const renameNewName = ref('')

const workflowUrl = ref(null)
const ciStatus = ref(null)

const currentBranch = ref('')
const availableBranches = ref([])
const showCreateBranchDialog = ref(false)
const newBranchName = ref('')

const contextMenu = ref({ visible: false, x: 0, y: 0, node: null })
const showCommitHistory = ref(false)

const toasts = ref([])
const addToast = (message, type = 'success') => {
  const id = Date.now()
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 3000)
}

const getStatusClass = (status) => {
  return {
    Successful: 'status-success',
    Failed: 'status-failed',
    Unknown: 'status-unknown'
  }[status] || 'status-unknown'
}

const formatRam = (raw) => {
  if (!raw) return '—'
  const [used, total] = raw.split('/')
  return `${used.trim()} / ${total.trim()}`
}

onMounted(async () => {
  try {
    const appsRes = await api.get('/applications')
    app.value = appsRes.data.find(a => a.name === route.params.name)
    if (!app.value) throw new Error('Приложение не найдено')

    const serversRes = await api.get('/settings/servers')
    server.value = serversRes.data.find(s => s.name === app.value.serverName)
    serverInfo.value = server.value

    if (app.value.serverName) {
      setInterval(async () => {
        try {
          const updatedServers = await api.get('/settings/servers')
          const updatedInfo = updatedServers.data.find(s => s.name === app.value.serverName)
          if (updatedInfo) serverInfo.value = updatedInfo
        } catch (err) {
          console.error('Ошибка при автообновлении serverInfo:', err)
        }
      }, 60000)
    }

    const reposRes = await api.get('/settings/git')
    repo.value = reposRes.data.find(r => r.name === app.value.repoName || r.repoUrl === app.value.repoName)

    if (!repo.value || !repo.value.repoUrl) {
      console.error('Репозиторий не найден или repoUrl отсутствует:', app.value.repoName)
      addToast('Ошибка: репозиторий не найден или некорректен', 'error')
      return
    }

    await refreshTree()
    await pollCiStatus()
    await fetchBranches()
    await refreshTree()

    document.addEventListener('click', () => (contextMenu.value.visible = false))
  } catch (err) {
    console.error('Ошибка при инициализации редактора:', err.message)
    addToast('Ошибка при загрузке приложения или сервера', 'error')
  }
})

onUnmounted(() => {
  if (pollTimeout) clearTimeout(pollTimeout)
})

const generateGitflow = async () => {
  try {
    const res = await api.post(`/applications/${app.value.name}/gitflow`)
    addToast(res.data, 'success')
  } catch (e) {
    addToast(e.response?.data || 'Ошибка при генерации gitflow', 'error')
  }
}

const refreshTree = async () => {
  const path = app.value.path || ''
  const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/entry`, {
    params: { path }
  })
  rootEntry.value = enrichEntry(res.data, '')
}

const enrichEntry = (entry, parentPath) => {
  entry.fullPath = parentPath ? `${parentPath}/${entry.name}` : entry.name
  entry.fullPath = entry.fullPath.replace(/\/+/g, '/')

  if (!entry.children) {
    entry.children = []
  }

  if (entry.children && Array.isArray(entry.children)) {
    entry.children = entry.children
        .filter(child => child.name !== '.gitkeep')
        .map(child => enrichEntry(child, entry.fullPath))
        .sort((a, b) => {
          if (a.type === 'folder' && b.type !== 'folder') return -1
          if (a.type !== 'folder' && b.type === 'folder') return 1
          return a.name.localeCompare(b.name)
        })
  }

  return entry
}

const fetchBranches = async () => {
  try {
    const res = await api.post(`/git/writer/branches`, {
      url: repo.value.repoUrl,
      username: repo.value.username,
      token: repo.value.token
    })
    availableBranches.value = res.data
    currentBranch.value = app.value.branch
  } catch (e) {
    addToast('Ошибка при загрузке веток', 'error')
  }
}

const switchBranch = async () => {
  app.value.branch = currentBranch.value
  await refreshTree()
  await loadCommits(currentFileName.value)
  addToast(`Переключено на ветку ${currentBranch.value}`, 'success')
}

const createBranch = async () => {
  const name = newBranchName.value.trim()
  if (!name) {
    addToast('Имя ветки не может быть пустым', 'error')
    return
  }

  try {
    await api.post(`/git/writer/repo/branch`, {
      url: repo.value.repoUrl,
      username: repo.value.username,
      token: repo.value.token,
      name,
      from: currentBranch.value
    })

    const pathExistsRes = await api.get(`/git/writer/${repo.value.name}/branch/${name}/path/exists`, {
      params: { path: app.value.path }
    })

    if (!pathExistsRes.data) {
      await api.post(`/git/writer/${repo.value.name}/branch/${name}/create-folder`, {
        path: app.value.path,
        commitMessage: `Инициализация директории ${app.value.path}`
      })
    }

    await fetchBranches()
    currentBranch.value = name
    app.value.branch = name
    showCreateBranchDialog.value = false
    await refreshTree()
    addToast(`Ветка ${name} создана и готова к работе`, 'success')
  } catch (e) {
    addToast(e.response?.data || 'Ошибка при создании ветки', 'error')
  }
}

const deleteBranch = async () => {
  if (!currentBranch.value) {
    addToast('Ветка не выбрана', 'error')
    return
  }

  const confirmed = confirm(`Удалить ветку "${currentBranch.value}"?`)
  if (!confirmed) return

  try {
    await api.request({
      url: `/git/writer/repo/branch`,
      method: 'delete',
      data: {
        url: repo.value.repoUrl,
        username: repo.value.username,
        token: repo.value.token,
        branch: currentBranch.value
      },
      headers: {
        'Content-Type': 'application/json'
      }
    })

    addToast(`Ветка "${currentBranch.value}" удалена`, 'success')

    await fetchBranches()

    currentBranch.value = availableBranches.value[0] || ''
    app.value.branch = currentBranch.value

    if (currentBranch.value) {
      await refreshTree()
    }

  } catch (e) {
    addToast(e.response?.data || 'Ошибка при удалении ветки', 'error')
  }
}

const loadEntry = async (path) => {
  if (!path || path.endsWith('/')) return

  try {
    currentFileContent.value = ''
    currentFileName.value = path
    const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/file`, {
      params: { path }
    })

    await nextTick()
    currentFileContent.value = res.data
    await loadCommits(path)
  } catch (e) {
    addToast('Ошибка при загрузке файла', 'error')
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

  let filesToValidate = []

  if (type === 'terraform') {
    try {
      const allEntries = []
      const collectFiles = (node) => {
        if (node.type === 'file' && node.name.toLowerCase().endsWith('.tf')) {
          allEntries.push(node)
        }
        if (node.children) {
          node.children.forEach(collectFiles)
        }
      }
      collectFiles(rootEntry.value)

      const allFiles = await Promise.all(
          allEntries.map(async (node) => {
            try {
              const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/file`, {
                params: { path: node.fullPath }
              })
              return {
                filename: node.fullPath,
                content: res.data,
                serverName: serverInfo.value?.name || null
              }
            } catch {
              return null
            }
          })
      )

      filesToValidate = allFiles.filter(Boolean).filter(f => f.filename !== filename)
      filesToValidate.push({
        filename,
        content: currentFileContent.value,
        serverName: serverInfo.value?.name || null
      })

      const res = await api.post(`/validate/${type}`, filesToValidate)
      if (!res.data.valid) {
        addToast(res.data.output || 'Ошибка валидации', 'error')
        return
      }

    } catch (e) {
      const error = e.response?.data?.output || e.response?.data?.message || e.message
      addToast(error || 'Ошибка валидации', 'error')
      return
    }

  } else if (type === 'ansible') {
    filesToValidate = [{
      filename,
      content: currentFileContent.value,
      serverName: serverInfo.value?.name || null
    }]

    try {
      const res = await api.post(`/validate/${type}`, filesToValidate)
      if (!res.data.valid) {
        addToast(res.data.output || 'Ошибка валидации', 'error')
        return
      }
    } catch (e) {
      const error = e.response?.data?.output || e.response?.data?.message || e.message
      addToast(error || 'Ошибка валидации', 'error')
      return
    }

  } else if (type === 'bash') {
    filesToValidate = [{
      filename,
      content: currentFileContent.value,
      serverName: serverInfo.value?.name || null
    }]

    try {
      const res = await api.post(`/validate/${type}`, filesToValidate)
      if (!res.data.valid) {
        addToast(res.data.output || 'Ошибка валидации', 'error')
        return
      }
    } catch (e) {
      const error = e.response?.data?.output || e.response?.data?.message || e.message
      addToast(error || 'Ошибка валидации', 'error')
      return
    }
  }

  try {
    await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/save`, {
      path: filename,
      content: currentFileContent.value,
      commitMessage: `Обновление файла ${filename}`,
      serverName: serverInfo.value?.name || null,
      allFiles: filesToValidate
    })

    await refreshTree()
    addToast(`Файл ${filename} сохранён`, 'success')
  } catch (e) {
    addToast(e.response?.data?.message || e.message, 'error')
  }
}

const revertCommit = async (commit) => {
  const confirmed = confirm(`Вы уверены, что хотите отменить коммит: "${commit.message}"?`)
  if (!confirmed) return

  try {
    await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/revert`, {
      commitHash: commit.hash,
      commitMessage: `Revert: ${commit.message}`
    })
    addToast('Коммит отменён', 'success')
    await refreshTree()
    await loadCommits(currentFileName.value)
  } catch (e) {
    addToast('Ошибка при откате коммита', 'error')
  }
}

const detectType = (filename) => {
  if (filename.endsWith('.tf')) return 'terraform'
  if (filename.endsWith('.yml') || filename.endsWith('.yaml')) return 'ansible'
  if (filename.endsWith('.sh')) return 'bash'
  return null
}

const getLanguageForFile = (filename) => {
  if (filename.endsWith('.tf')) return 'hcl'
  if (filename.endsWith('.yaml') || filename.endsWith('.yml')) return 'yaml'
  if (filename.endsWith('.sh')) return 'shell'
  return 'plaintext'
}

const openNewFileDialog = () => {
  showNewFileDialog.value = true
  contextMenu.value.visible = false
}

const openNewFolderDialog = () => {
  showNewFolderDialog.value = true
  contextMenu.value.visible = false
}

const openRenameDialog = () => {
  renameNewName.value = contextMenu.value.node?.name || ''
  showRenameDialog.value = true
  contextMenu.value.visible = false
}

const renameEntry = async () => {
  const newName = renameNewName.value.trim()
  const node = contextMenu.value.node
  if (!newName || !node) return

  const oldPath = node.fullPath
  const parts = oldPath.split('/')
  parts.pop()
  const basePath = parts.join('/')
  const newPath = `${basePath}/${newName}`.replace(/\/+/g, '/')

  try {
    await api.put(`/git/writer/${repo.value.name}/branch/${app.value.branch}/rename`, {
      oldPath,
      newPath,
      commitMessage: `Переименование ${node.name} в ${newName}`
    })
    showRenameDialog.value = false
    await refreshTree()
    addToast('Переименование успешно', 'success')
  } catch (e) {
    addToast('Ошибка при переименовании', 'error')
  }
}

const showContextMenu = (event, node) => {
  event.preventDefault()
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    node
  }
}

const canDelete = (name) => {
  return name !== '.gitkeep' && name.toLowerCase() !== 'readme.md'
}

const createNewFile = () => {
  const name = newFileName.value.trim()
  if (!name) {
    addToast('Имя файла не может быть пустым', 'error')
    return
  }

  let basePath = app.value.path

  if (contextMenu.value.node) {
    if (contextMenu.value.node.type === 'folder') {
      basePath = contextMenu.value.node.fullPath
    } else {
      const parts = contextMenu.value.node.fullPath.split('/')
      parts.pop()
      basePath = parts.join('/')
    }
  }

  const fullPath = `${basePath}/${name}`.replace(/\/+/g, '/')

  currentFileName.value = fullPath
  currentFileContent.value = ''
  showNewFileDialog.value = false
}

const createNewFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name) {
    addToast('Имя папки не может быть пустым', 'error')
    return
  }

  let basePath = app.value.path

  if (contextMenu.value.node) {
    if (contextMenu.value.node.type === 'folder') {
      basePath = contextMenu.value.node.fullPath
    } else {
      const parts = contextMenu.value.node.fullPath.split('/')
      parts.pop()
      basePath = parts.join('/')
    }
  }

  const fullPath = `${basePath}/${name}`.replace(/\/+/g, '/')

  try {
    await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/create-folder`, {
      path: fullPath,
      commitMessage: `Создание папки ${name}`
    })
    showNewFolderDialog.value = false
    await refreshTree()
    addToast('Папка создана', 'success')
  } catch (e) {
    addToast(e.response?.data || 'Ошибка при создании папки', 'error')
  }
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
  addToast('Удалено', 'success')
}

const handleMoveNode = async ({ source, targetFolder }) => {
  if (!source || !targetFolder || targetFolder.type !== 'folder') {
    addToast('Неверная папка перемещения', 'error')
    return
  }

  const name = source.fullPath.split('/').pop()
  const newPath = `${targetFolder.fullPath}/${name}`.replace(/\/+/g, '/')
  const currentParent = source.fullPath.split('/').slice(0, -1).join('/')

  if (source.fullPath === newPath || currentParent === targetFolder.fullPath) {
    addToast(`Элемент уже находится в папке "${targetFolder.fullPath}"`, 'info')
    return
  }

  try {
    await api.put(`/git/writer/${repo.value.name}/branch/${app.value.branch}/rename`, {
      oldPath: source.fullPath,
      newPath,
      commitMessage: `Перемещение ${source.name} в ${targetFolder.fullPath}`
    })

    addToast(`Перемещено в папку: ${targetFolder.fullPath}`, 'success')
    await refreshTree()
  } catch (e) {
    addToast(e.response?.data?.message || 'Ошибка при перемещении', 'error')
  }
}

const isRootDragOver = ref(false)

const onRootDragOver = () => {
  isRootDragOver.value = true
}

const onRootDragLeave = () => {
  isRootDragOver.value = false
}

const findNodeByPath = (entry, targetPath) => {
  if (!entry) return null
  if (entry.fullPath === targetPath) return entry
  if (!entry.children) return null

  for (const child of entry.children) {
    const result = findNodeByPath(child, targetPath)
    if (result) return result
  }

  return null
}

let pollTimeout = null

const pollCiStatus = async () => {
  try {
    const ciRes = await api.get(`/applications/${app.value.name}/gitflow`, {
      headers: { 'Cache-Control': 'no-cache' }
    })

    const newStatus = ciRes.data.status
    workflowUrl.value = ciRes.data.workflowUrl

    if (ciStatus.value !== newStatus) {
      ciStatus.value = ''
      await nextTick()
      ciStatus.value = newStatus
    }

    const delay = ['in_progress', 'queued'].includes(newStatus) ? 10000 : 30000
    pollTimeout = setTimeout(pollCiStatus, delay)

  } catch (e) {
    console.warn('Ошибка при обновлении CI/CD:', e.response?.data || e.message)
    workflowUrl.value = null
    ciStatus.value = null
    pollTimeout = setTimeout(pollCiStatus, 30000)
  }
}

const getCiStatusClass = (status) => {
  return {
    success: 'status-success',
    failed: 'status-failed',
    running: 'status-running',
    generated: 'status-neutral',
    not_tracked: 'status-unknown'
  }[status] || 'status-unknown'
}

const onRootDrop = async (e) => {
  isRootDragOver.value = false

  const raw = e.dataTransfer.getData('application/json')
  if (!raw) {
    addToast('Не удалось получить данные элемента', 'error')
    return
  }

  let sourceNode
  try {
    sourceNode = JSON.parse(raw)
  } catch {
    addToast('Невалидные данные элемента', 'error')
    return
  }

  const sourcePath = sourceNode.fullPath
  const name = sourcePath.split('/').pop()
  const targetFolder = app.value.path
  const newPath = `${targetFolder}/${name}`.replace(/\/+/g, '/')

  if (sourcePath === newPath) {
    addToast(`Элемент уже находится в папке "${targetFolder}"`, 'info')
    return
  }

  const currentParent = sourcePath.split('/').slice(0, -1).join('/')
  if (currentParent === targetFolder) {
    addToast(`Элемент уже находится в папке "${targetFolder}"`, 'info')
    return
  }

  try {
    await api.put(`/git/writer/${repo.value.name}/branch/${app.value.branch}/rename`, {
      oldPath: sourcePath,
      newPath,
      commitMessage: `Перемещение ${name} в ${targetFolder}`
    })

    addToast(`Перемещено в папку: ${targetFolder}`, 'success')
    await refreshTree()
  } catch (err) {
    addToast(err.response?.data?.message || 'Ошибка при перемещении', 'error')
  }
}

const searchInFiles = async () => {
  searchResults.value = []

  const allEntries = []
  const collectFiles = (node) => {
    if (node.type === 'file') allEntries.push(node)
    if (node.children) node.children.forEach(collectFiles)
  }
  collectFiles(rootEntry.value)

  const matches = []

  for (const node of allEntries) {
    try {
      const res = await api.get(`/git/writer/${repo.value.name}/branch/${app.value.branch}/file`, {
        params: { path: node.fullPath }
      })

      const lines = res.data.split('\n')
      lines.forEach((line, index) => {
        if (line.toLowerCase().includes(searchQuery.value.toLowerCase())) {
          matches.push({
            file: node.fullPath,
            line: index + 1,
            content: line.trim()
          })
        }
      })
    } catch {
    }
  }

  searchResults.value = matches
}

const goToSearchResult = async (result) => {
  await loadEntry(result.file)
  addToast(`Открыт ${result.file} (строка ${result.line})`, 'info')
}


const filterTree = (entry, query) => {
  if (!entry.children || entry.children.length === 0) {
    return entry.name.toLowerCase().includes(query) ? entry : null
  }

  const filteredChildren = entry.children
      .map(child => filterTree(child, query))
      .filter(Boolean)

  if (filteredChildren.length > 0 || entry.name.toLowerCase().includes(query)) {
    return {
      ...entry,
      children: filteredChildren
    }
  }

  return null
}

const filteredRoot = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return rootEntry.value
  return filterTree(rootEntry.value, query)
})

const formatDate = (raw) => {
  if (!raw) return '—'
  const normalized = raw.replace('MSK', '+03:00')
  const date = new Date(normalized)
  return isNaN(date.getTime()) ? '—' : date.toLocaleString('ru-RU')
}

const onEditorChange = (val) => {
  currentFileContent.value = val
}

const filteredCommitsByDate = computed(() => {
  if (!revertDateFilter.value) return commits.value

  const selected = new Date(revertDateFilter.value)
  return commits.value.filter(c => {
    const commitDate = new Date(c.date.replace('MSK', '+03:00'))
    return (
        commitDate.getFullYear() === selected.getFullYear() &&
        commitDate.getMonth() === selected.getMonth() &&
        commitDate.getDate() === selected.getDate()
    )
  })
})

const confirmRevert = async (commit) => {
  const confirmed = confirm(`Откатить к коммиту: "${commit.message}"?`)
  if (!confirmed) return

  try {
    await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/revert`, {
      commitHash: commit.hash,
      commitMessage: `Revert to: ${commit.message}`
    })
    addToast('Откат выполнен', 'success')
    showRevertModal.value = false
    await refreshTree()
    await loadCommits(currentFileName.value)
  } catch (e) {
    addToast('Ошибка при откате', 'error')
  }
}

const totalPages = computed(() =>
    Math.ceil(filteredCommitsByDate.value.length / commitsPerPage)
)

const paginatedCommits = computed(() => {
  const start = (currentPage.value - 1) * commitsPerPage
  return filteredCommitsByDate.value.slice(start, start + commitsPerPage)
})

const nextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value++
}

const prevPage = () => {
  if (currentPage.value > 1) currentPage.value--
}

</script>
