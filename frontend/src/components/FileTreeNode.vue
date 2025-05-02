<template>
  <div class="file-node">
    <div class="node-label" @click="toggle">
      {{ node.type === 'folder' ? (node.expanded ? '📂' : '📁') : '📄' }}
      {{ node.name }}
    </div>
    <div v-if="node.type === 'folder' && node.expanded" class="children">
      <FileTreeNode
          v-for="child in node.children || []"
          :key="child.fullPath"
          :node="child"
          :fullPath="child.fullPath"
          @open-file="$emit('open-file', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  node: Object,
  fullPath: String
})

const emit = defineEmits(['open-file'])

const toggle = () => {
  if (props.node.type === 'folder') {
    props.node.expanded = !props.node.expanded
  } else {
    emit('open-file', props.fullPath)
  }
}
</script>

<style scoped>
.file-node {
  margin-left: 0.5rem;
}
.node-label {
  cursor: pointer;
  user-select: none;
}
.children {
  margin-left: 1rem;
}
</style>
