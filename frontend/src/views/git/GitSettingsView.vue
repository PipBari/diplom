<template>
  <div class="git-settings">
    <h2>🧬 Git Репозитории</h2>

    <button class="add-btn" @click="showForm = true">➕ Добавить репозиторий</button>

    <div v-if="showForm" class="side-panel">
      <div class="side-panel-content">
        <h3>Добавить репозиторий</h3>
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
            <button type="submit">✅ Сохранить</button>
            <button type="button" @click="showForm = false">✖ Отмена</button>
          </div>
        </form>
      </div>
    </div>

    <p v-if="message" class="success-msg">{{ message }}</p>

    <ul v-if="repos.length">
      <li v-for="repo in repos" :key="repo.name">
        <strong>{{ repo.name }}</strong> — {{ repo.repoUrl }} (ветка: {{ repo.branch }}) —
        <span :class="statusClass(repo.status)">{{ repo.status }}</span>
        <button @click="removeRepo(repo.name)">🗑️</button>
      </li>
    </ul>
    <p v-else>Нет добавленных репозиториев.</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const form = ref({
  name: '',
  repoUrl: '',
  branch: '',
  username: '',
  token: ''
})

const repos = ref([])
const message = ref('')
const showForm = ref(false)

const loadRepos = async () => {
  try {
    const response = await axios.get('http://localhost:8080/settings/git')
    repos.value = response.data
  } catch (e) {
    console.error('Ошибка при загрузке репозиториев', e)
  }
}

const addRepo = async () => {
  try {
    await axios.post('http://localhost:8080/settings/git', form.value)
    message.value = '✅ Репозиторий добавлен'
    form.value = { name: '', repoUrl: '', branch: '', username: '', token: '' }
    showForm.value = false
    await loadRepos()
  } catch (e) {
    message.value = '❌ Ошибка при добавлении'
  }
}

const removeRepo = async (name) => {
  try {
    await axios.delete(`http://localhost:8080/settings/git/${name}`)
    await loadRepos()
  } catch (e) {
    console.error('Ошибка при удалении', e)
  }
}

const statusClass = (status) => {
  if (status === 'Successful') return 'status-ok'
  if (status === 'Error') return 'status-error'
  return 'status-unknown'
}

onMounted(loadRepos)
</script>

<style scoped>
.git-settings {
  position: relative;
  max-width: 800px;
  padding-right: 340px;
}

h2 {
  margin-bottom: 0.5rem;
}

.add-btn {
  background-color: #3f7cff;
  color: white;
  border: none;
  padding: 6px 12px;
  font-size: 0.95rem;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 1.5rem;
  display: inline-block;
}

.side-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 320px;
  height: 100vh;
  background-color: #fefefe;
  border-left: 1px solid #ddd;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  z-index: 1000;
}

.side-panel-content h3 {
  margin-bottom: 1rem;
}

.side-panel-content input {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 6px;
  font-size: 0.95rem;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
}

button {
  padding: 6px 10px;
  font-size: 0.9rem;
  cursor: pointer;
}

.success-msg {
  margin-top: 1rem;
  color: green;
}

ul {
  list-style: none;
  padding: 0;
  margin-top: 1rem;
}

li {
  margin: 0.5rem 0;
  padding: 0.5rem;
  background-color: #f3f3f3;
  border-radius: 4px;
}

.status-ok {
  color: green;
}
.status-error {
  color: red;
}
.status-unknown {
  color: gray;
}
</style>