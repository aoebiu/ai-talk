<template>
  <div class="login-page">
    <div class="login-card">
      <button
        type="button"
        class="theme-toggle"
        :title="themeTooltip"
        @click="theme.toggleTheme()"
      >
        {{ theme.resolvedTheme === 'dark' ? '🌙' : '☀️' }}
      </button>
      <h1 class="title">AI Talk</h1>
      <p class="subtitle">登录后与 AI 对话</p>
      <form class="form" @submit.prevent="onSubmit">
        <input
          v-model="username"
          type="text"
          class="input"
          placeholder="用户名"
          autocomplete="username"
        />
        <input
          v-model="password"
          type="password"
          class="input"
          placeholder="密码"
          autocomplete="current-password"
        />
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" class="btn" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const themeTooltip = computed(() =>
  theme.resolvedTheme === 'dark' ? '切换到浅色模式' : '切换到深色模式'
)

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    router.replace('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-page-bg);
  transition: background 0.3s ease;
}
.login-card {
  position: relative;
  width: 100%;
  max-width: 360px;
  padding: 2rem;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  transition: all 0.3s ease;
}
.theme-toggle {
  position: absolute;
  top: 1rem;
  right: 1rem;
  padding: 0.4rem 0.6rem;
  font-size: 1.125rem;
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  line-height: 1;
}
.theme-toggle:hover {
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}
.title {
  margin: 0 0 0.25rem;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text-primary);
  transition: color 0.3s ease;
}
.subtitle {
  margin: 0 0 1.5rem;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  transition: color 0.3s ease;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.input {
  padding: 0.75rem 1rem;
  font-size: 1rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  outline: none;
  transition: all 0.3s ease;
}
.input::placeholder {
  color: var(--color-text-tertiary);
}
.input:focus {
  border-color: var(--color-border-focus);
}
.error {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-error);
  transition: color 0.3s ease;
}
.btn {
  padding: 0.75rem 1rem;
  font-size: 1rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}
.btn:hover:not(:disabled) {
  background: var(--color-button-primary-hover);
}
.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
