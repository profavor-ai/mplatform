<template>
  <div class="auth-container" style="opacity: 0;" :style="{ opacity: isMounted ? 1 : 0, transition: 'opacity 0.3s ease' }">
    <div class="auth-box">
      <!-- Welcome Header -->
      <div class="auth-header">
        <div class="logo-container">
          <va-icon name="hub" size="large" color="primary" />
        </div>
        <h1 class="title">Domain System</h1>
        <p class="subtitle">Secure Data Classification Platform</p>
      </div>

      <va-card class="auth-card">
        <va-tabs v-model="activeTab" style="width: 100%; border-bottom: 1px solid #eee;">
          <template #tabs>
            <va-tab name="login" style="flex: 1; text-align: center;">Login</va-tab>
            <va-tab name="register" style="flex: 1; text-align: center;">Register</va-tab>
          </template>
        </va-tabs>

        <va-card-content class="auth-content">
          <!-- LOGIN FORM -->
          <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
            <va-input 
              ref="usernameInputRef"
              v-model="loginForm.username" 
              label="Username" 
              placeholder="Enter your username"
              class="w-full mb-4"
              outline
              tabindex="1"
              :error="!!errorMessage && activeTab === 'login'"
              @keydown.tab.prevent="focusPassword"
            >
              <template #prependInner>
                <va-icon name="person" color="secondary" />
              </template>
            </va-input>
            
            <va-input 
              ref="passwordInputRef"
              v-model="loginForm.password" 
              label="Password" 
              type="password" 
              placeholder="Enter your password"
              class="w-full mb-4"
              outline
              tabindex="2"
              :error="!!errorMessage && activeTab === 'login'"
            >
              <template #prependInner>
                <va-icon name="lock" color="secondary" />
              </template>
            </va-input>
            
            <va-alert v-if="errorMessage && activeTab === 'login'" color="danger" class="mb-4 text-sm" outline>
              <template #icon><va-icon name="warning" /></template>
              {{ errorMessage }}
            </va-alert>
            
            <va-button type="submit" size="large" class="w-full mt-2" :loading="loading" style="border-radius: 8px;">
              Sign In
            </va-button>
          </form>

          <!-- REGISTER FORM -->
          <form v-if="activeTab === 'register'" @submit.prevent="handleRegister" class="auth-form">
            <va-input 
              v-model="registerForm.username" 
              label="Username" 
              placeholder="Choose a username"
              class="w-full mb-4"
              outline
              tabindex="1"
            >
              <template #prependInner>
                <va-icon name="person_add" color="secondary" />
              </template>
            </va-input>
            
            <va-input 
              v-model="registerForm.password" 
              label="Password" 
              type="password" 
              placeholder="Create a password"
              class="w-full mb-4"
              outline
              tabindex="2"
            >
              <template #prependInner>
                <va-icon name="lock" color="secondary" />
              </template>
            </va-input>

            <va-select
              v-model="registerForm.role"
              :options="['USER', 'MANAGER', 'ADMIN']"
              label="Role"
              class="w-full mb-4"
              outline
              placeholder="Select your role"
              tabindex="3"
            >
              <template #prependInner>
                <va-icon name="badge" color="secondary" />
              </template>
            </va-select>
            
            <va-alert v-if="errorMessage && activeTab === 'register'" color="danger" class="mb-4 text-sm" outline>
              <template #icon><va-icon name="warning" /></template>
              {{ errorMessage }}
            </va-alert>
            
            <va-alert v-if="successMessage && activeTab === 'register'" color="success" class="mb-4 text-sm" outline>
              <template #icon><va-icon name="check_circle" /></template>
              {{ successMessage }}
            </va-alert>
            
            <va-button type="submit" size="large" color="success" class="w-full mt-2" :loading="loading" style="border-radius: 8px;" :outline="isDark">
              Create Account
            </va-button>
          </form>
        </va-card-content>
      </va-card>

      <div class="auth-footer">
        &copy; 2026 Domain System. All rights reserved.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCookie, useRuntimeConfig } from '#app'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const isMounted = ref(false)
onMounted(() => {
  isMounted.value = true
})
const usernameInputRef = ref(null)
const passwordInputRef = ref(null)

const focusPassword = () => {
  if (passwordInputRef.value) {
    if (typeof passwordInputRef.value.focus === 'function') {
      passwordInputRef.value.focus()
    } else if (passwordInputRef.value.$el) {
      const inputEl = passwordInputRef.value.$el.querySelector('input')
      if (inputEl) inputEl.focus()
    }
  }
}

const activeTab = ref('login')

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', role: 'EDITOR' })

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const router = useRouter()

const config = useRuntimeConfig()
const accessMaxAge = Number(config.public.accessTokenExpirationSec || 1800)
const refreshMaxAge = Number(config.public.refreshTokenExpirationSec || 172800)

const tokenCookie = useCookie('auth_token', { maxAge: accessMaxAge })
const refreshTokenCookie = useCookie('refresh_token', { maxAge: refreshMaxAge })
const userCookie = useCookie('user_data', { maxAge: accessMaxAge })
const userPermissionsCookie = useCookie('user_permissions', { maxAge: accessMaxAge })

definePageMeta({
  layout: false
})

const handleLogin = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await $fetch('/api/auth/login', {
      method: 'POST',
      body: loginForm.value
    })
    
    tokenCookie.value = response.token
    if (response.refreshToken) {
      refreshTokenCookie.value = response.refreshToken
    }
    if (response.permissions) {
      userPermissionsCookie.value = response.permissions
    }
    userCookie.value = JSON.stringify({
      id: response.id || response.uuid,
      uuid: response.uuid || response.id,
      username: response.username,
      role: response.role,
      organizationId: response.organizationId,
      departmentId: response.departmentId,
      timezone: response.timezone,
      serverOffset: response.serverOffset,
      permissions: response.permissions || []
    })
    
    const tzCookie = useCookie('timezone', { default: () => 'Asia/Seoul' })
    tzCookie.value = response.timezone || 'Asia/Seoul'
    
    const serverOffsetCookie = useCookie('server_offset', { default: () => '+09:00' })
    serverOffsetCookie.value = response.serverOffset || '+09:00'
    
    window.location.href = '/'
  } catch (error) {
    console.error("Login error:", error);
    errorMessage.value = error.response?.status === 401 ? 'Invalid username or password.' : 'Login failed. Please check the backend connection. (' + (error.message || '') + ')'
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.role) {
    errorMessage.value = 'Please fill out all fields.'
    loading.value = false
    return
  }

  try {
    await $fetch('/api/auth/register', {
      method: 'POST',
      body: registerForm.value
    })
    
    successMessage.value = 'Account created successfully! You can now log in.'
    
    // Auto switch to login tab after brief delay and populate username
    setTimeout(() => {
      loginForm.value.username = registerForm.value.username
      loginForm.value.password = ''
      registerForm.value = { username: '', password: '', role: 'EDITOR' }
      successMessage.value = ''
      activeTab.value = 'login'
    }, 1500)
    
  } catch (error) {
    errorMessage.value = error.response?._data || 'Registration failed. Username might already exist.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  font-family: 'Inter', sans-serif;
  padding: 1rem;
}

.auth-box {
  width: 100%;
  max-width: 420px;
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  margin-bottom: 1rem;
}

.title {
  font-size: 1.75rem;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  font-size: 0.95rem;
  color: #64748b;
  margin: 0;
}

.auth-card {
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  background: white;
}

.auth-content {
  padding: 2rem !important;
}

.auth-form {
  display: flex;
  flex-direction: column;
}

.w-full {
  width: 100%;
}

.mb-4 {
  margin-bottom: 1.25rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.text-sm {
  font-size: 0.875rem;
}

.auth-footer {
  text-align: center;
  margin-top: 2rem;
  font-size: 0.85rem;
  color: #94a3b8;
}

/* Deep overrides for Vuestic inputs to make them look more modern */
:deep(.va-input-wrapper__field) {
  border-radius: 8px;
}
</style>
