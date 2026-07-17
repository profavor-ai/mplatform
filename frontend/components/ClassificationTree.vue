<template>
  <div class="schema-tree-wrapper">
    <div v-if="!treeNodes || treeNodes.length === 0" style="padding: 2rem; text-align: center; color: #666;">
      {{ emptyMessage }}
    </div>
    <div v-else class="va-tree" style="width: 100%;">
      <SchemaTreeNode 
        v-for="domain in treeNodes" 
        :key="domain.id" 
        :node="domain" 
        :selectedNode="selectedNode" 
        :showEdit="showEdit"
        @select="onNodeSelected" 
        @edit="handleNodeEdit" 
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useCookie } from '#app'

const props = defineProps({
  selectedNode: {
    type: Object,
    default: null
  },
  showEdit: {
    type: Boolean,
    default: false
  },
  emptyMessage: {
    type: String,
    default: '분류체계 트리가 없습니다.'
  }
})

const emit = defineEmits(['select', 'edit', 'loaded'])

const token = useCookie('auth_token')
const currentLocale = useCookie('locale', { default: () => 'ko' })
const treeNodes = ref([])

const parseName = (nameObj) => {
  if (!nameObj) return { ko: 'Unknown' };
  if (typeof nameObj === 'object') return nameObj;
  try {
    const parsed = JSON.parse(nameObj);
    if (typeof parsed === 'object' && parsed !== null) {
      return parsed;
    }
    return { ko: nameObj };
  } catch (e) {
    return { ko: nameObj };
  }
}

const loadTree = async () => {
  if (!token.value) return;
  try {
    const domains = await $fetch('/api/domains', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    
    const builtTree = []
    for (const d of domains) {
      const nodes = await $fetch(`/api/domains/${d.id}/nodes/tree`, {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      
      const formatNode = (n) => {
        const pName = parseName(n.name);
        return {
          id: n.id,
          label: pName?.[currentLocale.value] || pName?.ko || pName?.en || 'Unknown',
          domainId: d.id,
          isDomain: false,
          icon: n.icon || null,
          children: n.children ? n.children.map(formatNode) : [],
          originalNameMap: pName,
          originalData: n
        };
      };
      
      const dName = parseName(d.name);
      builtTree.push({
        id: d.id,
        label: (dName?.[currentLocale.value] || dName?.ko || dName?.en || 'Unknown') + ' (Domain)',
        domainId: d.id,
        isDomain: true,
        icon: d.icon || null,
        expanded: true,
        children: nodes.map(formatNode),
        originalNameMap: dName,
        originalData: d
      })
    }
    treeNodes.value = builtTree
    emit('loaded', builtTree)
  } catch (error) {
    console.error('Failed to load tree:', error.message || error)
  }
}

watch(currentLocale, () => {
  const updateLabel = (nodes) => {
    nodes.forEach(n => {
      if (n.originalNameMap) {
        n.label = n.originalNameMap[currentLocale.value] || n.originalNameMap.ko || n.originalNameMap.en || 'Unknown';
        if (n.isDomain) n.label += ' (Domain)';
      }
      if (n.children && n.children.length > 0) {
        updateLabel(n.children);
      }
    })
  }
  updateLabel(treeNodes.value)
})

const onNodeSelected = (node) => {
  emit('select', node)
}

const handleNodeEdit = (node) => {
  emit('edit', node)
}

onMounted(() => {
  loadTree()
})

defineExpose({
  loadTree
})
</script>

<style scoped>
.schema-tree-wrapper {
  flex: 1;
  overflow-y: auto;
}
</style>
