<template>
  <!-- No children: Render as a single link item -->
  <va-sidebar-item v-if="!menu.children || menu.children.length === 0" 
                   :active="$route.path === menu.path" 
                   :to="menu.path"
                   class="sidebar-item"
                   :style="{ paddingLeft: `${depth * 1.5 + 1}rem !important` }">
    <va-sidebar-item-content>
      <va-icon :name="menu.icon || 'circle'" :size="depth > 0 ? 'small' : 'medium'" />
      <va-sidebar-item-title style="margin-left: 0.5rem;">
        {{ getMenuTitle(menu) }}
      </va-sidebar-item-title>
    </va-sidebar-item-content>
  </va-sidebar-item>

  <!-- Has children: Render as accordion/collapse -->
  <va-accordion v-else>
    <va-collapse v-model="isExpanded">
      <template #header>
        <va-sidebar-item>
          <va-sidebar-item-content style="display: flex; justify-content: space-between; align-items: center; width: 100%;"
                                   :style="{ paddingLeft: `${depth * 1.5 + 1}rem !important` }">
            <div style="display: flex; align-items: center;">
              <va-icon :name="menu.icon || 'folder'" :size="depth > 0 ? 'small' : 'medium'" />
              <va-sidebar-item-title style="margin-left: 0.5rem;">
                {{ getMenuTitle(menu) }}
              </va-sidebar-item-title>
            </div>
            <va-icon :name="isExpanded ? 'expand_less' : 'expand_more'" />
          </va-sidebar-item-content>
        </va-sidebar-item>
      </template>

      <!-- Recursive call for children -->
      <SidebarMenuItem v-for="child in menu.children" :key="child.id" :menu="child" :depth="depth + 1" />
    </va-collapse>
  </va-accordion>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const { t, te, locale } = useI18n()

const props = defineProps({
  menu: {
    type: Object,
    required: true
  },
  depth: {
    type: Number,
    default: 0
  }
})

const router = useRouter()
const route = useRoute()
const isExpanded = ref(false)

const getMenuTitle = (menu) => {
  if (!menu || !menu.name) return ''
  // 1. If DB menu.name is a JSON object or JSON stringified multilingual object (e.g. {"ko":"...", "en":"..."})
  try {
    const parsed = typeof menu.name === 'object' ? menu.name : JSON.parse(menu.name)
    if (parsed && typeof parsed === 'object') {
      const loc = (locale?.value || 'ko').toLowerCase()
      return loc.startsWith('en') ? (parsed.en || parsed.ko || '') : (parsed.ko || parsed.en || '')
    }
  } catch (e) {}

  // 2. Return DB menu name directly without i18n override
  return String(menu.name)
}

const checkActiveChild = () => {
  if (props.menu.children && props.menu.children.length > 0) {
    const hasActiveChild = props.menu.children.some(child => route.path.startsWith(child.path) && child.path !== '/')
    if (hasActiveChild) {
      isExpanded.value = true
    }
  }
}

onMounted(() => {
  checkActiveChild()
})

watch(() => route.path, () => {
  checkActiveChild()
})
</script>

<style scoped>
/* Scoped styles if needed */
</style>
