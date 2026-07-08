export default defineNuxtRouteMiddleware((to, from) => {
  if (to.path === '/login') {
    return
  }

  const token = useCookie('auth_token')
  
  if (!token.value) {
    return navigateTo('/login')
  }
})
