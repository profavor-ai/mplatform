<template>
  <div class="custom-tree-node">
    <div class="node-wrapper" :class="{ 'selected-node': node.id === selectedNode?.id }" @click="onNodeSelected(node)">
      <!-- Indentation -->
      <div :style="{ width: `${level * 20}px` }"></div>
      
      <!-- Expand Icon -->
      <div class="expand-icon" @click.stop="toggleExpand" v-if="hasChildren">
        <va-icon :name="isExpanded ? 'keyboard_arrow_down' : 'keyboard_arrow_right'" size="small" />
      </div>
      <div class="expand-icon-placeholder" v-else></div>

      <!-- Label -->
      <span class="node-label" :style="{ fontWeight: node.id === selectedNode?.id ? 'bold' : 'normal', color: node.id === selectedNode?.id ? 'var(--va-primary)' : 'inherit' }">
        {{ node.label }}
      </span>

      <!-- Edit Button -->
      <va-button v-if="showEdit" class="edit-btn" icon="edit" size="small" @click.stop="handleNodeEdit(node)" preset="plain" />
    </div>

    <!-- Children -->
    <div class="children-wrapper" v-if="hasChildren" v-show="isExpanded">
      <SchemaTreeNode 
        v-for="child in node.children" 
        :key="child.id" 
        :node="child" 
        :level="level + 1"
        :selectedNode="selectedNode" 
        :showEdit="showEdit"
        @select="onNodeSelected" 
        @edit="handleNodeEdit" 
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  node: Object,
  selectedNode: Object,
  level: {
    type: Number,
    default: 0
  },
  showEdit: {
    type: Boolean,
    default: true
  }
})
const emit = defineEmits(['select', 'edit'])

const isExpanded = ref(props.node.expanded !== false)

const hasChildren = computed(() => props.node.children && props.node.children.length > 0)

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const onNodeSelected = (n) => emit('select', n)
const handleNodeEdit = (n) => emit('edit', n)
</script>

<style scoped>
.custom-tree-node {
  width: 100%;
}
.node-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.node-wrapper.selected-node {
  background-color: rgba(21, 78, 193, 0.1);
}
.node-wrapper:hover {
  background-color: rgba(128, 128, 128, 0.15);
}
.expand-icon {
  width: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
}
.expand-icon-placeholder {
  width: 24px;
}
.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 40px;
}
.node-wrapper .edit-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease, visibility 0.2s ease;
  z-index: 10;
}
.node-wrapper:hover .edit-btn {
  opacity: 1;
  visibility: visible;
}
</style>
