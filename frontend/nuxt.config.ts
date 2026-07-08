// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  unhead: { legacy: true },
  modules: ['@vuestic/nuxt', 'nuxt-auth-utils'],
  build: {
  },
  app: {
    head: {
      link: [
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Source+Sans+Pro:ital,wght@0,400;1,700&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/icon?family=Material+Icons' }
      ]
    }
  },
  routeRules: {
    '/api/**': { proxy: 'http://localhost:8080/api/**' }
  },
  vite: {
    server: {
      allowedHosts: true
    }
  }
})