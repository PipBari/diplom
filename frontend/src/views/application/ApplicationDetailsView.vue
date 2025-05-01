<template>
  <div class="details-container">
    <div class="top-bar">
      <div class="app-info">
        <h2>{{ app.name }}</h2>
        <p><b>Sync:</b> {{ app.status }} • <b>Branch:</b> {{ app.branch }}</p>
      </div>
      <div class="actions">
        <button @click="syncApp">Sync</button>
        <button @click="refreshGraph">Refresh</button>
      </div>
    </div>

    <div class="graph-section">
      <VueFlow :nodes="nodes" :edges="edges" fit-view>
        <Background />
        <Controls />
      </VueFlow>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { VueFlow } from '@vue-flow/core'
import { Background, Controls } from '@vue-flow/additional-components'

const route = useRoute()
const app = ref({})
const nodes = ref([])
const edges = ref([])

const loadApp = async () => {
  const res = await api.get('/applications')
  app.value = res.data.find(a => a.name === route.params.name)

  if (app.value) {
    const treeRes = await api.get(`/git/writer/${app.value.repoName}/branch/${app.value.branch}/tree`, {
      params: { path: app.value.path }
    })

    const files = treeRes.data

    const baseId = app.value.name
    nodes.value = [
      {
        id: baseId,
        label: app.value.name,
        position: { x: 250, y: 0 },
        style: { padding: '12px', border: '1px solid #555', borderRadius: '4px' }
      },
      ...files.map((f, i) => ({
        id: f.name,
        label: f.name,
        position: { x: 100 + i * 200, y: 150 },
        style: { padding: '8px', border: '1px solid #555', borderRadius: '4px' }
      }))
    ]

    edges.value = files.map(f => ({
      id: `${baseId}-${f.name}`,
      source: baseId,
      target: f.name
    }))
  }
}

const syncApp = () => {
  console.log('Sync clicked')
}

const refreshGraph = () => {
  loadApp()
}

onMounted(() => {
  loadApp()
})
</script>

<style scoped>
.details-container {
  padding: 1rem;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.app-info h2 {
  margin: 0;
}

.actions button {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  margin-left: 0.5rem;
  border-radius: 4px;
  cursor: pointer;
}

.graph-section {
  height: 600px;
  background: #f9fafb;
  border: 1px solid #ddd;
  border-radius: 8px;
}
</style>
