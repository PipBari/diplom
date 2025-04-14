<template>
  <div class="layout">
    <Sidebar />
    <main class="content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import Sidebar from '../components/Sidebar.vue'
</script>

<style scoped>
.layout {
  display: flex;
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.content {
  flex-grow: 1;
  padding: 2rem 3rem;
  overflow-y: auto;
  background-color: #fefefe;
}
</style>
