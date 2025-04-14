<template>
  <div class="settings-view">
    <h2>Настройки</h2>

    <div class="card-list">
      <button @click="goToGit">Git Репозитории</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToGit = () => router.push('/settings/git')
// const goToCI = () => router.push('/settings/ci')
</script>

<style scoped>
.settings-view {
  max-width: 600px;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}
button {
  padding: 10px 16px;
  font-size: 1rem;
  cursor: pointer;
  border: none;
  border-radius: 6px;
  background-color: #3f7cff;
  color: white;
  transition: 0.2s;
}
button:hover {
  background-color: #2f60d1;
}
</style>