<template>
  <aside class="sidebar">
    <h2>CONFIGS EDITOR</h2>

    <router-link
        to="/applications"
        :class="{ active: isActive('/applications') }"
    >📂 Applications</router-link>

    <router-link
        to="/settings"
        :class="{ active: isActive('/settings') }"
    >⚙️ Git Settings</router-link>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const isActive = (target) => route.path.startsWith(target)
</script>

<style scoped>
.sidebar {
  width: 220px;
  background-color: #1a1a1a;
  padding: 2rem 1rem;
  height: 100vh;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.sidebar h2 {
  font-size: 1.1rem;
  margin-bottom: 2rem;
  text-align: center;
  color: #f5f5f5;
}

.sidebar a {
  display: block;
  color: #ccc;
  padding: 0.5rem 1rem;
  margin-bottom: 0.5rem;
  text-decoration: none;
  border-radius: 6px;
  font-size: 0.95rem;
  transition: 0.2s;
}

.sidebar a:hover,
.sidebar a.active {
  background-color: #3f7cff;
  color: #fff;
}
</style>
