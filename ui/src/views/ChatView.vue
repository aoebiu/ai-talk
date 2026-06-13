<template>
  <div class="chat-page">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="title">AI Talk</h1>
        <button type="button" class="new-chat-btn" @click="handleNewChat">
          {{ creating ? '创建中…' : '+ 新建对话' }}
        </button>
      </div>
      <nav class="conversation-list">
        <div v-for="key in GROUP_KEYS" :key="key">
          <template v-if="groupedItems(key).length > 0">
            <div class="conv-group-label">{{ GROUP_LABELS[key] }}</div>
            <div
              v-for="c in groupedItems(key)"
              :key="c.id"
              class="conversation-item"
              :class="{ active: conv.currentId === c.id }"
              @click="handleSelectConversation(c.id)"
            >
              <span class="conversation-title">{{ c.title }}</span>
              <button
                type="button"
                class="conversation-delete-btn"
                title="删除对话"
                @click.stop="handleDeleteConversation(c.id)"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
              </button>
            </div>
          </template>
        </div>
        <p v-if="conv.conversations.length === 0" class="no-conversations">
          暂无对话，点击上方「新建对话」开始
        </p>
      </nav>
      <div class="sidebar-footer">
        <button
          type="button"
          class="theme-toggle"
          :title="themeTooltip"
          @click="theme.toggleTheme()"
        >
          {{ theme.resolvedTheme === 'dark' ? '🌙' : '☀️' }}
        </button>
        <button
          type="button"
          class="settings-btn"
          title="后台配置"
          @click="goToSettings"
        >
          ⚙️
        </button>
        <span class="user">{{ auth.user?.nickname || auth.user?.username }}</span>
        <button type="button" class="logout" @click="handleLogout">退出</button>
      </div>
    </aside>
    <main class="main">
      <header v-if="conv.currentId" class="chat-header">
        <span class="chat-header-title">{{ currentTitle }}</span>
      </header>
      <div v-if="!conv.currentId" class="empty-state">
        选择左侧一个对话，或点击「新建对话」开始与 AI 聊天
      </div>
      <template v-else>
        <div v-if="loadingHistory" class="empty">
          加载对话记录中…
        </div>
        <div v-else-if="conv.currentMessages.length === 0" class="empty">
          发送一条消息开始与 AI 对话
        </div>
        <div v-else class="messages">
          <div
            v-for="(m, i) in conv.currentMessages"
            :key="m.id ?? `${conv.currentId ?? 'session'}-${i}`"
            class="message"
            :class="m.role"
          >
            <!-- 编辑模式：独立居中 -->
            <div v-if="editingIndex === i" class="edit-area">
              <textarea
                v-model="editText"
                class="edit-textarea"
                rows="4"
              />
              <div class="edit-actions">
                <button type="button" class="edit-btn cancel" @click="handleCancelEdit">取消</button>
                <button type="button" class="edit-btn save" @click="handleSaveEdit(i)">发送</button>
              </div>
            </div>
            <!-- 正常气泡 -->
            <template v-else>
              <div class="bubble">
                <div class="content markdown-body" v-html="renderMarkdown(m.content)"></div>
              </div>
              <!-- 知识库来源标记（AI 气泡下方，每个知识库独立显示） -->
              <div v-if="m.role === 'assistant' && m.ragSources?.length" class="rag-badges">
                <button
                  v-for="kbName in uniqueKbNames(ragSourcesOf(m))"
                  :key="kbName"
                  type="button"
                  class="rag-badge"
                  @click="handleRagBadgeClick(ragSourcesOf(m), kbName)"
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                  {{ kbName }}
                </button>
              </div>
              <div v-if="m.role === 'user' && !streaming" class="message-actions">
                <button type="button" class="action-btn" title="重新生成" @click="handleRegenerate(i)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
                </button>
                <button type="button" class="action-btn" title="修改" @click="handleStartEdit(i)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
              </div>
            </template>
          </div>
        </div>
        <div class="input-area">
          <textarea
            v-model="inputText"
            class="textarea"
            placeholder="输入消息…"
            rows="2"
            :disabled="streaming"
            @keydown.enter.exact.prevent="send"
          />
          <button
            type="button"
            class="send-btn"
            :disabled="!inputText.trim() || streaming"
            @click="send"
          >
            {{ streaming ? '回复中…' : '发送' }}
          </button>
        </div>
      </template>
    </main>

    <!-- 知识库来源弹框 -->
    <Teleport to="body">
      <div v-if="ragModal" class="rag-modal-overlay" @click.self="ragModal = null">
        <div class="rag-modal">
          <div class="rag-modal-header">
            <span>引用的知识库片段</span>
            <button type="button" class="rag-modal-close" @click="ragModal = null">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="rag-modal-body">
            <div v-for="(src, si) in ragModal" :key="si" class="rag-source-item">
              <div class="rag-source-kb">{{ src.kbName || src.indexName || '知识库' }}</div>
              <div class="rag-source-text">{{ src.text }}</div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useConversationStore } from '@/stores/conversation'
import type { RagSource } from '@/stores/conversation'
import { marked } from 'marked'
import hljs from 'highlight.js/lib/common'
import { createChat, chatStream, getHistory, getSessions, deleteSession, getRagSourcesLatest, getRagSources } from '@/api/chat'
import type { HistoryMessage, HistoryData, HistoryResponse, SessionItem } from '@/api/chat'

marked.setOptions({ gfm: true, breaks: true })

const COPY_BTN = `<button class="copy-code-btn" type="button" title="复制代码"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg></button>`

const renderer = new marked.Renderer()
renderer.code = function ({ text, lang }: { text: string; lang?: string }) {
  let highlighted: string
  let langClass: string
  if (lang && hljs.getLanguage(lang)) {
    highlighted = hljs.highlight(text, { language: lang }).value
    langClass = `hljs language-${lang}`
  } else {
    highlighted = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    langClass = 'hljs'
  }
  return `<div class="code-block-wrapper"><pre><code class="${langClass}">${highlighted}</code></pre>${COPY_BTN}</div>`
}

function completeMarkdown(text: string): string {
  const lines = text.split('\n')
  let inCodeBlock = false
  for (const line of lines) {
    if (/^(`{3,}|~{3,})/.test(line.trimStart())) {
      inCodeBlock = !inCodeBlock
    }
  }
  return inCodeBlock ? text + '\n```' : text
}

function renderMarkdown(text: string): string {
  if (!text?.trim()) return ''
  try {
    const normalized = completeMarkdown(text.trim())
    const cached = markdownCache.get(normalized)
    if (cached) return cached
    const rawHtml = marked.parse(normalized, { async: false, renderer }) as string
    const safeHtml = sanitizeMarkdownHtml(rawHtml)
    markdownCache.set(normalized, safeHtml)
    return safeHtml
  } catch {
    return text
  }
}

function sanitizeMarkdownHtml(rawHtml: string): string {
  const parser = new DOMParser()
  const doc = parser.parseFromString(rawHtml, 'text/html')
  doc.querySelectorAll('script,style,iframe,object,embed,link,meta').forEach((el) => el.remove())
  doc.body.querySelectorAll('*').forEach((element) => {
    for (let i = element.attributes.length - 1; i >= 0; i -= 1) {
      const attr = element.attributes.item(i)
      if (!attr) continue
      const name = attr.name.toLowerCase()
      const value = attr.value.trim().toLowerCase()
      if (name.startsWith('on')) {
        element.removeAttribute(attr.name)
        continue
      }
      if ((name === 'href' || name === 'src') && value.startsWith('javascript:')) {
        element.removeAttribute(attr.name)
      }
    }
  })
  return doc.body.innerHTML
}

function handleCopyCode(e: MouseEvent) {
  const btn = (e.target as HTMLElement).closest('.copy-code-btn') as HTMLButtonElement | null
  if (!btn) return
  const wrapper = btn.closest('.code-block-wrapper')
  const code = wrapper?.querySelector('code')
  if (!code) return
  navigator.clipboard.writeText(code.textContent || '').then(() => {
    btn.classList.add('copied')
    btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>'
    setTimeout(() => {
      btn.classList.remove('copied')
      btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'
    }, 2000)
  })
}

onMounted(() => {
  document.addEventListener('click', handleCopyCode)
})
onUnmounted(() => {
  document.removeEventListener('click', handleCopyCode)
  abortStream?.()
  abortStream = null
  stopTypewriter()
})

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const conv = useConversationStore()

const themeTooltip = computed(() =>
  theme.resolvedTheme === 'dark' ? '切换到浅色模式' : '切换到深色模式'
)

const currentTitle = computed(() =>
  conv.conversations.find(c => c.id === conv.currentId)?.title ?? ''
)

const GROUP_KEYS = ['today', 'week', 'month', 'older'] as const
type GroupKey = (typeof GROUP_KEYS)[number]
const GROUP_LABELS: Record<GroupKey, string> = {
  today: '今天', week: '本周', month: '本月', older: '更久',
}
const markdownCache = new Map<string, string>()

const groupedConversations = computed(() => {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const dow = startOfToday.getDay()
  const startOfWeek = new Date(startOfToday)
  startOfWeek.setDate(startOfToday.getDate() - (dow === 0 ? 6 : dow - 1))
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)

  const groups: Record<GroupKey, typeof conv.conversations> = {
    today: [],
    week: [],
    month: [],
    older: [],
  }
  for (const c of conv.conversations) {
    const d = c.updatedAt ? new Date(c.updatedAt) : null
    if (!d || isNaN(d.getTime())) { groups.older.push(c); continue }
    if (d >= startOfToday)      groups.today.push(c)
    else if (d >= startOfWeek)  groups.week.push(c)
    else if (d >= startOfMonth) groups.month.push(c)
    else                        groups.older.push(c)
  }
  return groups
})

function groupedItems(key: GroupKey) {
  return groupedConversations.value[key]
}

const inputText = ref('')
const streaming = ref(false)
const creating = ref(false)
const loadingHistory = ref(false)
const editingIndex = ref<number | null>(null)
const editText = ref('')
const ragModal = ref<RagSource[] | null>(null)
let abortStream: (() => void) | null = null

// 打字机效果：后端 chunk 先入缓冲，再逐字显示；缓冲多时加快间隔
let typewriterBuffer = ''
let typewriterTimerId: ReturnType<typeof setTimeout> | null = null
let streamSessionId: string | null = null
const TYPEWRITER_BASE_MS = 48
const TYPEWRITER_FAST_MS = 16
const TYPEWRITER_FAST_THRESHOLD = 12

function stopTypewriter() {
  if (typewriterTimerId !== null) {
    clearTimeout(typewriterTimerId)
    typewriterTimerId = null
  }
  typewriterBuffer = ''
  streamSessionId = null
}

function flushOneChar() {
  const sid = streamSessionId
  if (!sid || typewriterBuffer === '') return
  const list = conv.currentMessages
  const last = list[list.length - 1]
  if (last?.role !== 'assistant') return
  const chars = [...typewriterBuffer]
  const first = chars[0]
  if (!first) return
  typewriterBuffer = chars.slice(1).join('')
  conv.updateLastMessage(sid, last.content + first)

  if (typewriterBuffer === '' && !streaming.value) {
    typewriterTimerId = null
    const sid = streamSessionId
    streamSessionId = null
    if (sid) {
      const messages = conv.currentMessages
      if (messages.length === 2 && messages[1]?.role === 'assistant') {
        const assistantIdx = messages.length - 1
        conv.fetchConversationTitle(sid).then((sourceIds) => {
          if (sourceIds?.length) {
            getRagSources(sourceIds).then((sourcesRes) => {
              if (sourcesRes.success && sourcesRes.data?.sources?.length) {
                conv.patchMessage(sid, assistantIdx, { ragSources: sourcesRes.data.sources })
              }
            }).catch(() => {})
          }
        })
      }
    }
    return
  }
  const ms = typewriterBuffer.length > TYPEWRITER_FAST_THRESHOLD ? TYPEWRITER_FAST_MS : TYPEWRITER_BASE_MS
  typewriterTimerId = setTimeout(flushOneChar, ms)
}

function startTypewriterIfNeeded() {
  if (typewriterTimerId !== null || typewriterBuffer === '' || !streamSessionId) return
  const ms = typewriterBuffer.length > TYPEWRITER_FAST_THRESHOLD ? TYPEWRITER_FAST_MS : TYPEWRITER_BASE_MS
  typewriterTimerId = setTimeout(flushOneChar, ms)
}

/** 从 getHistory 返回的 data 中解析出消息列表和 ragSourceMap */
function parseHistoryMessages(data: HistoryData | undefined): {
  messages: import('@/stores/conversation').MessageItem[]
  ragSourceMap: Record<string, number[]>
} {
  const rawList = Array.isArray(data) ? data : ((data as HistoryResponse)?.messages ?? [])
  const ragSourceMap: Record<string, number[]> =
    (!Array.isArray(data) ? (data as HistoryResponse)?.ragSourceMap : undefined) ?? {}
  const messages = (rawList as HistoryMessage[])
    .filter((m) => (m.role || '').toUpperCase() !== 'SYSTEM')
    .map((m) => {
      const role = ((m.role || 'user').toUpperCase() === 'USER' ? 'user' : 'assistant') as 'user' | 'assistant'
      return { id: m.id, role, content: m.content ?? '' }
    })
  return { messages, ragSourceMap }
}

/** 批量拉取 rag sources 并按 messageId 挂载到消息列表 */
async function attachRagSources(
  sessionId: string,
  messages: import('@/stores/conversation').MessageItem[],
  ragSourceMap: Record<string, number[]>
) {
  const allIds = Object.values(ragSourceMap).flat()
  if (!allIds.length) return
  try {
    const res = await getRagSources(allIds)
    if (!res.success || !res.data?.sources?.length) return
    // 构建 sourceId → messageId 反向映射
    const sourceToMsg: Record<number, number> = {}
    for (const [msgIdStr, ids] of Object.entries(ragSourceMap)) {
      for (const id of ids) sourceToMsg[id] = Number(msgIdStr)
    }
    // 按 messageId 分组
    const msgIdToSources: Record<number, RagSource[]> = {}
    for (const src of res.data.sources) {
      if (src.id == null) continue
      const msgId = sourceToMsg[src.id]
      if (msgId == null) continue
      msgIdToSources[msgId] ??= []
      msgIdToSources[msgId].push(src)
    }
    const patched = messages.map((m) =>
      m.id && msgIdToSources[m.id] ? { ...m, ragSources: msgIdToSources[m.id] } : m
    )
    conv.setMessages(sessionId, patched)
  } catch {}
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.replace('/login')
    return
  }
  // 刷新页面时：先拉取所有对话列表
  try {
    const sessionsRes = await getSessions()
    if (sessionsRes.success && sessionsRes.data && Array.isArray(sessionsRes.data)) {
      conv.setConversations(
        sessionsRes.data.map((s: SessionItem) => ({
          id: s.sessionId ?? s.id ?? '',
          title: s.title || '新对话',
          updatedAt: s.lastModified,
        }))
      )
    }
  } catch (e) {
    console.error('拉取对话列表失败:', e)
  }
  // 恢复当前选中的会话并拉取该对话历史
  const sessionId = conv.getPersistedSessionId()
  if (sessionId) {
    const exists = conv.conversations.some((c) => c.id === sessionId)
    if (exists) {
      conv.setCurrent(sessionId)
      try {
        const res = await getHistory(sessionId)
        if (res.success && res.data) {
          const { messages, ragSourceMap } = parseHistoryMessages(res.data)
          conv.setMessages(sessionId, messages)
          await attachRagSources(sessionId, messages, ragSourceMap)
        }
      } catch (e) {
        console.error('拉取对话历史失败:', e)
      }
    }
  }
})

async function handleDeleteConversation(sessionId: string) {
  const res = await deleteSession(sessionId)
  if (res.success) {
    conv.removeConversation(sessionId)
  }
}

async function handleNewChat() {
  if (creating.value) return
  creating.value = true
  try {
    const res = await createChat()
    if (res.success && res.data) {
      conv.addConversation(res.data.sessionId, res.data.title || '新对话')
    } else if (res.success && !res.data) {
      // 最近的会话已为空，直接切换过去
      const latest = conv.conversations[0]
      if (latest) conv.setCurrent(latest.id)
    }
  } finally {
    creating.value = false
  }
}

function goToSettings() {
  router.push('/settings')
}

async function handleLogout() {
  await auth.logout()
  router.replace('/login')
}

/** 点击左侧会话标题：切换当前会话并拉取该会话历史消息渲染 */
async function handleSelectConversation(sessionId: string) {
  conv.setCurrent(sessionId)
  loadingHistory.value = true
  try {
    const res = await getHistory(sessionId)
    if (res.success && res.data) {
      const { messages, ragSourceMap } = parseHistoryMessages(res.data)
      conv.setMessages(sessionId, messages)
      await attachRagSources(sessionId, messages, ragSourceMap)
    } else {
      conv.setMessages(sessionId, [])
    }
  } catch (e) {
    console.error('拉取对话历史失败:', e)
    conv.setMessages(sessionId, [])
  } finally {
    loadingHistory.value = false
  }
}

/** 从指定索引截断消息，重新发送 text 并流式获取 AI 回复 */
function resendFrom(index: number, text: string) {
  const sessionId = conv.currentId
  if (!sessionId || streaming.value) return
  const fromMessageId = conv.currentMessages[index]?.id
  if (fromMessageId == null) {
    console.warn('[ChatView] fromMessageId is null/undefined, regenerate will not delete messages on server')
  }
  conv.truncateMessagesFrom(sessionId, index)
  conv.appendMessage(sessionId, { role: 'user', content: text })
  conv.appendMessage(sessionId, { role: 'assistant', content: '' })
  const assistantMsgIdx = conv.currentMessages.length - 1
  streaming.value = true
  stopTypewriter()
  streamSessionId = sessionId
  abortStream = chatStream(
    { sessionId, message: text, fromMessageId },
    (chunk) => {
      typewriterBuffer += chunk
      startTypewriterIfNeeded()
    },
    () => {
      streaming.value = false
      abortStream = null
      const isFirstMessage = conv.currentMessages.length === 2 && conv.currentMessages[1]?.role === 'assistant'
      if (!isFirstMessage) {
        // 非首轮：单独拉取 rag source ids
        getRagSourcesLatest(sessionId).then((res) => {
          if (res.success && res.data?.sourceIds?.length) {
            getRagSources(res.data.sourceIds).then((sourcesRes) => {
              if (sourcesRes.success && sourcesRes.data?.sources?.length) {
                conv.patchMessage(sessionId, assistantMsgIdx, { ragSources: sourcesRes.data.sources })
              }
            }).catch(() => {})
          }
        }).catch(() => {})
      }
      if (typewriterBuffer === '' && typewriterTimerId === null) {
        const messages = conv.currentMessages
        if (messages.length === 2 && messages[1]?.role === 'assistant' && sessionId) {
          conv.fetchConversationTitle(sessionId).then((sourceIds) => {
            if (sourceIds?.length) {
              getRagSources(sourceIds).then((sourcesRes) => {
                if (sourcesRes.success && sourcesRes.data?.sources?.length) {
                  conv.patchMessage(sessionId, assistantMsgIdx, { ragSources: sourcesRes.data.sources })
                }
              }).catch(() => {})
            }
          })
        }
      }
    },
    (err) => {
      streaming.value = false
      abortStream = null
      stopTypewriter()
      const list = conv.currentMessages
      const last = list[list.length - 1]
      if (last?.role === 'assistant') {
        conv.updateLastMessage(sessionId!, '回复出错：' + (err.message || '未知错误'))
      }
    }
  )
}

/** 重新生成：用同一条用户消息重新请求 AI 回复 */
function handleRegenerate(index: number) {
  const msg = conv.currentMessages[index]
  if (!msg || msg.role !== 'user') return
  resendFrom(index, msg.content)
}

/** 进入编辑模式 */
function handleStartEdit(index: number) {
  const msg = conv.currentMessages[index]
  if (!msg || msg.role !== 'user') return
  editingIndex.value = index
  editText.value = msg.content
}

/** 保存编辑并重新发送 */
function handleSaveEdit(index: number) {
  const text = editText.value.trim()
  if (!text) return
  editingIndex.value = null
  editText.value = ''
  resendFrom(index, text)
}

/** 取消编辑 */
function handleCancelEdit() {
  editingIndex.value = null
  editText.value = ''
}

/** 返回来源列表中去重后的知识库名称列表 */
function uniqueKbNames(sources: RagSource[]): string[] {
  const seen = new Set<string>()
  const result: string[] = []
  for (const s of sources) {
    const name = s.kbName || s.indexName || '知识库'
    if (!seen.has(name)) { seen.add(name); result.push(name) }
  }
  return result
}

function ragSourcesOf(message: { ragSources?: RagSource[] }): RagSource[] {
  return message.ragSources ?? []
}

/** 点击某个知识库 badge：展示该知识库的所有片段 */
function handleRagBadgeClick(sources: RagSource[], kbName: string) {
  ragModal.value = sources.filter((s) => (s.kbName || s.indexName || '知识库') === kbName)
}

function send() {
  const text = inputText.value.trim()
  const sessionId = conv.currentId
  if (!text || !sessionId || streaming.value) return

  conv.appendMessage(sessionId, { role: 'user', content: text })
  const isFirstUserMessage =
    conv.currentMessages.filter((m) => m.role === 'user').length === 1
  if (isFirstUserMessage) {
    conv.updateTitle(sessionId, text.slice(0, 24) + (text.length > 24 ? '…' : ''))
  }

  inputText.value = ''
  conv.appendMessage(sessionId, { role: 'assistant', content: '' })
  const assistantMsgIdx = conv.currentMessages.length - 1
  streaming.value = true
  stopTypewriter()
  streamSessionId = sessionId
  abortStream = chatStream(
    { sessionId, message: text },
    (chunk) => {
      typewriterBuffer += chunk
      startTypewriterIfNeeded()
    },
    () => {
      streaming.value = false
      abortStream = null
      const isFirstMessage = conv.currentMessages.length === 2 && conv.currentMessages[1]?.role === 'assistant'
      if (!isFirstMessage) {
        // 非首轮：单独拉取 rag source ids
        getRagSourcesLatest(sessionId).then((res) => {
          if (res.success && res.data?.sourceIds?.length) {
            getRagSources(res.data.sourceIds).then((sourcesRes) => {
              if (sourcesRes.success && sourcesRes.data?.sources?.length) {
                conv.patchMessage(sessionId, assistantMsgIdx, { ragSources: sourcesRes.data.sources })
              }
            }).catch(() => {})
          }
        }).catch(() => {})
      }
      // 缓冲未打完时由 flushOneChar 继续打完再结束；已打完则这里直接触发标题拉取
      if (typewriterBuffer === '' && typewriterTimerId === null) {
        const messages = conv.currentMessages
        if (messages.length === 2 && messages[1]?.role === 'assistant' && sessionId) {
          conv.fetchConversationTitle(sessionId).then((sourceIds) => {
            if (sourceIds?.length) {
              getRagSources(sourceIds).then((sourcesRes) => {
                if (sourcesRes.success && sourcesRes.data?.sources?.length) {
                  conv.patchMessage(sessionId, assistantMsgIdx, { ragSources: sourcesRes.data.sources })
                }
              }).catch(() => {})
            }
          })
        }
      }
    },
    (err) => {
      streaming.value = false
      abortStream = null
      stopTypewriter()
      const list = conv.currentMessages
      const last = list[list.length - 1]
      if (last?.role === 'assistant') {
        conv.updateLastMessage(
          sessionId!,
          '回复出错：' + (err.message || '未知错误')
        )
      }
    }
  )
}
</script>

<style scoped>
.chat-page {
  height: 100vh;
  min-height: 0;
  display: flex;
  overflow: hidden;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}

.sidebar {
  width: 260px;
  min-width: 260px;
  flex-shrink: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-card);
  transition: border-color 0.3s ease, background-color 0.3s ease;
}

.sidebar-header {
  padding: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.title {
  margin: 0 0 0.75rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.new-chat-btn {
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-btn:hover:not(:disabled) {
  border-color: var(--color-border-focus);
  background: var(--color-bg-page);
}

.new-chat-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0.5rem;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}

.conversation-list:hover {
  scrollbar-color: var(--color-border-hover) transparent;
}

.conversation-list::-webkit-scrollbar {
  width: 4px;
}

.conversation-list::-webkit-scrollbar-track {
  background: transparent;
}

.conversation-list::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 999px;
}

.conversation-list:hover::-webkit-scrollbar-thumb {
  background: var(--color-border-hover);
}

.conversation-list::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-focus);
}

.conversation-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.25rem;
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
  box-sizing: border-box;
}

.conversation-item:hover {
  background: var(--color-bg-input);
}

.conversation-item.active {
  background: var(--color-bg-input);
  color: var(--color-text-accent);
}

.conversation-delete-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s ease, color 0.15s ease, background-color 0.15s ease;
}

.conversation-item:hover .conversation-delete-btn,
.conversation-item.active .conversation-delete-btn {
  opacity: 1;
}

.conversation-delete-btn:hover {
  color: #ef4444;
  background: var(--color-bg-page);
}

.conversation-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-conversations {
  padding: 1rem 0.75rem;
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.conv-group-label {
  padding: 0.5rem 0.75rem 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
}

.theme-toggle {
  padding: 0.35rem 0.5rem;
  font-size: 1rem;
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-toggle:hover {
  border-color: var(--color-border-focus);
  background: var(--color-bg-page);
}

.settings-btn {
  padding: 0.35rem 0.5rem;
  font-size: 1rem;
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.settings-btn:hover {
  border-color: var(--color-border-focus);
  background: var(--color-bg-page);
}

.user {
  flex: 1;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout {
  padding: 0.35rem 0.6rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  padding: 1rem 1.5rem;
}

.chat-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-bottom: 0.75rem;
  margin-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.chat-header-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state,
.empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9375rem;
  color: var(--color-text-tertiary);
}

.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-bottom: 1rem;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}

.messages:hover {
  scrollbar-color: var(--color-border-hover) transparent;
}

.messages::-webkit-scrollbar {
  width: 4px;
}

.messages::-webkit-scrollbar-track {
  background: transparent;
}

.messages::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 999px;
}

.messages:hover::-webkit-scrollbar-thumb {
  background: var(--color-border-hover);
}

.messages::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-focus);
}

.message {
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
}

.message.user {
  align-items: flex-end;
  padding-right: 3rem;
}

.message.assistant {
  align-items: flex-start;
  padding-left: 3rem;
}

.bubble {
  max-width: 75%;
  padding: 0.65rem 1rem;
  border-radius: 1rem;
  font-size: 0.9375rem;
  line-height: 1.6;
  word-break: break-word;
}

.message.user .bubble {
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-bottom-right-radius: 0.25rem;
  color: var(--color-text-primary);
}

.message.assistant .bubble {
  background: transparent;
  padding: 0.65rem 0;
  color: var(--color-text-primary);
}

.message-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.35rem;
}

/* RAG 知识库来源标记容器 */
.rag-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin-top: 0.25rem;
}

/* RAG 知识库来源标记按钮 */
.rag-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  border: 1px solid var(--color-border);
  background: transparent;
  margin-top: 0.25rem;
  align-self: flex-end;
  transition: color 0.15s ease, border-color 0.15s ease;
}

.rag-badge:hover {
  color: var(--color-text-accent);
  border-color: var(--color-text-accent);
}

/* 弹框遮罩 */
.rag-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.rag-modal {
  width: 540px;
  max-width: calc(100vw - 2rem);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.rag-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.9rem 1.1rem;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  flex-shrink: 0;
}

.rag-modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.rag-modal-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.rag-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 1.1rem;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}

.rag-modal-body:hover {
  scrollbar-color: var(--color-border-hover) transparent;
}

.rag-modal-body::-webkit-scrollbar {
  width: 4px;
}

.rag-modal-body::-webkit-scrollbar-track {
  background: transparent;
}

.rag-modal-body::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 999px;
}

.rag-modal-body:hover::-webkit-scrollbar-thumb {
  background: var(--color-border-hover);
}

.rag-modal-body::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-focus);
}

.rag-source-item {
  padding: 0.75rem 0;
}

.rag-source-item + .rag-source-item {
  border-top: 1px solid var(--color-border);
}

.rag-source-kb {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-accent);
  margin-bottom: 0.35rem;
  letter-spacing: 0.02em;
}

.rag-source-text {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.action-btn svg {
  display: block;
}

.edit-area {
  width: 100%;
  align-self: center;
  padding: 0 3rem;
}

.edit-textarea {
  width: 100%;
  min-height: 100px;
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 12px;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s ease;
}

.edit-textarea:focus {
  border-color: var(--color-border-focus);
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.edit-btn {
  padding: 0.4rem 1rem;
  font-size: 0.8125rem;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.edit-btn.save {
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  font-weight: 500;
}

.edit-btn.save:hover {
  background: var(--color-button-primary-hover);
}

.edit-btn.cancel {
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
}

.edit-btn.cancel:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
}

.message .content {
  font-size: 0.9375rem;
  line-height: 1.6;
  word-break: break-word;
}

.message .content.markdown-body :deep(p) {
  margin: 0.5em 0;
}
.message .content.markdown-body :deep(p:first-child) {
  margin-top: 0;
}
.message .content.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.message .content.markdown-body :deep(code) {
  padding: 0.2em 0.4em;
  font-size: 0.9em;
  background: var(--color-bg-input);
  border-radius: 4px;
  border: 1px solid var(--color-border);
}
.message .content.markdown-body :deep(.code-block-wrapper) {
  position: relative;
  margin: 0.5em 0;
}
.message .content.markdown-body :deep(pre) {
  margin: 0;
  padding: 0.75rem 1rem;
  padding-bottom: 2rem;
  overflow-x: auto;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
}
.message .content.markdown-body :deep(.copy-code-btn) {
  position: absolute;
  right: 0.5rem;
  bottom: 0.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.message .content.markdown-body :deep(.copy-code-btn:hover) {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}
.message .content.markdown-body :deep(.copy-code-btn.copied) {
  color: #22c55e;
  border-color: #22c55e;
}
.message .content.markdown-body :deep(pre code) {
  padding: 0;
  background: none;
  border: none;
  font-size: 0.875rem;
}
.message .content.markdown-body :deep(ul),
.message .content.markdown-body :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}
.message .content.markdown-body :deep(li) {
  margin: 0.2em 0;
}
.message .content.markdown-body :deep(blockquote) {
  margin: 0.5em 0;
  padding-left: 1em;
  border-left: 4px solid var(--color-border-focus);
  color: var(--color-text-secondary);
}
.message .content.markdown-body :deep(h1),
.message .content.markdown-body :deep(h2),
.message .content.markdown-body :deep(h3) {
  margin: 0.75em 0 0.35em;
  font-weight: 600;
  line-height: 1.3;
}
.message .content.markdown-body :deep(h1) { font-size: 1.25rem; }
.message .content.markdown-body :deep(h2) { font-size: 1.1rem; }
.message .content.markdown-body :deep(h3) { font-size: 1rem; }
.message .content.markdown-body :deep(a) {
  color: var(--color-text-accent);
  text-decoration: none;
}
.message .content.markdown-body :deep(a:hover) {
  text-decoration: underline;
}
.message .content.markdown-body :deep(strong) {
  font-weight: 600;
}
.message .content.markdown-body :deep(table) {
  border-collapse: collapse;
  font-size: 0.9em;
}
.message .content.markdown-body :deep(th),
.message .content.markdown-body :deep(td) {
  padding: 0.35em 0.6em;
  border: 1px solid var(--color-border);
}
.message .content.markdown-body :deep(thead th) {
  background: var(--color-bg-input);
  font-weight: 600;
}
.message .content.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0.75em 0;
}

.input-area {
  flex-shrink: 0;
  display: flex;
  gap: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.textarea {
  flex: 1;
  min-width: 0;
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  resize: none;
  outline: none;
  transition: all 0.3s ease;
}

.textarea::placeholder {
  color: var(--color-text-tertiary);
}

.textarea:focus {
  border-color: var(--color-border-focus);
}

.send-btn {
  padding: 0.75rem 1.25rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  align-self: flex-end;
  transition: background-color 0.3s ease;
}

.send-btn:hover:not(:disabled) {
  background: var(--color-button-primary-hover);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
