<template>
  <div>
    <h2>Приложения</h2>

    <ApplicationForm @submit="addApp" />

    <AppCard
        v-for="app in applications"
        :key="app.name"
        :title="app.name"
        :status="'Synced'"
    >
      <p>Path: {{ app.path }}</p>
      <p>Created at: {{ app.createdAt }}</p>
    </AppCard>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import AppCard from '../../components/AppCard.vue'
import ApplicationForm from './ApplicationForm.vue'

const applications = ref([])

const addApp = (app) => {
  const createdAt = new Date().toLocaleString()
  applications.value.push({...app, createdAt})
}
</script>
