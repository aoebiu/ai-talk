<template>
  <div class="settings-page">
    <aside class="settings-sidebar">
      <div class="sidebar-header">
        <button type="button" class="back-btn" @click="goToChat">← 返回对话</button>
        <button
          type="button"
          class="theme-toggle"
          :title="themeTooltip"
          @click="theme.toggleTheme()"
        >
          {{ theme.resolvedTheme === 'dark' ? '🌙' : '☀️' }}
        </button>
      </div>
      <nav class="settings-nav">
        <div v-for="group in menuGroups" :key="group.title" class="nav-group">
          <div class="nav-group-title">{{ group.title }}</div>
          <button
            v-for="item in group.items"
            :key="item.id"
            type="button"
            class="nav-item"
            :class="{ active: activeSection === item.id }"
            @click="navigateSection(item.id)"
          >
            {{ item.label }}
          </button>
        </div>
      </nav>
      <div class="sidebar-footer">
        <span class="user">{{ auth.user?.nickname || auth.user?.username }}</span>
        <button type="button" class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <main class="settings-body">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

interface MenuItem {
  id: string
  label: string
}

interface MenuGroup {
  title: string
  items: MenuItem[]
}

const menuGroups: MenuGroup[] = [
  {
    title: '知识库',
    items: [
      { id: 'documents', label: '文档知识库' },
      { id: 'functionCall', label: 'Function Call' },
    ],
  },
  {
    title: '系统设置',
    items: [
      { id: 'apikey', label: 'API Key 管理' },
      { id: 'bizConfig', label: '外部服务配置' },
      { id: 'modelConfig', label: '模型配置' },
      { id: 'accounts', label: '账号管理' },
    ],
  },
]

const menuItems = menuGroups.flatMap((group) => group.items)

const themeTooltip = computed(() =>
  theme.resolvedTheme === 'dark' ? '切换到浅色模式' : '切换到深色模式',
)

/** 子路由与左侧菜单高亮映射 */
const activeSection = computed(() => {
  const name = route.name as string | undefined
  if (name === 'functionCallCreate' || name === 'functionCallDetail') {
    return 'functionCall'
  }
  if (
    name === 'kbCreate' ||
    name === 'kbUpload' ||
    name === 'kbDetail' ||
    name === 'documentDetail'
  ) {
    return 'documents'
  }
  const section = route.query.section as string | undefined
  if (section && menuItems.some((item) => item.id === section)) {
    return section
  }
  return 'documents'
})

function navigateSection(id: string) {
  router.push({ name: 'settings', query: { section: id } })
}

function goToChat() {
  router.push('/')
}

async function handleLogout() {
  try {
    await logout()
    auth.clearAuth()
    router.push('/login')
  } catch {
    // ignore
  }
}
</script>

<style scoped>
.settings-page {
  height: 100vh;
  overflow: hidden;
  display: flex;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}

.settings-sidebar {
  width: 240px;
  min-width: 240px;
  height: 100vh;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-card);
  transition: border-color 0.3s ease, background-color 0.3s ease;
  z-index: 10;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.back-btn {
  flex: 1;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.back-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}

.sidebar-header .theme-toggle {
  padding: 0.4rem 0.6rem;
  font-size: 1.125rem;
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-header .theme-toggle:hover {
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}

.settings-nav {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.nav-group + .nav-group {
  margin-top: 0.75rem;
}

.nav-group-title {
  padding: 0.4rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  user-select: none;
}

.nav-item {
  width: 100%;
  padding: 0.6rem 0.75rem;
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
  text-align: left;
  color: var(--color-text-primary);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-item:hover {
  background: var(--color-bg-input);
}

.nav-item.active {
  background: var(--color-bg-input);
  color: var(--color-text-accent);
  font-weight: 500;
}

.sidebar-footer {
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
}

.sidebar-footer .user {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.sidebar-footer .logout-btn {
  margin-top: 0.5rem;
  width: 100%;
  padding: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-footer .logout-btn:hover {
  color: var(--color-error);
  border-color: var(--color-error);
  background: rgba(220, 53, 69, 0.08);
}

.settings-body {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}
</style>
