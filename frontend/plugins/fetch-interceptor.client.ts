import { translateBackendError } from '~/utils/errorTranslator'

let isRefreshing = false
let refreshPromise: Promise<string | null> | null = null

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig()
  const accessMaxAge = Number(config.public.accessTokenExpirationSec || 1800)
  const refreshMaxAge = Number(config.public.refreshTokenExpirationSec || 172800)

  globalThis.$fetch = $fetch.create({
    async onResponseError({ request, response, options }) {
      if (response.status === 401 && process.client) {
        const reqUrl = request.toString()
        // If the failed request was login or refresh endpoint itself, do not retry
        if (reqUrl.includes('/api/auth/login') || reqUrl.includes('/api/auth/refresh')) {
          console.warn('Authentication failed or refresh token expired. Redirecting to login...')
          clearAuthCookies()
          window.location.href = '/login'
          return
        }

        const getCookieValue = (name: string) => {
          const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'))
          return match ? decodeURIComponent(match[2]) : null
        }

        const refreshToken = getCookieValue('refresh_token')
        if (!refreshToken) {
          console.warn('No refresh token available. Redirecting to login...')
          clearAuthCookies()
          window.location.href = '/login'
          return
        }

        if (!isRefreshing) {
          isRefreshing = true
          refreshPromise = (async () => {
            try {
              const res: any = await $fetch('/api/auth/refresh', {
                method: 'POST',
                body: { refreshToken }
              })

              if (res && res.token) {
                document.cookie = `auth_token=${res.token}; max-age=${accessMaxAge}; path=/;`
                if (res.refreshToken) {
                  document.cookie = `refresh_token=${res.refreshToken}; max-age=${refreshMaxAge}; path=/;`
                }
                return res.token
              }
              return null
            } catch (err) {
              console.error('Failed to refresh token:', err)
              return null
            } finally {
              isRefreshing = false
            }
          })()
        }

        const newToken = await refreshPromise
        refreshPromise = null

        if (newToken) {
          // Retry the failed original request with the new access token
          options.headers = options.headers || {}
          if (options.headers instanceof Headers) {
            options.headers.set('Authorization', `Bearer ${newToken}`)
          } else if (Array.isArray(options.headers)) {
            options.headers = options.headers.filter(([k]) => k.toLowerCase() !== 'authorization')
            options.headers.push(['Authorization', `Bearer ${newToken}`])
          } else {
            (options.headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`
          }
          return globalThis.$fetch(request, options)
        } else {
          clearAuthCookies()
          window.location.href = '/login'
          return
        }
      }

      // Universal Error Interceptor for message translation
      try {
        const i18n = nuxtApp.vueApp.config.globalProperties.$i18n
        if (i18n && i18n.t) {
          const t = (key: string, params?: any) => i18n.t(key, params)
          if (response._data) {
            if (typeof response._data === 'object' && response._data.errorCode) {
              response._data.translatedMessage = translateBackendError(response._data, t)
            } else if (typeof response._data === 'string') {
              response._data = translateBackendError(response._data, t)
            }
          }
        }
      } catch (e) {
        console.warn('Global error translation failed', e)
      }
    }
  })
})

function clearAuthCookies() {
  if (process.client) {
    document.cookie = 'auth_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
    document.cookie = 'refresh_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
    document.cookie = 'user_data=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
  }
}
