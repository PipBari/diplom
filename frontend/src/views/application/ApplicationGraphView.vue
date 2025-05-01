<template>
  <VueFlow
      :nodes="nodes"
      :edges="edges"
      fit-view
      class="graph"
      :default-viewport="{ zoom: 1.0 }"
      :node-types="customNodes"
  />
</template>

<script setup>
import { VueFlow } from '@vue-flow/core'
import { ref, onMounted } from 'vue'
import ApplicationNode from '@/components/ApplicationNode.vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()
const nodes = ref([])
const edges = ref([])

const customNodes = {
  appNode: ApplicationNode
}

onMounted(async () => {
  const res = await api.get('/applications')
  const app = res.data.find(a => a.name === route.params.name)

  const repoRes = await api.get('/settings/git')
  const repo = repoRes.data.find(r => r.name === app.repoName)

  const filesRes = await api.get(`/git/writer/${repo.name}/branch/${app.branch}/tree`, {
    params: { path: app.path }
  })

  const centerX = 400
  let y = 0

  nodes.value.push({
    id: 'app',
    type: 'appNode',
    data: { label: app.name, status: app.status },
    position: { x: centerX, y }
  })

  y += 150

  filesRes.data.forEach((file, index) => {
    const id = `file-${index}`
    nodes.value.push({
      id,
      type: 'appNode',
      data: { label: file.name, status: 'Healthy' },
      position: { x: centerX + (index - 1) * 200, y }
    })

    edges.value.push({
      id: `e-${id}`,
      source: 'app',
      target: id,
      style: { stroke: '#bbb' }
    })
  })
})
</script>

<style scoped>
.graph {
  width: 100%;
  height: calc(100vh - 60px);
  background: #f8fafc;
}
</style>
