// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: false },
  unhead: { legacy: true },
  modules: ['@vuestic/nuxt', 'nuxt-auth-utils', '@nuxtjs/i18n'],
  css: ['~/assets/main.css'],
  vuestic: {
    config: {
      i18n: {
        dropzone: '여기로 파일을 드래그 하거나 ',
        uploadFile: '내 PC에서 선택',
      }
    }
  },
  i18n: {
    locales: [
      { code: 'ko', file: 'ko.json' },
      { code: 'en', file: 'en.json' }
    ],
    defaultLocale: 'ko',
    strategy: 'no_prefix',
    lazy: true,
    langDir: 'locales/'
  },
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
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.API_BASE_URL || 'http://localhost:8080',
      agGridLicense: process.env.AG_GRID_LICENSE
    }
  },
  routeRules: {
    '/api/**': { proxy: process.env.API_BASE_URL ? `${process.env.API_BASE_URL}/api/**` : 'http://localhost:8080/api/**' }
  },
  vite: {
    server: {
      allowedHosts: true
    }
  }
})