<template>
  <div
      class="file-node"
      :style="{ paddingLeft: `${depth * 16}px` }"
      @contextmenu.prevent="onRightClick"
  >
    <div class="node-label" @click="toggle">
      <span class="arrow" v-if="node.type === 'folder'">
        {{ node.expanded ? '▼' : '▶' }}
      </span>
      <span class="icon">
        <template v-if="node.type === 'folder'">📁</template>
        <template v-else-if="node.name.endsWith('.tf')">
          <img :src="TerraformIcon" alt="Terraform" class="file-icon" />
        </template>
        <template v-else-if="node.name.endsWith('.yml') || node.name.endsWith('.yaml')">
          <img :src="AnsibleIcon" alt="Ansible" class="file-icon" />
        </template>
        <template v-else>📄</template>
      </span>
      {{ node.name }}
    </div>

    <div v-if="node.type === 'folder' && node.expanded" class="children">
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
import TerraformIcon from '@/assets/icons/terraform.svg'
import AnsibleIcon from '@/assets/icons/ansible.svg'

const props = defineProps({
  node: Object,
  fullPath: String,
  depth: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['open-file', 'context-menu'])

const toggle = () => {
  if (props.node.type === 'folder') {
    props.node.expanded = !props.node.expanded
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
  font-size: 16px;
  line-height: 1.8;
}

.node-label {
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  transition: background-color 0.2s;
}

.node-label:hover {
  background-color: #e0e7ff;
  border-radius: 4px;
}

.arrow {
  width: 16px;
  text-align: center;
  font-size: 16px;
}

.icon {
  width: 20px;
  text-align: center;
  font-size: 16px;
}

.file-icon {
  width: 16px;
  height: 16px;
  display: inline-block;
  vertical-align: middle;
}

.children {
  margin-left: 0;
}
</style>
