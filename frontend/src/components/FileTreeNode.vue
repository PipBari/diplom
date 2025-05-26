<template>
  <div
      class="file-node"
      :style="{ paddingLeft: `${depth * 16}px` }"
      @contextmenu.prevent="onRightClick"
      :class="{ 'drag-over': isDragOver }"
  >
    <div class="node-label" :draggable="true" @dragstart="handleDragStart" @dragover.prevent="handleDragOver" @dragleave="handleDragLeave" @drop="handleDrop" @dragend="handleDragEnd" @click="toggle">
      <span class="arrow" v-if="node.type === 'folder'">
        {{ isOpen ? '▼' : '▶' }}
      </span>
      <span class="icon">
        <template v-if="node.type === 'folder'">
          <img src="@/assets/icons/field.svg" alt="folder" class="icon" />
        </template>
        <template v-else-if="node.name.endsWith('.tf')">
          <img src="@/assets/icons/terraform.svg" class="file-icon" alt="Terraform" />
        </template>
        <template v-else-if="node.name.endsWith('.yml') || node.name.endsWith('.yaml')">
          <img src="@/assets/icons/ansible.svg" class="file-icon" alt="Ansible" />
        </template>
        <template v-else-if="node.name.endsWith('.sh')">
          <img src="@/assets/icons/bash.svg" class="file-icon" alt="Bash" />
        </template>
        <template v-else>
          <img src="@/assets/icons/file.svg" alt="file" class="icon" />
        </template>
      </span>
      <span class="file-name">{{ node.name }}</span>
    </div>

    <div v-if="node.type === 'folder' && isOpen" class="children">
      <FileTreeNode
          v-for="child in node.children || []"
          :key="child.fullPath"
          :node="child"
          :fullPath="child.fullPath"
          :depth="depth + 1"
          @open-file="$emit('open-file', $event)"
          @context-menu="$emit('context-menu', $event, child)"
          @move-node="$emit('move-node', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  node: Object,
  fullPath: String,
  depth: { type: Number, default: 0 }
})

const emit = defineEmits(['open-file', 'context-menu', 'move-node'])

const isOpen = ref(false)
const isDragOver = ref(false)

const toggle = () => {
  if (props.node.type === 'folder') {
    isOpen.value = !isOpen.value
  } else {
    emit('open-file', props.fullPath || props.node.fullPath)
  }
}

const onRightClick = (event) => {
  event.stopPropagation()
  emit('context-menu', event, props.node)
}

const handleDragStart = (e) => {
  e.dataTransfer.setData('application/json', JSON.stringify(props.node))
}

const handleDragOver = () => {
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = (e) => {
  isDragOver.value = false

  let sourceNode
  try {
    sourceNode = JSON.parse(e.dataTransfer.getData('application/json'))
  } catch {
    return
  }

  const targetNode = props.node

  if (targetNode.type !== 'folder') return
  if (sourceNode.fullPath === targetNode.fullPath) return
  if (sourceNode.fullPath.startsWith(`${targetNode.fullPath}/`)) return

  emit('move-node', {
    source: sourceNode,
    targetFolder: targetNode
  })
}

const handleDragEnd = () => {
  isDragOver.value = false
}
</script>

<style scoped>
.file-node {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.node-label {
  display: flex;
  align-items: center;
  padding: 2px 6px;
  user-select: none;
  transition: background 0.2s;
}

.node-label:hover {
  background: #e0e0e0;
}

.arrow {
  width: 16px;
  text-align: center;
  margin-right: 4px;
  font-size: 12px;
  color: #888;
}

.icon {
  width: 16px;
  margin-right: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.children {
  margin-left: 12px;
  border-left: 1px solid #ddd;
  padding-left: 4px;
}

.drag-over > .node-label {
  background: #d0ebff;
}
</style>
