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
      {{ node.name }}
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
import {ref} from 'vue'

const props = defineProps({
  node: Object,
  fullPath: String,
  depth: {type: Number, default: 0}
})

const emit = defineEmits(['open-file', 'context-menu'])

// Локальное реактивное состояние
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
