<template>
  <div ref="editorContainer" class="monaco-container"></div>
</template>

<script setup>
import * as monaco from 'monaco-editor'
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  value: String,
  language: {
    type: String,
    default: 'plaintext'
  },
  readonly: Boolean
})

const emits = defineEmits(['update:value'])

const editorContainer = ref(null)
let editorInstance = null

onMounted(() => {
  editorInstance = monaco.editor.create(editorContainer.value, {
    value: props.value || '',
    language: props.language,
    readOnly: props.readonly || false,
    theme: 'vs-light',
    automaticLayout: true,
    minimap: { enabled: false },
    wordWrap: 'on',
    scrollBeyondLastColumn: 0,
    wrappingStrategy: 'simple',
    lineNumbers: 'on'
  })

  editorInstance.onDidChangeModelContent(() => {
    emits('update:value', editorInstance.getValue())
  })
})

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose()
  }
})

watch(() => props.value, (newValue) => {
  if (editorInstance && editorInstance.getValue() !== newValue) {
    editorInstance.setValue(newValue || '')
  }
})
</script>

<style>
.monaco-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
