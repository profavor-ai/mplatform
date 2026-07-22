<template>
  <div style="margin-left: 1.25rem; border-left: 2px dashed var(--va-background-border); padding-left: 1rem; margin-top: 0.75rem; position: relative;">
    <!-- Department Node Header -->
    <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.65rem 1rem; border-radius: 8px; background: var(--va-background-secondary); border: 1px solid var(--va-background-border); transition: all 0.2s ease;">
      <div style="display: flex; align-items: center; gap: 0.5rem; flex: 1;">
        <va-icon
          v-if="hasChildren"
          :name="isOpen ? 'expand_more' : 'chevron_right'"
          style="cursor: pointer;"
          @click="isOpen = !isOpen"
        />
        <va-icon v-else name="folder" color="primary" size="small" />

        <span style="font-weight: 700; font-size: 0.95rem; color: var(--va-text-primary);">
          {{ node.name }}
        </span>
        <va-chip size="small" color="info" outline>{{ getLabel('dept', '부서') }}</va-chip>
        <span v-if="node.description" style="font-size: 0.8rem; color: var(--va-text-secondary); margin-left: 0.5rem;">
          {{ node.description }}
        </span>
      </div>

      <!-- Actions -->
      <div style="display: flex; gap: 0.4rem; align-items: center;">
        <va-button size="small" preset="secondary" icon="add" @click="$emit('add-subdept', node.id)">
          + {{ getLabel('add_subdept', '하위 부서 추가') }}
        </va-button>
        <va-button size="small" preset="secondary" color="warning" icon="edit" @click="$emit('edit-dept', node)">
          {{ getLabel('edit', '수정') }}
        </va-button>
        <va-button size="small" preset="secondary" color="danger" icon="delete" @click="$emit('delete-dept', node)">
          {{ getLabel('delete', '삭제') }}
        </va-button>
      </div>
    </div>

    <!-- Children Nodes -->
    <div v-if="isOpen" style="display: flex; flex-direction: column;">
      <!-- Recursive Sub-Departments -->
      <OrgTreeItem
        v-for="subDept in (node.subDepts || [])"
        :key="subDept.id"
        :node="subDept"
        @add-subdept="$emit('add-subdept', $event)"
        @add-team="$emit('add-team', $event)"
        @edit-dept="$emit('edit-dept', $event)"
        @delete-dept="$emit('delete-dept', $event)"
      />

      <!-- Teams under this Dept -->
      <div
        v-for="team in (node.teams || [])"
        :key="team.id"
        style="margin-left: 1.25rem; border-left: 2px dashed var(--va-background-border); padding-left: 1rem; margin-top: 0.5rem;"
      >
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.5rem 0.85rem; border-radius: 6px; background: var(--va-background-element); border: 1px solid var(--va-background-border);">
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <va-icon name="groups" color="secondary" size="small" />
            <span style="font-size: 0.88rem; font-weight: 600; color: var(--va-text-primary);">
              {{ team.name }}
            </span>
            <va-chip size="small" color="secondary" outline>{{ getLabel('team', '팀') }}</va-chip>
            <span v-if="team.description" style="font-size: 0.78rem; color: var(--va-text-secondary); margin-left: 0.5rem;">
              {{ team.description }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const { t } = useI18n()

const props = defineProps({
  node: { type: Object, required: true }
})

defineEmits(['add-subdept', 'add-team', 'edit-dept', 'delete-dept'])

const isOpen = ref(true)

const getLabel = (key, fallback) => {
  const res = t(key)
  return (!res || res === key) ? fallback : res
}

const hasChildren = computed(() => {
  return (props.node.subDepts && props.node.subDepts.length > 0) || (props.node.teams && props.node.teams.length > 0)
})
</script>
