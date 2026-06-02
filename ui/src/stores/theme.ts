import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const THEME_KEY = 'theme-preference'

export type ThemeMode = 'light' | 'dark'

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>('light')

  const resolvedTheme = computed(() => mode.value)

  function setTheme(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem(THEME_KEY, newMode)
  }

  function toggleTheme() {
    const newMode = mode.value === 'light' ? 'dark' : 'light'
    setTheme(newMode)
  }

  function initTheme() {
    const stored = localStorage.getItem(THEME_KEY) as ThemeMode | null
    if (stored === 'light' || stored === 'dark') {
      mode.value = stored
    } else {
      mode.value = 'light'
    }
  }

  return { mode, resolvedTheme, setTheme, toggleTheme, initTheme }
})
