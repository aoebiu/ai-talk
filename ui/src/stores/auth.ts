import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { MemberInfo } from '@/api/auth'
import * as authApi from '@/api/auth'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  let initialUser: MemberInfo | null = null
  try {
    const raw = localStorage.getItem(USER_KEY)
    initialUser = raw ? (JSON.parse(raw) as MemberInfo) : null
  } catch {
    /* ignore */
  }
  const user = ref<MemberInfo | null>(initialUser)

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(t: string, u: MemberInfo) {
    token.value = t
    user.value = u
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
  }

  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    if (!res.success || !res.data) {
      throw new Error(res.message || '登录失败')
    }
    const data = res.data
    setAuth(data.token, data)
    return data
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }

  return { token, user, isLoggedIn, setAuth, clearAuth, login, logout }
})
