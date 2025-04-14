<template>
  <form @submit.prevent="handleSubmit" class="app-form">
    <input v-model="name" placeholder="Имя приложения" required />
    <input v-model="path" placeholder="Путь в репозитории (например: my-app/)" required />
    <button type="submit">Добавить</button>
  </form>
</template>

<script setup>
import { ref } from 'vue'

const name = ref('')
const path = ref('')

const emit = defineEmits(['submit'])

const handleSubmit = () => {
  emit('submit', { name: name.value, path: path.value })
  name.value = ''
  path.value = ''
}
</script>

<style scoped>
.app-form {
  margin-bottom: 1rem;
}
.app-form input {
  display: block;
  margin-bottom: 0.5rem;
  padding: 6px;
  width: 100%;
}
</style>
