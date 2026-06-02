<template>
  <div class="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useThemeStore } from '@/stores/theme'

const theme = useThemeStore()

// 初始化主题
theme.initTheme()

// 监听并应用主题变化
watch(
  () => theme.resolvedTheme,
  (newTheme) => {
    document.documentElement.setAttribute('data-theme', newTheme)
  },
  { immediate: true }
)
</script>

<style>
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: system-ui, "Segoe UI", sans-serif;
    background: var(--color-bg-page);
    color: var(--color-text-primary);
    transition: background-color 0.3s ease, color 0.3s ease;
  }
  .app { min-height: 100vh; }
</style>
