<template>
  <div class="layout-wrapper" style="opacity: 0;" :style="{ opacity: isMounted ? 1 : 0, transition: 'opacity 0.3s ease' }">
    <va-layout>
      <template #top>
        <va-navbar :color="isDark ? '#1F2937' : 'primary'">
          <template #left>
            <div style="display: flex; align-items: center; gap: 0.5rem;">
              <va-icon name="menu" @click="showSidebar = !showSidebar" class="mobile-menu-btn" style="cursor: pointer; font-size: 28px;" />
              <va-navbar-item class="font-bold text-lg text-white title-text" style="padding: 0;">
                <span class="full-title">Domain Governance System</span>
                <span class="short-title">Domain System</span>
              </va-navbar-item>
            </div>
          </template>
          <template #right>
            <va-navbar-item class="text-white">
              <div class="navbar-right">
                <!-- Theme Toggle for Desktop -->
                <va-button preset="plain" class="mr-2 hide-mobile theme-btn" @click="toggleTheme" style="color: white !important;">
                  <va-icon :name="isDark ? 'light_mode' : 'dark_mode'" size="large" />
                </va-button>
                
                <!-- Organization Badge -->
                <va-chip size="small" color="info" style="color: white; background: rgba(255, 255, 255, 0.2); border: none;" class="mr-2 hide-mobile">
                  <va-icon name="corporate_fare" size="small" class="mr-1" />
                  <span>{{ currentOrgName || 'Default Organization' }}</span>
                </va-chip>

                <!-- User Profile Dropdown -->
                <va-dropdown placement="bottom-end" stick-to-edges>
                  <template #anchor>
                    <va-button preset="plain" class="profile-btn" style="color: white !important; padding: 0.5rem;">
                      <va-icon name="account_circle" class="mr-2" size="28px" /> 
                      <span class="username-text font-bold">{{ currentUser?.username || 'Admin' }}</span>
                      <va-icon name="expand_more" class="ml-1" />
                    </va-button>
                  </template>
                  <va-dropdown-content class="profile-dropdown-content">
                    <div class="profile-header text-center pb-2">
                      <div class="font-bold text-lg">{{ currentUser?.username || 'Admin' }}</div>
                      <div class="text-sm text-gray" style="color: var(--va-secondary)">{{ currentUser?.role || 'GUEST' }}</div>
                    </div>
                    <va-divider class="my-2" />
                    <va-list>
                      <va-list-item @click="toggleLang" class="cursor-pointer dropdown-item">
                        <va-list-item-section icon>
                          <va-icon name="language" />
                        </va-list-item-section>
                        <va-list-item-section>
                          <va-list-item-title>{{ currentLocale === 'ko' ? 'English' : '한국어' }}</va-list-item-title>
                        </va-list-item-section>
                      </va-list-item>
                      <va-list-item @click="toggleTheme" class="cursor-pointer dropdown-item show-mobile">
                        <va-list-item-section icon>
                          <va-icon :name="isDark ? 'light_mode' : 'dark_mode'" />
                        </va-list-item-section>
                        <va-list-item-section>
                          <va-list-item-title>{{ isDark ? 'Light Theme' : 'Dark Theme' }}</va-list-item-title>
                        </va-list-item-section>
                      </va-list-item>
                      <va-list-item @click="showRequestAccessModal = true" class="cursor-pointer dropdown-item">
                        <va-list-item-section icon>
                          <va-icon name="add_circle_outline" />
                        </va-list-item-section>
                        <va-list-item-section>
                          <va-list-item-title>{{ $t('request_domain_access') || 'Request Domain Access' }}</va-list-item-title>
                        </va-list-item-section>
                      </va-list-item>
                      <va-list-item @click="showSettingsModal = true" class="cursor-pointer dropdown-item">
                        <va-list-item-section icon>
                          <va-icon name="settings" />
                        </va-list-item-section>
                        <va-list-item-section>
                          <va-list-item-title>Personal Settings</va-list-item-title>
                        </va-list-item-section>
                      </va-list-item>
                      <va-list-item @click="handleLogout" class="cursor-pointer dropdown-item text-danger">
                        <va-list-item-section icon>
                          <va-icon name="logout" color="danger" />
                        </va-list-item-section>
                        <va-list-item-section>
                          <va-list-item-title style="color: var(--va-danger); font-weight: bold;">Logout</va-list-item-title>
                        </va-list-item-section>
                      </va-list-item>
                    </va-list>
                  </va-dropdown-content>
                </va-dropdown>
              </div>
            </va-navbar-item>
          </template>
        </va-navbar>
      </template>
      
      <template #left>
        <va-sidebar v-model="showSidebar" :minimized="false" class="responsive-sidebar" :class="{ 'dark-theme-sidebar': isDark }">
          <SidebarMenuItem v-for="menu in filteredMenus" :key="menu.id" :menu="menu" />
        </va-sidebar>
      </template>
      
      <template #content>
        <main class="responsive-main" :style="{ backgroundColor: isDark ? 'var(--va-background-primary)' : '#f4f6f8' }">
          <slot />
        </main>

        <!-- Personal Settings Modal -->
        <va-modal v-model="showSettingsModal" :title="$t('personal_settings') || 'Personal Settings'" hide-default-actions>
          <div style="min-width: 320px; padding: 1rem 1.5rem 1.5rem 1.5rem; overflow: hidden; box-sizing: border-box;">
            <div class="mb-4" style="display: flex; flex-direction: column; gap: 0.5rem;">
              <span style="font-size: 0.85rem; color: var(--va-text-secondary); font-weight: 600;">
                {{ $t('timezone') || 'Timezone Settings' }}
              </span>
              <va-select 
                v-model="selectedTimezone" 
                :options="timezoneOptions" 
                value-by="value"
                text-by="label"
                class="w-full"
                outline
                :placeholder="$t('timezone_select') || 'Select timezone'"
              />
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
              <va-button preset="secondary" color="secondary" @click="showSettingsModal = false">Cancel</va-button>
              <va-button :loading="isSavingTimezone" @click="handleSaveTimezone">Save</va-button>
            </div>
          </div>
        </va-modal>
        <!-- Request Access Modal -->
        <DomainAccessRequestModal v-model="showRequestAccessModal" />
      </template>
    </va-layout>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCookie, useState } from '#app'
import { useColors } from 'vuestic-ui'

const { t, locale, setLocale } = useI18n()
const { applyPreset, currentPresetName } = useColors()

const router = useRouter()
const tokenCookie = useCookie('auth_token')
const userCookie = useCookie('user_data')
const currentLocale = useCookie('locale', { default: () => 'ko' })
const savedTheme = useCookie('theme', { default: () => 'light' })

const showSettingsModal = ref(false)
const savedTimezone = useCookie('timezone', { default: () => 'Asia/Seoul' })
const selectedTimezone = ref('')
const isSavingTimezone = ref(false)

const showRequestAccessModal = ref(false)

import { useMenu } from '~/composables/useMenu'
const { menus, fetchMenus } = useMenu()

const filteredMenus = computed(() => {
  const role = currentUser.value?.role
  
  const filterTree = (nodes) => {
    return nodes.filter(node => {
      let canAccess = true
      
      if (node.requiredRole) {
        const requiredRoles = node.requiredRole.split(',').map(r => r.trim()).filter(r => r)
        if (requiredRoles.length > 0) {
          // If the user's role is not explicitly in the required roles list
          if (!requiredRoles.includes(role)) {
            // Role hierarchy logic:
            if (role === 'ADMIN') {
              // ADMIN can access anything
            } else if (role === 'MANAGER' && requiredRoles.includes('USER')) {
              // MANAGER can access USER level menus
            } else {
              canAccess = false
            }
          }
        }
      }
      
      if (!canAccess) return false
      
      if (node.children && node.children.length > 0) {
        node.children = filterTree(node.children)
      }
      return true
    })
  }
  
  if (!menus.value) return []
  const menusCopy = JSON.parse(JSON.stringify(menus.value))
  const filtered = filterTree(menusCopy)
  
  // Ensure DQ Dashboard is accessible in sidebar
  const hasDqDashboard = filtered.some(m => m.path === '/dq-dashboard' || (m.children && m.children.some(c => c.path === '/dq-dashboard')))
  if (!hasDqDashboard) {
    filtered.push({
      id: 'dq-dashboard-auto',
      name: 'DQ Dashboard',
      path: '/dq-dashboard',
      icon: 'analytics',
      requiredRole: 'USER',
      children: []
    })
  }
  return filtered
})
const timezoneOptions = ref([
  { label: '[GMT+09:00] Asia/Seoul (Seoul)', value: 'Asia/Seoul' },
  { label: '[GMT+09:00] Asia/Tokyo (Tokyo)', value: 'Asia/Tokyo' },
  { label: '[GMT+08:00] Asia/Shanghai (Shanghai)', value: 'Asia/Shanghai' },
  { label: '[GMT+08:00] Asia/Hong Kong (Hong Kong)', value: 'Asia/Hong_Kong' },
  { label: '[GMT+08:00] Asia/Singapore (Singapore)', value: 'Asia/Singapore' },
  { label: '[GMT+05:30] Asia/Kolkata (Kolkata)', value: 'Asia/Kolkata' },
  { label: '[GMT+04:00] Asia/Dubai (Dubai)', value: 'Asia/Dubai' },
  { label: '[GMT+07:00] Asia/Jakarta (Jakarta)', value: 'Asia/Jakarta' },
  { label: '[GMT+00:00] Europe/London (London)', value: 'Europe/London' },
  { label: '[GMT+01:00] Europe/Paris (Paris)', value: 'Europe/Paris' },
  { label: '[GMT+01:00] Europe/Berlin (Berlin)', value: 'Europe/Berlin' },
  { label: '[GMT+03:00] Europe/Moscow (Moscow)', value: 'Europe/Moscow' },
  { label: '[GMT-05:00] America/New York (New York)', value: 'America/New_York' },
  { label: '[GMT-06:00] America/Chicago (Chicago)', value: 'America/Chicago' },
  { label: '[GMT-07:00] America/Denver (Denver)', value: 'America/Denver' },
  { label: '[GMT-08:00] America/Los Angeles (Los Angeles)', value: 'America/Los_Angeles' },
  { label: '[GMT-09:00] America/Anchorage (Anchorage)', value: 'America/Anchorage' },
  { label: '[GMT-10:00] America/Honolulu (Honolulu)', value: 'America/Honolulu' },
  { label: '[GMT-03:00] America/Sao Paulo (Sao Paulo)', value: 'America/Sao_Paulo' },
  { label: '[GMT+10:00] Australia/Sydney (Sydney)', value: 'Australia/Sydney' },
  { label: '[GMT+12:00] Pacific/Auckland (Auckland)', value: 'Pacific/Auckland' },
  { label: '[GMT+02:00] Africa/Cairo (Cairo)', value: 'Africa/Cairo' },
  { label: '[GMT+02:00] Africa/Johannesburg (Johannesburg)', value: 'Africa/Johannesburg' },
  { label: '[GMT+00:00] UTC (Coordinated Universal Time)', value: 'UTC' }
])

watch(showSettingsModal, (isOpen) => {
  if (isOpen) {
    selectedTimezone.value = savedTimezone.value || 'Asia/Seoul'
  }
})

const handleSaveTimezone = async () => {
  isSavingTimezone.value = true
  try {
    savedTimezone.value = selectedTimezone.value
    
    if (userCookie.value) {
      const usr = typeof userCookie.value === 'string' ? JSON.parse(userCookie.value) : userCookie.value
      usr.timezone = selectedTimezone.value
      userCookie.value = JSON.stringify(usr)
    }
    
    await $fetch('/api/users/timezone', {
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenCookie.value}` },
      body: { timezone: selectedTimezone.value }
    })
    
    showSettingsModal.value = false
    window.location.reload()
  } catch (e) {
    console.error('Failed to save timezone:', e)
  } finally {
    isSavingTimezone.value = false
  }
}

const isDark = computed(() => currentPresetName.value === 'dark')

if (setLocale) setLocale(currentLocale.value || 'ko')
else locale.value = currentLocale.value || 'ko'

watch(currentLocale, (newVal) => {
  if (setLocale) setLocale(newVal || 'ko')
  else locale.value = newVal || 'ko'
})

const toggleTheme = () => {
  const newTheme = isDark.value ? 'light' : 'dark'
  applyPreset(newTheme)
  savedTheme.value = newTheme
}

const toggleLang = () => {
  const newLang = currentLocale.value === 'ko' ? 'en' : 'ko'
  currentLocale.value = newLang
  if (setLocale) setLocale(newLang)
  else locale.value = newLang
  if (typeof document !== 'undefined') {
    document.documentElement.lang = newLang === 'en' ? 'en-US' : 'ko-KR'
  }
}

watch(currentLocale, (newLang) => {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = newLang === 'en' ? 'en-US' : 'ko-KR'
  }
}, { immediate: true })
const showSidebar = ref(true)
const isMobile = ref(false)

const route = useRoute()

const isMounted = ref(false)

onMounted(async () => {
  if (savedTheme.value) {
    applyPreset(savedTheme.value)
  }
  
  await fetchMenus()

  if (window.innerWidth < 768) {
    isMobile.value = true
    showSidebar.value = false
  }
  window.addEventListener('resize', () => {
    const isNowMobile = window.innerWidth < 768
    if (isMobile.value && !isNowMobile) {
      showSidebar.value = true // Restore on PC
    }
    isMobile.value = isNowMobile
  })
  isMounted.value = true
})

watch(route, () => {
  if (isMobile.value) {
    showSidebar.value = false
  }
})

const currentUser = computed(() => {
  if (userCookie.value) {
    return typeof userCookie.value === 'string' ? JSON.parse(userCookie.value) : userCookie.value
  }
  return null
})

const handleLogout = () => {
  tokenCookie.value = null
  userCookie.value = null
  router.push('/login')
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Inter', sans-serif;
  background-color: var(--va-background-primary);
  color: var(--va-text-primary);
  height: 100vh;
  width: 100%;
  overflow: hidden; /* Prevent double scrollbars, let va-layout handle it */
}
.layout-wrapper {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
.navbar-right {
  display: flex; 
  align-items: center; 
  white-space: nowrap; 
  padding-right: 1.5rem;
}
.profile-btn {
  text-transform: none !important;
}
.profile-dropdown-content {
  min-width: 240px;
  padding: 0.5rem 0;
}
.dropdown-item {
  padding: 0.75rem 1rem;
  transition: background-color 0.2s;
}
.dropdown-item:hover {
  background-color: var(--va-background-element);
}
.hide-mobile {
  display: block;
}
.show-mobile {
  display: none !important;
}
.mobile-menu-btn {
  display: inline-flex !important;
}
.responsive-main {
  padding: 1rem;
  height: calc(100vh - 64px);
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}
.short-title { display: none; }

/* Minimalist Flat Sidebar Navigation UI */
.va-sidebar {
  border-right: 1px solid var(--va-background-border);
}
.va-sidebar-item {
  margin: 0 !important;
  padding: 0 !important;
  border-radius: 0 !important;
  transition: background-color 0.15s ease !important;
  color: #111827 !important;
  background-color: transparent !important;
  min-height: 40px !important; /* Slightly increased height */
}
.va-sidebar-item-content {
  padding: 8px 1.2rem !important; /* Slightly increased padding */
  min-height: 40px !important;
  display: flex !important;
  align-items: center !important;
}
.va-sidebar__menu, .va-sidebar__body, .va-accordion, .va-collapse, .va-collapse__body, .va-collapse__content, .va-collapse__content-wrapper {
  padding: 0 !important;
  margin: 0 !important;
  gap: 0 !important;
}
.va-sidebar-item .va-icon {
  color: #6b7280 !important;
}
.va-sidebar-item:hover {
  background-color: rgba(0, 0, 0, 0.04) !important;
}

/* Active State */
.va-sidebar-item--active {
  background-color: #f0f4f8 !important; 
  border-left: 3px solid var(--va-primary) !important;
}
.va-sidebar-item--active .va-sidebar-item-title,
.va-sidebar-item--active .va-icon {
  color: var(--va-primary) !important;
  font-weight: 600;
}

/* Dark Theme Sidebar Overrides */
.dark-theme-sidebar {
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}
.dark-theme-sidebar .va-sidebar-item {
  color: #e5e7eb !important;
}
.dark-theme-sidebar .va-sidebar-item .va-icon {
  color: #9ca3af !important;
}
.dark-theme-sidebar .va-sidebar-item:hover {
  background-color: rgba(255, 255, 255, 0.05) !important;
}
.dark-theme-sidebar .va-sidebar-item--active {
  background-color: rgba(59, 130, 246, 0.1) !important;
}
.dark-theme-sidebar .va-sidebar-item--active .va-sidebar-item-title,
.dark-theme-sidebar .va-sidebar-item--active .va-icon {
  color: #60A5FA !important;
}
@media (max-width: 768px) {
  body {
    overflow-x: hidden;
  }
  .responsive-main {
    padding: 0.25rem !important;
  }
  .username-text {
    display: none;
  }
  .navbar-right {
    padding-right: 0.75rem;
  }
  .responsive-sidebar {
    position: absolute;
    z-index: 1000;
    height: 100%;
  }
  .full-title { display: none; }
  .short-title { display: inline; font-size: 1.1rem; }
  .hide-mobile { display: none !important; }
  .show-mobile { display: flex !important; }
  .mobile-menu-btn { display: inline-flex !important; }
}
</style>
