<template>
  <div class="editor-layout">
    <div class="file-tree" @contextmenu.self.prevent="showContextMenu($event, null)">
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
        <div
            class="commit-main-wrapper"
            @mouseenter="showCommitHistory = true"
            @mouseleave="showCommitHistory = false"
        >
          💬 {{ commits[0].message }} — {{ commits[0].author }}, {{ formatDate(commits[0].date) }}
          <div class="commit-popup" v-if="showCommitHistory">
            <ul>
              <li v-for="c in commits" :key="c.hash">
                <div class="commit-item">
                  <div class="commit-text">
                    <b>{{ formatDate(c.date) }}</b> — {{ c.author }}: {{ c.message }}
                  </div>
                  <button class="revert-button" @click.stop="revertCommit(c)">↩️</button>
                </div>
              </li>
            </ul>
          </div>
        </div>
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
          v-if="contextMenu.node"
          class="context-item"
          @click="openRenameDialog"
      >
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

    <div v-if="showNewFolderDialog" class="overlay" @click.self="showNewFolderDialog = false">
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
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import api from '@/api/axios'
import FileTreeNode from '@/components/FileTreeNode.vue'
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
const showRenameDialog = ref(false)

const newFileName = ref('')
const newFolderName = ref('')
const renameNewName = ref('')

const validationError = ref(null)
const validationStatus = ref(null)
const validationTip = ref(null)

const contextMenu = ref({visible: false, x: 0, y: 0, node: null})

const showCommitHistory = ref(false)

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
    params: {path: app.value.path}
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
      params: {path}
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
      params: {path, limit: 5}
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

const revertCommit = async (commit) => {
  const confirmed = confirm(`Вы уверены, что хотите отменить коммит: "${commit.message}"?`)
  if (!confirmed) return

  try {
    await api.post(`/git/writer/${repo.value.name}/branch/${app.value.branch}/revert`, {
      commitHash: commit.hash,
      commitMessage: `Revert: ${commit.message}`
    })
    alert('Коммит успешно отменён')
    await refreshTree()
    await loadCommits(currentFileName.value)
  } catch (e) {
    alert('Ошибка при откате коммита')
  }
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
  } catch (e) {
    validationError.value = 'Ошибка при переименовании'
  }
}

const showContextMenu = (event, node) => {
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