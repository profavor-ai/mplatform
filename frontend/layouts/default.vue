<template>
  <va-layout>
    <template #top>
      <va-navbar color="primary" class="mb-2">
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
              <div class="lang-select hide-mobile">
                <va-select v-model="currentLocale" :options="[{text: '한국어', value: 'ko'}, {text: 'English', value: 'en'}]" value-by="value" background="#ffffff" class="small-select" style="font-size: 0.8rem;" />
              </div>
              <span class="user-info">
                <va-icon name="account_circle" class="mr-1" /> 
                <span class="username-text">{{ currentUser?.username || 'Admin' }} ({{ currentUser?.role || 'GUEST' }})</span>
              </span>
              <va-button color="danger" size="small" @click="handleLogout" style="margin-left: 0.5rem;" class="logout-btn">
                <span class="logout-text">Logout</span>
                <va-icon name="logout" class="logout-icon" />
              </va-button>
            </div>
          </va-navbar-item>
        </template>
      </va-navbar>
    </template>
    
    <template #left>
      <va-sidebar v-model="showSidebar" :minimized="false" class="responsive-sidebar">
        <va-sidebar-item :active="$route.path === '/'" to="/">
          <va-sidebar-item-content>
            <va-icon name="dashboard" />
            <va-sidebar-item-title>Dashboard</va-sidebar-item-title>
          </va-sidebar-item-content>
        </va-sidebar-item>
        <va-sidebar-item :active="$route.path === '/schema'" to="/schema">
          <va-sidebar-item-content>
            <va-icon name="list" />
            <va-sidebar-item-title>Domain Schema</va-sidebar-item-title>
          </va-sidebar-item-content>
        </va-sidebar-item>
        <va-sidebar-item :active="$route.path === '/records'" to="/records">
          <va-sidebar-item-content>
            <va-icon name="storage" />
            <va-sidebar-item-title>Records</va-sidebar-item-title>
          </va-sidebar-item-content>
        </va-sidebar-item>
        <router-link to="/approvals" style="text-decoration: none;">
          <va-sidebar-item :active="$route.path === '/approvals'">
            <va-sidebar-item-content>
              <va-icon name="check_circle" />
              <va-sidebar-item-title>Approvals</va-sidebar-item-title>
            </va-sidebar-item-content>
          </va-sidebar-item>
        </router-link>

        <router-link to="/admin" style="text-decoration: none;">
          <va-sidebar-item :active="$route.path === '/admin'">
            <va-sidebar-item-content>
              <va-icon name="admin_panel_settings" />
              <va-sidebar-item-title>Admin Monitor</va-sidebar-item-title>
            </va-sidebar-item-content>
          </va-sidebar-item>
        </router-link>
      </va-sidebar>
    </template>
    
    <template #content>
      <main class="responsive-main">
        <slot />
      </main>
    </template>
  </va-layout>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCookie, useState } from '#app'

const router = useRouter()
const tokenCookie = useCookie('auth_token')
const userCookie = useCookie('user_data')
const currentLocale = useCookie('locale', { default: () => 'ko' })
const showSidebar = ref(true)
const isMobile = ref(false)

const route = useRoute()

onMounted(() => {
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
  background-color: #f4f6f8;
  overflow: hidden; /* Force hide global scrollbar */
  height: 100vh;
  width: 100vw;
}
.small-select {
  --va-input-font-size: 0.8rem;
  --va-input-wrapper-min-height: 28px;
  --va-input-wrapper-border-radius: 4px;
}
.small-select .va-input-wrapper__text {
  font-size: 0.8rem !important;
}
.navbar-right {
  display: flex; 
  align-items: center; 
  gap: 0.5rem; 
  white-space: nowrap; 
  padding-right: 1rem;
}
.lang-select {
  width: 90px;
}
.user-info {
  display: flex;
  align-items: center;
}
.hide-mobile {
  display: block;
}
.mobile-menu-btn {
  display: inline-flex !important;
}
.responsive-main {
  padding: 1rem;
  height: calc(100vh - 64px);
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.short-title { display: none; }
.logout-icon { display: none; }

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
  .lang-select {
    width: 75px;
  }
  .navbar-right {
    padding-right: 0;
    gap: 0.2rem;
  }
  .responsive-sidebar {
    position: absolute;
    z-index: 1000;
    height: 100%;
  }
  .full-title { display: none; }
  .short-title { display: inline; font-size: 1.1rem; }
  .logout-text { display: none; }
  .logout-icon { display: inline-block; }
  .logout-btn { min-width: 0; padding: 0.2rem 0.5rem; }
  .hide-mobile { display: none !important; }
  .mobile-menu-btn { display: inline-flex !important; }
}
</style>
