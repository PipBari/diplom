<template>
  <div class="settings-view">
    <h2>Настройки</h2>
    <div class="card-list">
      <div class="card" @click="goToGit">
        <div class="card-title">Репозитории</div>
        <div class="card-description">Настройка подключенных репозиториев</div>
      </div>
      <div class="card" @click="goToServers">
        <div class="card-title">Серверы</div>
        <div class="card-description">Настройка подключенных серверов</div>
      </div>
      <div class="card" @click="goToProjects">
        <div class="card-title">Проекты</div>
        <div class="card-description">Настройка проектов</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const goToGit = () => router.push('/settings/git')
const goToProjects = () => router.push('/settings/projects')
const goToServers = () => router.push('/settings/servers')
</script>

<style scoped>
.settings-view {
  max-width: 800px;
  margin: auto;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 30px;
}
.card {
  padding: 20px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  border-left: 4px solid #3f7cff;
}
.card:hover {
  background: #f9f9f9;
}
.card-title {
  font-weight: bold;
  color: #007bff;
}
.card-description {
  color: #555;
  font-size: 0.95rem;
  margin-top: 4px;
}
</style>
