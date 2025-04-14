<template>
  <div class="settings">
    <h2>⚙️ Git Настройки</h2>
    <form @submit.prevent="save">
      <label>Repo URL:</label>
      <input v-model="form.repoUrl" required />

      <label>Branch:</label>
      <input v-model="form.branch" required />

      <label>Username:</label>
      <input v-model="form.username" required />

      <label>Token:</label>
      <input v-model="form.token" type="password" required />

      <label>Local Path:</label>
      <input v-model="form.localPath" required />

      <button type="submit">Сохранить</button>
    </form>

    <p v-if="message" class="success-msg">{{ message }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const form = ref({
  repoUrl: '',
  branch: '',
  username: '',
  token: '',
  localPath: ''
})

const message = ref('')

onMounted(async () => {
  try {
    const response = await axios.get('http://localhost:8080/settings/git')
    Object.assign(form.value, response.data)
  } catch (e) {
    console.warn('Git settings not found')
  }
})

const save = async () => {
  try {
    await axios.post('http://localhost:8080/settings/git', form.value)
    message.value = 'Настройки успешно сохранены ✅'
  } catch (e) {
    message.value = 'Ошибка при сохранении ❌'
  }
}
</script>

<style scoped>
.settings {
  max-width: 500px;
}
.settings input {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 6px;
}
button {
  padding: 6px 12px;
}
.success-msg {
  margin-top: 1rem;
  color: green;
}
</style>
