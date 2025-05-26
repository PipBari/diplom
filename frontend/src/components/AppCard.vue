<template>
  <div class="app-card">
    <h3>{{ title }}</h3>
    <p v-if="status">
      <img src="@/assets/icons/file.svg" alt="status icon" class="icon" />
      {{ status }}
    </p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title: String,
  status: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.app-card {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 1rem;
  margin: 1rem 0;
  background-color: #f9f9f9;
  transition: 0.3s;
}

.app-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
  vertical-align: middle;
  margin-right: 0.4em;
}
</style>
