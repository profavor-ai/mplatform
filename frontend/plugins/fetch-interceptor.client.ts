export default defineNuxtPlugin((nuxtApp) => {
  globalThis.$fetch = $fetch.create({
    onResponseError({ request, response, options }) {
      if (response.status === 401) {
        if (process.client) {
          console.warn('401 Unauthorized detected. Redirecting to login...');
          
          document.cookie = 'auth_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
          document.cookie = 'user_data=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
          
          window.location.href = '/login';
        }
      }
      // Universal Error Interceptor
      try {
        const i18n = nuxtApp.vueApp.config.globalProperties.$i18n;
        if (i18n && i18n.t) {
          const t = (key: string, params?: any) => i18n.t(key, params);
          if (response._data) {
            if (typeof response._data === 'object' && response._data.errorCode) {
              response._data.translatedMessage = translateBackendError(response._data, t);
            } else if (typeof response._data === 'string') {
              response._data = translateBackendError(response._data, t);
            }
          }
        }
      } catch (e) {
        console.warn('Global error translation failed', e);
      }
    }
  })
})
