import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { setTokenExpiredCallback } from '@/api/request'
import ChatView from '../views/ChatView.vue'
import LoginView from '../views/LoginView.vue'
import SettingsLayout from '../layouts/SettingsLayout.vue'
import SettingsView from '../views/SettingsView.vue'
import FunctionCallDetailView from '../views/FunctionCallDetailView.vue'
import DocumentDetailView from '../views/DocumentDetailView.vue'
import KnowledgeBaseCreateView from '../views/KnowledgeBaseCreateView.vue'
import KnowledgeBaseDetailView from '../views/KnowledgeBaseDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'chat',
      component: ChatView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      component: SettingsLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'settings',
          component: SettingsView,
        },
        {
          path: 'function/create',
          name: 'functionCallCreate',
          component: FunctionCallDetailView,
        },
        {
          path: 'function/:id',
          name: 'functionCallDetail',
          component: FunctionCallDetailView,
        },
        {
          path: 'document/:id',
          name: 'documentDetail',
          component: DocumentDetailView,
        },
        {
          path: 'kb/create',
          name: 'kbCreate',
          component: KnowledgeBaseCreateView,
        },
        {
          path: 'kb/:kbId/upload',
          name: 'kbUpload',
          component: KnowledgeBaseCreateView,
        },
        {
          path: 'kb/:kbId',
          name: 'kbDetail',
          component: KnowledgeBaseDetailView,
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
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
