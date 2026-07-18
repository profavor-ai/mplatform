<template>
  <div class="menus-admin-container">
    <va-card>
      <va-card-title>
        <div class="d-flex justify-space-between align-center w-100">
          <h2>{{ $t('menu_management') || 'Menu Management' }}</h2>
          <va-button icon="add" @click="openAddModal(null)">Add Root Menu</va-button>
        </div>
      </va-card-title>
      <va-card-content>
        <div class="d-flex" style="gap: 2rem;">
          <!-- Menu Tree -->
          <div style="flex: 1; border-right: 1px solid var(--va-background-border); padding-right: 1rem;">
            <va-tree-view :nodes="treeNodes" @selected="onNodeSelected" expandable>
              <template #content="node">
                <div class="d-flex justify-space-between align-center w-100" style="padding: 0.25rem 0; cursor: pointer;" @click="onNodeSelected(node)">
                  <div>
                    <span>{{ node.label }}</span>
                  </div>
                  <div>
                    <va-button preset="plain" icon="add" size="small" @click.stop="openAddModal(node.id)" />
                    <va-button preset="plain" icon="delete" color="danger" size="small" @click.stop="deleteMenu(node.id)" />
                  </div>
                </div>
              </template>
            </va-tree-view>
          </div>

          <!-- Edit Form -->
          <div style="flex: 2;">
            <div v-if="selectedMenu" class="edit-form">
              <h3>Edit Menu</h3>
              <va-input v-model="selectedMenu.name" label="Name" class="mb-4 w-100" />
              <va-input v-model="selectedMenu.path" label="Path" class="mb-4 w-100" />
              
              <div class="mb-4">
                <label style="font-size: 0.8rem; color: var(--va-text-primary); margin-bottom: 0.5rem; display: block;">Menu Icon</label>
                <div class="d-flex align-center" style="gap: 1rem;">
                  <va-icon :name="selectedMenu.icon || 'help_outline'" size="large" color="primary" />
                  <va-button preset="secondary" border-color="primary" @click="openIconPicker('edit')">Select Icon</va-button>
                </div>
              </div>
              
              <va-input v-model="selectedMenu.sortOrder" label="Sort Order" type="number" class="mb-4 w-100" />
              <va-select v-model="selectedMenuRoles" :options="['USER', 'MANAGER', 'ADMIN']" label="Required Roles (Multiple)" class="mb-4 w-100" multiple clearable />
              
              <div class="d-flex justify-end mt-4">
                <va-button @click="saveMenu">Save Changes</va-button>
              </div>
            </div>
            <div v-else class="text-center mt-5" style="color: var(--va-secondary);">
              Select a menu from the tree to edit.
            </div>
          </div>
        </div>
      </va-card-content>
    </va-card>

    <!-- Add Modal -->
    <va-modal v-model="showAddModal" title="Add Menu" ok-text="Add" @ok="addMenu">
      <va-input v-model="newMenu.name" label="Name" class="mb-4 w-100" />
      <va-input v-model="newMenu.path" label="Path" class="mb-4 w-100" />
      
      <div class="mb-4">
        <label style="font-size: 0.8rem; color: var(--va-text-primary); margin-bottom: 0.5rem; display: block;">Menu Icon</label>
        <div class="d-flex align-center" style="gap: 1rem;">
          <va-icon :name="newMenu.icon || 'help_outline'" size="large" color="primary" />
          <va-button preset="secondary" border-color="primary" @click="openIconPicker('add')">Select Icon</va-button>
        </div>
      </div>
      
      <va-input v-model="newMenu.sortOrder" label="Sort Order" type="number" class="mb-4 w-100" />
      <va-select v-model="newMenuRoles" :options="['USER', 'MANAGER', 'ADMIN']" label="Required Roles (Multiple)" class="mb-4 w-100" multiple clearable />
    </va-modal>

    <!-- Icon Picker Modal -->
    <va-modal v-model="showIconPickerModal" title="Select Icon" hide-default-actions>
      <IconPicker v-model="tempIcon" />
      <div class="d-flex justify-end mt-4" style="gap: 1rem;">
        <va-button preset="plain" color="secondary" @click="showIconPickerModal = false">Cancel</va-button>
        <va-button @click="confirmIconSelection">Select</va-button>
      </div>
    </va-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMenu } from '~/composables/useMenu'
import { useToast } from 'vuestic-ui'

const { menus, fetchMenus } = useMenu()
const { init } = useToast()

const token = useCookie('auth_token')
const selectedMenu = ref(null)
const showAddModal = ref(false)
const showIconPickerModal = ref(false)
const iconPickerTarget = ref('')
const tempIcon = ref('')
const newMenu = ref({ name: '', path: '', icon: '', sortOrder: 0, requiredRole: '', parentId: null })

const selectedMenuRoles = computed({
  get: () => selectedMenu.value && selectedMenu.value.requiredRole ? selectedMenu.value.requiredRole.split(',') : [],
  set: (val) => {
    if (selectedMenu.value) selectedMenu.value.requiredRole = val.join(',')
  }
})

const newMenuRoles = computed({
  get: () => newMenu.value.requiredRole ? newMenu.value.requiredRole.split(',') : [],
  set: (val) => {
    newMenu.value.requiredRole = val.join(',')
  }
})

const openIconPicker = (target) => {
  iconPickerTarget.value = target
  tempIcon.value = target === 'edit' ? (selectedMenu.value?.icon || '') : (newMenu.value?.icon || '')
  showIconPickerModal.value = true
}

const confirmIconSelection = () => {
  if (iconPickerTarget.value === 'edit' && selectedMenu.value) {
    selectedMenu.value.icon = tempIcon.value
  } else if (iconPickerTarget.value === 'add') {
    newMenu.value.icon = tempIcon.value
  }
  showIconPickerModal.value = false
}

// Convert to va-tree-view format
const formatToTreeNodes = (items) => {
  return items.map(item => ({
    id: item.id,
    label: item.name,
    icon: item.icon,
    children: item.children ? formatToTreeNodes(item.children) : [],
    raw: item
  }))
}

const treeNodes = computed(() => {
  if (!menus.value) return []
  return formatToTreeNodes(menus.value)
})

const onNodeSelected = (node) => {
  if (node && node.raw) {
    selectedMenu.value = JSON.parse(JSON.stringify(node.raw))
  }
}

const openAddModal = (parentId) => {
  newMenu.value = { name: '', path: '', icon: '', sortOrder: 0, requiredRole: '', parentId }
  showAddModal.value = true
}

const addMenu = async () => {
  try {
    await $fetch('/api/menus', {
      method: 'POST',
      headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
      body: newMenu.value
    })
    init({ message: 'Menu added successfully', color: 'success' })
    await fetchMenus()
  } catch (error) {
    init({ message: 'Failed to add menu', color: 'danger' })
  }
}

const saveMenu = async () => {
  if (!selectedMenu.value) return
  try {
    await $fetch(`/api/menus/${selectedMenu.value.id}`, {
      method: 'PUT',
      headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
      body: selectedMenu.value
    })
    init({ message: 'Menu updated successfully', color: 'success' })
    await fetchMenus()
  } catch (error) {
    init({ message: 'Failed to update menu', color: 'danger' })
  }
}

const deleteMenu = async (id) => {
  if (!confirm('Are you sure you want to delete this menu?')) return
  try {
    await $fetch(`/api/menus/${id}`, {
      method: 'DELETE',
      headers: token.value ? { Authorization: `Bearer ${token.value}` } : {}
    })
    init({ message: 'Menu deleted successfully', color: 'success' })
    if (selectedMenu.value && selectedMenu.value.id === id) {
      selectedMenu.value = null
    }
    await fetchMenus()
  } catch (error) {
    init({ message: 'Failed to delete menu', color: 'danger' })
  }
}

onMounted(async () => {
  await fetchMenus()
})
</script>

<style scoped>
.menus-admin-container {
  padding: 20px;
}
</style>
