import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { $get } from '@/api/request'

export interface ConversationItem {
  id: string
  title: string
  updatedAt?: string
}

export type MessageRole = 'user' | 'assistant'
export interface RagSource {
  kbName?: string
  indexName?: string
  text: string
}
export interface MessageItem {
  id?: number
  role: MessageRole
  content: string
  ragSources?: RagSource[] // 已加载的知识库来源（懒加载或历史恢复时批量填充）
}

const STORAGE_KEY_SESSION = 'ai-talk-current-session'

/**
 * 对话列表与消息保存在内存中；当前会话 id 会持久化，刷新后通过 /history/{sessionId} 拉取历史恢复
 */
export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<ConversationItem[]>([])
  const currentId = ref<string | null>(null)
  const messagesCache = ref<Record<string, MessageItem[]>>({})

  const currentConversation = computed(() =>
    conversations.value.find((c) => c.id === currentId.value)
  )

  const currentMessages = computed(() => {
    const id = currentId.value
    if (!id) return []
    return messagesCache.value[id] ?? []
  })

  function addConversation(sessionId: string, title?: string) {
    const item: ConversationItem = {
      id: sessionId,
      title: title ?? '新对话',
      updatedAt: new Date().toISOString(),
    }
    conversations.value = [item, ...conversations.value]
    currentId.value = sessionId
    messagesCache.value[sessionId] = []
    try {
      localStorage.setItem(STORAGE_KEY_SESSION, sessionId)
    } catch {}
  }

  function setCurrent(sessionId: string | null) {
    currentId.value = sessionId
    try {
      if (sessionId) localStorage.setItem(STORAGE_KEY_SESSION, sessionId)
      else localStorage.removeItem(STORAGE_KEY_SESSION)
    } catch {}
  }

  function setMessages(sessionId: string, messages: MessageItem[]) {
    messagesCache.value[sessionId] = messages
  }

  function appendMessage(sessionId: string, message: MessageItem) {
    const list = messagesCache.value[sessionId] ?? []
    const newMessages = [...list, message]
    messagesCache.value[sessionId] = newMessages
  }

  /** 请求服务端生成/返回对话标题并更新（在第一次 AI 回复完成后调用），返回该轮 rag source ids */
  function fetchConversationTitle(sessionId: string): Promise<number[] | undefined> {
    return $get<{ title?: string; sourceIds?: number[] }>(`/chat/conversations?sessionId=${sessionId}`)
      .then((res) => {
        const title = res.data?.title ?? (res as { title?: string }).title
        if (title) updateTitle(sessionId, title)
        return res.data?.sourceIds
      })
      .catch((e) => { console.error(e); return undefined })
  }

  function updateLastMessage(sessionId: string, content: string) {
    const list = messagesCache.value[sessionId]
    if (!list?.length) return
    const last = list[list.length - 1]
    if (!last) return
    const next = [...list]
    next[next.length - 1] = { ...last, content: content || last.content }
    messagesCache.value[sessionId] = next
  }

  function updateTitle(sessionId: string, title: string) {
    conversations.value = conversations.value.map((c) =>
      c.id === sessionId ? { ...c, title } : c
    )
  }

  /** 截断指定会话从 fromIndex 开始的所有消息（用于重新生成 / 编辑后重发） */
  function truncateMessagesFrom(sessionId: string, fromIndex: number) {
    const list = messagesCache.value[sessionId]
    if (!list) return
    messagesCache.value[sessionId] = list.slice(0, fromIndex)
  }

  function patchMessage(sessionId: string, index: number, patch: Partial<MessageItem>) {
    const list = messagesCache.value[sessionId]
    if (!list || index < 0 || index >= list.length) return
    const next = [...list]
    next[index] = { ...next[index]!, ...patch }
    messagesCache.value[sessionId] = next
  }

  function removeConversation(sessionId: string) {
    conversations.value = conversations.value.filter((c) => c.id !== sessionId)
    delete messagesCache.value[sessionId]
    if (currentId.value === sessionId) {
      currentId.value = conversations.value[0]?.id ?? null
      try {
        if (currentId.value) localStorage.setItem(STORAGE_KEY_SESSION, currentId.value)
        else localStorage.removeItem(STORAGE_KEY_SESSION)
      } catch {}
    }
  }

  /** 从服务端会话列表覆盖本地对话列表（刷新页面时调用） */
  function setConversations(list: ConversationItem[]) {
    conversations.value = list
  }

  /** 刷新页面后用于恢复的 sessionId（从 localStorage 读取） */
  function getPersistedSessionId(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY_SESSION)
    } catch {
      return null
    }
  }

  return {
    conversations,
    currentId,
    currentConversation,
    currentMessages,
    addConversation,
    setCurrent,
    setMessages,
    appendMessage,
    updateLastMessage,
    updateTitle,
    fetchConversationTitle,
    truncateMessagesFrom,
    patchMessage,
    removeConversation,
    setConversations,
    getPersistedSessionId,
  }
})
