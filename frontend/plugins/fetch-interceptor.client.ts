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
    }
  })
})
