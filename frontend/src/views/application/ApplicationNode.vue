<template>
  <div class="app-node" :class="statusClass">
    <div class="icon">📦</div>
    <div class="name">{{ label }}</div>
    <div class="status">{{ status }}</div>
  </div>
</template>

<script setup>
defineProps(['label', 'status'])

const statusClass = computed(() => {
  if (status === 'Healthy' || status === 'Synced') return 'ok'
  if (status === 'Error') return 'fail'
  return 'unknown'
})
</script>

<style scoped>
.app-node {
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 0 3px rgba(0, 0, 0, 0.2);
  min-width: 120px;
  text-align: center;
  font-size: 14px;
}
.icon {
  font-size: 20px;
  margin-bottom: 4px;
}
.status {
  font-size: 12px;
  margin-top: 4px;
}
.ok { border-left: 4px solid #28a745; }
.fail { border-left: 4px solid #dc3545; }
.unknown { border-left: 4px solid #6c757d; }
</style>
