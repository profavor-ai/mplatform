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
          const t = (key, params) => i18n.t(key, params);
          if (typeof response._data === 'string') {
            response._data = translateBackendError(response._data, t);
          } else if (response._data && typeof response._data.message === 'string') {
            response._data.message = translateBackendError(response._data.message, t);
          }
        }
      } catch (e) {
        console.warn('Global error translation failed', e);
      }
    }
  })
})
