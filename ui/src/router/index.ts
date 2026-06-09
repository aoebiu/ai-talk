import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { setTokenExpiredCallback } from '@/api/request'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      component: () => import('../layouts/SettingsLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'settings',
          component: () => import('../views/SettingsView.vue'),
        },
        {
          path: 'function/create',
          name: 'functionCallCreate',
          component: () => import('../views/FunctionCallDetailView.vue'),
        },
        {
          path: 'function/:id',
          name: 'functionCallDetail',
          component: () => import('../views/FunctionCallDetailView.vue'),
        },
        {
          path: 'document/:id',
          name: 'documentDetail',
          component: () => import('../views/DocumentDetailView.vue'),
        },
        {
          path: 'kb/create',
          name: 'kbCreate',
          component: () => import('../views/KnowledgeBaseCreateView.vue'),
        },
        {
          path: 'kb/:kbId/upload',
          name: 'kbUpload',
          component: () => import('../views/KnowledgeBaseCreateView.vue'),
        },
        {
          path: 'kb/:kbId',
          name: 'kbDetail',
          component: () => import('../views/KnowledgeBaseDetailView.vue'),
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { guest: true },
    },
  ],
})

// 设置 token 失效回调
setTokenExpiredCallback(() => {
  const auth = useAuthStore()
  auth.clearAuth()
  router.push('/login')
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.isLoggedIn && to.path === '/login') {
    return { path: '/' }
  }
  return true
})

export default router
