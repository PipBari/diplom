<template>
  <div
      class="file-node"
      :style="{ paddingLeft: `${depth * 16}px` }"
      @contextmenu.prevent="onRightClick"
  >
    <div class="node-label" @click="toggle">
      <span class="arrow" v-if="node.type === 'folder'">
        {{ isOpen ? '▼' : '▶' }}
      </span>
      <span class="icon">
        <template v-if="node.type === 'folder'">📁</template>
        <template v-else-if="node.name.endsWith('.tf')">
          <img src="@/assets/icons/terraform.svg" class="file-icon" alt="Terraform" />
        </template>
        <template v-else-if="node.name.endsWith('.yml') || node.name.endsWith('.yaml')">
          <img src="@/assets/icons/ansible.svg" class="file-icon" alt="Ansible" />
        </template>
        <template v-else>📄</template>
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

const emit = defineEmits(['open-file', 'context-menu'])

const isOpen = ref(false)

const toggle = () => {
  if (props.node.type === 'folder') {
    isOpen.value = !isOpen.value
  } else {
    emit('open-file', props.fullPath || props.node.fullPath)
  }
}

const onRightClick = (event) => {
  emit('context-menu', event, props.node)
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
</style>
