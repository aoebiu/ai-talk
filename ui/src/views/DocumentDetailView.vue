<template>
  <div class="doc-detail-page">
    <!-- 顶部导航栏 -->
    <header class="doc-detail-header">
      <button type="button" class="doc-back-btn" @click="goBack">&lt;</button>
      <h1 class="doc-breadcrumb">
        <span class="doc-breadcrumb-parent" @click="goBack">文档知识库</span>
        <span class="doc-breadcrumb-sep">/</span>
        <span v-if="kbName" class="doc-breadcrumb-parent" @click="goToKb">{{ kbName }}</span>
        <span v-if="kbName" class="doc-breadcrumb-sep">/</span>
        <span class="doc-breadcrumb-current">{{ doc?.originalName || '...' }}</span>
      </h1>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="doc-detail-loading">
      <span class="doc-spinner" />
      <span>加载中…</span>
    </div>

    <!-- 主体内容 -->
    <main v-else-if="doc" class="doc-detail-body">
      <div class="doc-detail-content">
        <!-- 基础信息 -->
        <section class="doc-section">
          <h2 class="doc-section-title">基础信息</h2>
          <div class="doc-info-grid">
            <div class="doc-info-item">
              <span class="doc-info-label">文件名称</span>
              <span class="doc-info-value">{{ doc.originalName }}</span>
            </div>
            <div class="doc-info-item">
              <span class="doc-info-label">文档类型</span>
              <span class="doc-info-value">{{ docTypeLabel(doc.docType) }}</span>
            </div>
            <div class="doc-info-item">
              <span class="doc-info-label">文件格式</span>
              <span class="doc-info-value">{{ doc.fileType }}</span>
            </div>
            <div class="doc-info-item">
              <span class="doc-info-label">文件大小</span>
              <span class="doc-info-value">{{ formatFileSize(doc.fileSize) }}</span>
            </div>
            <div class="doc-info-item">
              <span class="doc-info-label">上传时间</span>
              <span class="doc-info-value">{{ formatDate(doc.createdAt) }}</span>
            </div>
            <div class="doc-info-item">
              <span class="doc-info-label">当前状态</span>
              <span class="doc-status-badge" :class="docStatusClass(doc.status)">{{ docStatusLabel(doc.status) }}</span>
            </div>
          </div>
        </section>

        <!-- 知识库维度构建进度 -->
        <KbBuildProgress
          v-if="kbBuildTaskId || !isFinished()"
          title="知识库构建进度"
          :steps="kbBuildSteps"
          :task-status="kbBuildTaskStatus"
          :task-result="kbBuildTaskResult"
          :has-processing-docs="doc.status !== 'DONE' && doc.status !== 'FAILED'"
          :last-update-time="kbBuildLastUpdate"
        />

        <div v-if="doc.status === 'FAILED' && doc.errorMessage" class="doc-error-block">
          <span class="doc-error-label">错误信息：</span>
          <span class="doc-error-text">{{ doc.errorMessage }}</span>
        </div>

        <hr class="doc-divider" />

        <!-- 文档内容 -->
        <section class="doc-section">
          <div class="doc-section-header">
            <h2 class="doc-section-title">文档内容</h2>
          </div>
          <div v-if="contentLoading" class="doc-content-placeholder">文档内容加载中...</div>
          <div v-else-if="!canLoadContent()" class="doc-content-placeholder">
            文档处理完成后可查看内容
          </div>
          <div v-else-if="paragraphBlocks.length" class="doc-content-wrap">
            <div class="doc-content-meta">
              分块数：{{ segmentCount }} · 段落数：{{ paragraphBlocks.length }}
            </div>
            <div class="doc-content-scroll">
              <article
                v-for="(segment, idx) in paragraphBlocks"
                :key="`paragraph-${idx}`"
                class="doc-segment-item"
              >
                <header class="doc-segment-header">
                  <span class="doc-segment-index">第 {{ idx + 1 }} 段</span>
                </header>
                <p class="doc-segment-text">{{ segment }}</p>
              </article>
            </div>
          </div>
          <div v-else class="doc-content-placeholder">暂无可展示内容</div>
        </section>

        <!-- 底部操作栏 -->
        <div class="doc-detail-footer">
          <button type="button" class="btn-cancel" @click="goBack">返回列表</button>
        </div>
      </div>
    </main>

    <!-- 文档不存在 -->
    <div v-else class="doc-detail-loading">
      <span>文档不存在</span>
      <button type="button" class="btn-cancel" style="margin-top: 1rem;" @click="goBack">返回列表</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KbBuildProgress from '@/components/KbBuildProgress.vue'
import { getDocument, getDocumentContent } from '@/api/document'
import { getKnowledgeBase } from '@/api/knowledgeBase'
import type { DocumentInfoItem } from '@/api/document'
import { getTaskInfo } from '@/api/task'
import type { AsyncTaskStep } from '@/api/task'
import { KB_BUILD_DEFAULT_STEPS, normalizeTaskSteps } from '@/utils/buildProgress'

const route = useRoute()
const router = useRouter()

const documentId = Number(route.params.id)
const kbIdFromQuery = route.query.kbId ? Number(route.query.kbId) : null
const kbName = ref<string | null>(null)
const loading = ref(true)
const refreshing = ref(false)
const doc = ref<DocumentInfoItem | null>(null)
const autoRefresh = ref(true)
const contentLoading = ref(false)
const docSegments = ref<string[]>([])
const segmentCount = ref(0)
const paragraphBlocks = ref<string[]>([])

const kbBuildSteps = ref<AsyncTaskStep[]>([...KB_BUILD_DEFAULT_STEPS])
const kbBuildTaskId = ref<string | null>(null)
const kbBuildTaskStatus = ref<string | undefined>()
const kbBuildTaskResult = ref<string | null>(null)
const kbBuildLastUpdate = ref<string | null>(null)

let pollTimer: ReturnType<typeof setInterval> | null = null

function goBack() {
  router.push({ name: 'settings', query: { section: 'documents' } })
}

function goToKb() {
  const id = kbIdFromQuery ?? doc.value?.kbId
  if (id) {
    router.push({ name: 'kbDetail', params: { kbId: id } })
  } else {
    goBack()
  }
}

async function loadKbInfo(kbId: number) {
  try {
    const res = await getKnowledgeBase(kbId)
    if (res.success && res.data) {
      kbName.value = res.data.name
      kbBuildTaskId.value = res.data.buildTaskId ?? null
    }
  } catch {
    kbName.value = null
    kbBuildTaskId.value = null
  }
}

async function refreshKbBuildProgress() {
  const kbId = doc.value?.kbId ?? kbIdFromQuery
  if (!kbId) return
  if (!kbBuildTaskId.value) {
    await loadKbInfo(kbId)
  }
  if (!kbBuildTaskId.value) return

  try {
    const res = await getTaskInfo(kbBuildTaskId.value)
    if (res.success && res.data) {
      kbBuildTaskStatus.value = res.data.status
      kbBuildTaskResult.value = res.data.result
      if (res.data.steps) {
        const raw: AsyncTaskStep[] =
          typeof res.data.steps === 'string' ? JSON.parse(res.data.steps) : res.data.steps
        kbBuildSteps.value = normalizeTaskSteps(raw)
      }
      kbBuildLastUpdate.value = new Date().toLocaleString('zh-CN')
    }
  } catch {
    // ignore
  }
}

async function loadDoc() {
  try {
    const res = await getDocument(documentId)
    if (res.success && res.data) {
      doc.value = res.data
      const kbId = res.data.kbId ?? kbIdFromQuery
      if (kbId) {
        await loadKbInfo(kbId)
      }
    } else {
      doc.value = null
    }
  } catch {
    doc.value = null
  }
}

const canLoadContent = () => doc.value?.status === 'DONE'

async function loadDocumentContent() {
  if (!canLoadContent()) {
    docSegments.value = []
    paragraphBlocks.value = []
    segmentCount.value = 0
    return
  }
  contentLoading.value = true
  try {
    const res = await getDocumentContent(documentId)
    if (res.success && res.data) {
      const segments = Array.isArray(res.data.segments)
        ? res.data.segments.filter((s) => !!s && !!s.trim())
        : (res.data.content ?? '')
          .split('\n\n')
          .map((s) => s.trim())
          .filter((s) => !!s)
      docSegments.value = segments
      paragraphBlocks.value = segments
        .flatMap((chunk) =>
          chunk
            .split(/\n\s*\n+/)
            .map((paragraph) => paragraph.trim())
            .filter((paragraph) => !!paragraph),
        )
      segmentCount.value = res.data.segmentCount ?? segments.length
      return
    }
    docSegments.value = []
    paragraphBlocks.value = []
    segmentCount.value = 0
  } catch {
    docSegments.value = []
    paragraphBlocks.value = []
    segmentCount.value = 0
  } finally {
    contentLoading.value = false
  }
}


async function refreshStatus() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    await loadDoc()
    await refreshKbBuildProgress()
    await loadDocumentContent()
  } finally {
    refreshing.value = false
  }
}

function isFinished() {
  const s = doc.value?.status
  return s === 'DONE' || s === 'FAILED'
}

function startPoll() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    if (!autoRefresh.value) return
    if (isFinished()) {
      stopPoll()
      return
    }
    await refreshStatus()
  }, 2000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(autoRefresh, (enabled) => {
  if (enabled && !isFinished()) {
    startPoll()
  } else if (!enabled) {
    stopPoll()
  }
})

onMounted(async () => {
  await loadDoc()
  await refreshKbBuildProgress()
  await loadDocumentContent()
  loading.value = false
  if (!isFinished()) {
    startPoll()
  }
})

onBeforeUnmount(() => {
  stopPoll()
})

function docStatusLabel(status: string): string {
  switch (status) {
    case 'PENDING': return '待处理'
    case 'PARSING': return '解析中'
    case 'CLEANING': return '清洗中'
    case 'CHUNKING': return '分块中'
    case 'EMBEDDING': return '向量化中'
    case 'DONE': return '已完成'
    case 'FAILED': return '处理失败'
    default: return status
  }
}

function docStatusClass(status: string): string {
  switch (status) {
    case 'DONE': return 'status-done'
    case 'FAILED': return 'status-failed'
    case 'PENDING': return 'status-pending'
    default: return 'status-processing'
  }
}

function docTypeLabel(docType: string): string {
  switch (docType) {
    case 'default': return '通用文档'
    case 'short_text': return '短文本'
    case 'paper': return '论文'
    case 'contract': return '合同'
    case 'novel': return '小说'
    default: return docType
  }
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(s: string | null): string {
  if (!s) return '-'
  try {
    const d = new Date(s)
    return d.toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    })
  } catch {
    return s
  }
}
</script>

<style scoped>
.doc-detail-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

/* 顶部导航 */
.doc-detail-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 2rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.doc-back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.doc-back-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}

.doc-breadcrumb {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.doc-breadcrumb-parent {
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.doc-breadcrumb-parent:hover {
  color: var(--color-text-accent);
}

.doc-breadcrumb-sep {
  color: var(--color-text-tertiary);
  font-weight: 400;
}

.doc-breadcrumb-current {
  color: var(--color-text-primary);
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 加载状态 */
.doc-detail-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.doc-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-text-accent);
  border-radius: 50%;
  animation: doc-spin 0.7s linear infinite;
}

@keyframes doc-spin {
  to { transform: rotate(360deg); }
}

/* 主体：滚动由外层 settings-body 承担，避免双滚动条 */
.doc-detail-body {
  flex: 1;
  min-height: 0;
}

.doc-detail-content {
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 1.5rem 2rem 3rem;
}

/* 区块 */
.doc-section {
  margin-bottom: 1.5rem;
}

.doc-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.doc-section-header .doc-section-title {
  margin-bottom: 0;
}

.doc-section-title {
  margin: 0 0 1rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.doc-section-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* 分割线 */
.doc-divider {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0.5rem 0 1.5rem;
}

/* 基础信息网格 */
.doc-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem 1.5rem;
}

.doc-info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.doc-info-label {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  font-weight: 500;
}

.doc-info-value {
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  word-break: break-all;
}

/* 状态徽章 */
.doc-status-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  width: fit-content;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-done {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.status-failed {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error, #ef4444);
}

.status-pending {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.status-processing {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

/* 错误信息块 */
.doc-error-block {
  margin-top: 0.75rem;
  padding: 0.75rem 1rem;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  font-size: 0.875rem;
  line-height: 1.5;
}

.doc-error-label {
  font-weight: 600;
  color: var(--color-error, #ef4444);
}

.doc-error-text {
  color: var(--color-text-secondary);
}

.doc-content-wrap {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-input);
  padding: 0.85rem 0.9rem;
}

.doc-content-meta {
  margin-bottom: 0.65rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.doc-content-scroll {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 0.3rem;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}

.doc-content-scroll:hover {
  scrollbar-color: var(--color-border-hover) transparent;
}

.doc-content-scroll::-webkit-scrollbar {
  width: 4px;
}

.doc-content-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.doc-content-scroll::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 999px;
}

.doc-content-scroll:hover::-webkit-scrollbar-thumb {
  background: var(--color-border-hover);
}

.doc-content-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--color-border-focus);
}

.doc-segment-item {
  padding: 0.75rem 0.8rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-page);
}

.doc-segment-item + .doc-segment-item {
  margin-top: 0.7rem;
}

.doc-segment-header {
  margin-bottom: 0.45rem;
}

.doc-segment-index {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  background: color-mix(in srgb, var(--color-border) 30%, transparent);
}

.doc-segment-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.9rem;
  color: var(--color-text-secondary);
  line-height: 1.75;
}

.doc-content-placeholder {
  padding: 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
}

/* 按钮 */
.btn-secondary {
  padding: 0.25rem 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  color: var(--color-primary, #7c3aed);
  border-color: var(--color-primary, #7c3aed);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 底部操作栏 */
.doc-detail-footer {
  display: flex;
  align-items: center;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-border);
  margin-top: 1rem;
}

.btn-cancel {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--color-text-primary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
}

.btn-cancel:hover {
  border-color: var(--color-border-focus);
}
</style>
