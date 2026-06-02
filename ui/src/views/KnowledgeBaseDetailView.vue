<template>
  <div class="kb-detail-page">
    <div v-if="loadingKb" class="kb-detail-loading">
      <span class="kb-spinner" />
      <span>加载中…</span>
    </div>

    <template v-else-if="kb">
      <!-- 页头 -->
      <header class="kb-page-header">
        <div class="kb-page-header-main">
          <button type="button" class="kb-back-btn" aria-label="返回" @click="goToKbList">←</button>
          <div class="kb-page-header-text">
            <nav class="kb-breadcrumb" aria-label="面包屑">
              <button type="button" class="kb-breadcrumb-parent" @click="goToKbList">文档知识库</button>
              <span class="kb-breadcrumb-sep">/</span>
              <span class="kb-breadcrumb-current">{{ kb.name }}</span>
            </nav>
            <p v-if="kb.description" class="kb-page-desc">{{ kb.description }}</p>
          </div>
        </div>
        <div class="kb-page-header-actions">
          <button type="button" class="primary-action-btn" @click="goToUpload">上传文档</button>
          <button type="button" class="refresh-btn" :disabled="loadingDocs" @click="loadDocuments">
            {{ loadingDocs ? '刷新中…' : '刷新' }}
          </button>
        </div>
      </header>

      <!-- 标签 -->
      <nav class="kb-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          class="kb-tab"
          :class="{ active: activeTab === 'info' }"
          :aria-selected="activeTab === 'info'"
          @click="setTab('info')"
        >
          基础信息
        </button>
        <button
          type="button"
          role="tab"
          class="kb-tab"
          :class="{ active: activeTab === 'docs' }"
          :aria-selected="activeTab === 'docs'"
          @click="setTab('docs')"
        >
          文档列表
        </button>
      </nav>

      <main class="kb-detail-body">
        <!-- 文档列表（默认） -->
        <section v-show="activeTab === 'docs'" class="kb-tab-panel">
          <div class="data-table-card">
            <div class="data-table-panel" :class="{ 'is-loading': loadingDocs }">
              <div v-if="loadingDocs" class="data-table-loading-overlay">
                <span class="kb-spinner" />
              </div>
              <div v-else-if="documentList.length > 0" class="table-scroll-x">
                <div class="doc-table">
                  <div class="doc-table-header">
                    <span class="col-name">文档名称</span>
                    <span class="col-type">类型</span>
                    <span class="col-size">大小</span>
                    <span class="col-chunks">分块</span>
                    <span class="col-status">状态</span>
                    <span class="col-time">上传时间</span>
                    <span class="col-actions">操作</span>
                  </div>
                  <div v-for="doc in documentList" :key="doc.id" class="doc-table-row">
                    <div class="col-name">
                      <button
                        type="button"
                        class="doc-name-link"
                        :title="doc.originalName"
                        @click="goToDocumentDetail(doc)"
                      >
                        <span class="doc-file-icon" aria-hidden="true">📄</span>
                        <span class="doc-name-text">{{ doc.originalName }}</span>
                      </button>
                      <span class="doc-file-meta">{{ doc.fileType }} · {{ docTypeLabel(doc.docType) }}</span>
                    </div>
                    <span class="col-type">{{ docTypeLabel(doc.docType) }}</span>
                    <span class="col-size">{{ formatFileSize(doc.fileSize) }}</span>
                    <span class="col-chunks">
                      <template v-if="doc.status === 'DONE'">{{ doc.totalChunks }}</template>
                      <template v-else-if="doc.totalChunks != null">
                        {{ doc.processedChunks }}/{{ doc.totalChunks }}
                      </template>
                      <template v-else>-</template>
                    </span>
                    <span class="col-status">
                      <span class="doc-status-badge" :class="docStatusClass(doc.status)">
                        {{ docStatusLabel(doc.status) }}
                      </span>
                      <span
                        v-if="doc.status !== 'DONE' && doc.status !== 'FAILED'"
                        class="doc-progress-text"
                      >
                        {{ doc.progress }}%
                      </span>
                    </span>
                    <span class="col-time">{{ formatDate(doc.createdAt) }}</span>
                    <div class="col-actions">
                      <button type="button" class="edit-btn" @click="goToDocumentDetail(doc)">详情</button>
                      <button type="button" class="delete-btn" @click="confirmDelete(doc)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="table-empty">
                <p>暂无文档</p>
                <button type="button" class="primary-action-btn" @click="goToUpload">上传文档</button>
              </div>
            </div>
          </div>
        </section>

        <!-- 基础信息 -->
        <section v-show="activeTab === 'info'" class="kb-tab-panel kb-tab-panel-info">
          <div class="kb-info-cards">
            <div class="kb-info-card">
              <h2 class="kb-card-title">基本信息</h2>
              <dl class="kb-dl">
                <div class="kb-dl-row">
                  <dt>知识库名称</dt>
                  <dd>{{ kb.name }}</dd>
                </div>
                <div class="kb-dl-row">
                  <dt>知识库 ID</dt>
                  <dd class="kb-info-mono">{{ kb.indexName }}</dd>
                </div>
                <div class="kb-dl-row">
                  <dt>可见范围</dt>
                  <dd>{{ kb.visibility === 'public' ? '公开（所有人可见）' : '私有（仅自己可见）' }}</dd>
                </div>
                <div class="kb-dl-row">
                  <dt>文档数量</dt>
                  <dd>{{ kb.documentCount ?? documentList.length }}</dd>
                </div>
                <div class="kb-dl-row kb-dl-row-full">
                  <dt>知识库描述</dt>
                  <dd>{{ kb.description || '—' }}</dd>
                </div>
                <div class="kb-dl-row">
                  <dt>创建时间</dt>
                  <dd>{{ formatDate(kb.createdAt) }}</dd>
                </div>
              </dl>
            </div>

            <div v-if="kb.buildTaskId || hasProcessingDocs" class="kb-info-card kb-info-card-build">
              <h2 class="kb-card-title">构建进度</h2>
              <p class="kb-card-desc">{{ buildOverall.message }}</p>
              <KbBuildProgress
                :steps="buildSteps"
                :task-status="buildTaskStatus"
                :task-result="buildTaskResult"
                :has-processing-docs="hasProcessingDocs"
                :last-update-time="buildLastUpdate"
                class="kb-build-inline"
              />
            </div>
          </div>
        </section>
      </main>
    </template>

    <div v-else class="kb-detail-loading">
      <span>知识库不存在</span>
      <button type="button" class="refresh-btn" style="margin-top: 1rem;" @click="goToKbList">返回列表</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KbBuildProgress from '@/components/KbBuildProgress.vue'
import { getTaskInfo } from '@/api/task'
import type { AsyncTaskStep } from '@/api/task'
import type { KnowledgeBaseItem } from '@/api/knowledgeBase'
import { getKnowledgeBase } from '@/api/knowledgeBase'
import type { DocumentInfoItem } from '@/api/document'
import { getDocumentList, deleteDocument } from '@/api/document'
import {
  KB_BUILD_DEFAULT_STEPS,
  normalizeTaskSteps,
  resolveBuildOverallStatus,
} from '@/utils/buildProgress'
import type { BuildTaskStep } from '@/utils/buildProgress'
import { toastError } from '@/utils/toast'

type KbTab = 'info' | 'docs'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.kbId)

const activeTab = ref<KbTab>((route.query.tab as KbTab) === 'docs' ? 'docs' : 'info')

const kb = ref<KnowledgeBaseItem | null>(null)
const loadingKb = ref(true)
const documentList = ref<DocumentInfoItem[]>([])
const loadingDocs = ref(false)

const buildSteps = ref<BuildTaskStep[]>([...KB_BUILD_DEFAULT_STEPS])
const buildTaskStatus = ref<string | undefined>()
const buildTaskResult = ref<string | null>(null)
const buildLastUpdate = ref<string | null>(null)

let buildPollTimer: ReturnType<typeof setInterval> | null = null
let pageLoading = false

const hasProcessingDocs = computed(() =>
  documentList.value.some((d) => d.status !== 'DONE' && d.status !== 'FAILED'),
)

const buildOverall = computed(() =>
  resolveBuildOverallStatus(buildTaskStatus.value, buildTaskResult.value, hasProcessingDocs.value),
)

function isBuildActive() {
  return (
    hasProcessingDocs.value
    || buildTaskStatus.value === 'RUNNING'
    || buildTaskStatus.value === 'PENDING'
  )
}

function setTab(tab: KbTab) {
  activeTab.value = tab
  router.replace({ query: { ...route.query, tab: tab === 'info' ? undefined : tab } })
}

function goToKbList() {
  router.push({ name: 'settings', query: { section: 'documents' } })
}

function goToUpload() {
  router.push({ name: 'kbUpload', params: { kbId } })
}

function goToDocumentDetail(doc: DocumentInfoItem) {
  router.push({
    name: 'documentDetail',
    params: { id: doc.id },
    query: { kbId: String(kbId) },
  })
}

async function loadKb(silent = false) {
  if (!silent) loadingKb.value = true
  try {
    const res = await getKnowledgeBase(kbId)
    if (res.success && res.data) {
      if (res.data.status === 'DRAFT') {
        router.replace({ name: 'kbCreate', query: { resumeKbId: String(kbId) } })
        return
      }
      kb.value = res.data
    } else {
      kb.value = null
    }
  } catch {
    kb.value = null
  } finally {
    if (!silent) loadingKb.value = false
  }
}

async function loadDocuments() {
  loadingDocs.value = true
  try {
    const res = await getDocumentList(kbId)
    if (res.success && Array.isArray(res.data)) {
      documentList.value = res.data
    }
  } catch {
    // ignore
  } finally {
    loadingDocs.value = false
  }
}

async function refreshBuildProgress() {
  if (!kb.value?.buildTaskId) {
    await loadKb(true)
  }
  const taskId = kb.value?.buildTaskId
  if (!taskId) return

  try {
    const res = await getTaskInfo(taskId)
    if (res.success && res.data) {
      buildTaskStatus.value = res.data.status
      buildTaskResult.value = res.data.result
      if (res.data.steps) {
        const raw: AsyncTaskStep[] =
          typeof res.data.steps === 'string' ? JSON.parse(res.data.steps) : res.data.steps
        buildSteps.value = normalizeTaskSteps(raw)
      }
      buildLastUpdate.value = new Date().toLocaleString('zh-CN')
    }
  } catch {
    // ignore
  }
}

function startBuildPoll() {
  if (buildPollTimer) return
  buildPollTimer = setInterval(async () => {
    await loadDocuments()
    await refreshBuildProgress()
    if (!isBuildActive()) stopBuildPoll()
  }, 2000)
}

function stopBuildPoll() {
  if (buildPollTimer) {
    clearInterval(buildPollTimer)
    buildPollTimer = null
  }
}

async function loadPageData() {
  if (pageLoading) return
  pageLoading = true
  loadingKb.value = true
  try {
    await loadKb(true)
    if (!kb.value) {
      loadingKb.value = false
      return
    }
    await Promise.all([loadDocuments(), refreshBuildProgress()])
    if (isBuildActive()) {
      startBuildPoll()
    }
  } finally {
    loadingKb.value = false
    pageLoading = false
  }
}

function confirmDelete(doc: DocumentInfoItem) {
  if (!window.confirm(`确定要删除文档「${doc.originalName}」吗？`)) return
  deleteDocumentById(doc.id)
}

async function deleteDocumentById(id: number) {
  try {
    const res = await deleteDocument(id)
    if (res.success) {
      documentList.value = documentList.value.filter((d) => d.id !== id)
      await refreshBuildProgress()
      if (kb.value) {
        kb.value.documentCount = Math.max(0, (kb.value.documentCount ?? 1) - 1)
      }
    } else {
      toastError(res.message || '删除失败')
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '删除失败'
    toastError(msg)
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = tab === 'docs' ? 'docs' : 'info'
  },
)

onMounted(() => {
  if (!kbId || Number.isNaN(kbId)) {
    goToKbList()
    return
  }
  loadPageData()
})

onBeforeUnmount(() => {
  stopBuildPoll()
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
    case 'DONE': return 'doc-status-done'
    case 'FAILED': return 'doc-status-failed'
    case 'PENDING': return 'doc-status-pending'
    default: return 'doc-status-processing'
  }
}

function docTypeLabel(docType: string): string {
  switch (docType) {
    case 'default': return '通用'
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

function formatDate(value: string | null | undefined): string {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.kb-detail-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
  min-height: 0;
  background: var(--color-bg-page);
}

/* 页头 */
.kb-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.25rem 2rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  flex-shrink: 0;
}

.kb-page-header-main {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  min-width: 0;
  flex: 1;
}

.kb-back-btn {
  flex-shrink: 0;
  width: 2.25rem;
  height: 2.25rem;
  margin-top: 0.125rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  transition: all 0.2s ease;
}

.kb-back-btn:hover {
  border-color: var(--color-border-focus);
  color: var(--color-text-primary);
}

.kb-page-header-text {
  min-width: 0;
}

.kb-breadcrumb {
  margin: 0 0 0.25rem;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.25rem;
  font-size: 1.125rem;
  font-weight: 600;
}

.kb-breadcrumb-parent {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-text-secondary);
  font-size: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
}

.kb-breadcrumb-parent:hover {
  color: var(--color-text-accent);
}

.kb-breadcrumb-sep {
  color: var(--color-text-tertiary);
  font-weight: 400;
}

.kb-breadcrumb-current {
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-page-desc {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kb-page-header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

/* 标签 */
.kb-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0 2rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  flex-shrink: 0;
}

.kb-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.kb-tab:hover {
  color: var(--color-text-primary);
}

.kb-tab.active {
  color: var(--color-button-primary);
  border-bottom-color: var(--color-button-primary);
}

/* 内容区 */
.kb-detail-body {
  flex: 1;
  min-height: 0;
  padding: 1.5rem 2rem 2rem;
  overflow-y: auto;
}

.kb-tab-panel {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 文档表格 */
.data-table-card {
  width: 100%;
}

.data-table-panel {
  position: relative;
  min-height: 200px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-card);
  overflow: hidden;
}

.data-table-panel.is-loading {
  min-height: 240px;
}

.data-table-loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  z-index: 1;
}

[data-theme='dark'] .data-table-loading-overlay {
  background: rgba(15, 15, 18, 0.7);
}

.table-scroll-x {
  overflow-x: auto;
}

.doc-table {
  min-width: 880px;
}

.doc-table-header,
.doc-table-row {
  display: grid;
  grid-template-columns: minmax(200px, 2.2fr) 72px 80px 72px 100px 150px 100px;
  gap: 0.75rem 1rem;
  align-items: center;
  padding: 0.75rem 1.25rem;
}

.doc-table-header {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
}

.doc-table-row {
  font-size: 0.875rem;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.doc-table-row:last-child {
  border-bottom: none;
}

.doc-table-row:hover {
  background: var(--color-bg-input);
}

.col-name {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.doc-name-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  border: none;
  background: none;
  color: var(--color-text-primary);
  cursor: pointer;
  font-size: inherit;
  font-weight: 500;
  text-align: left;
  min-width: 0;
}

.doc-name-link:hover .doc-name-text {
  color: var(--color-text-accent);
}

.doc-file-icon {
  flex-shrink: 0;
  font-size: 1rem;
  line-height: 1;
}

.doc-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s ease;
}

.doc-file-meta {
  display: none;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  padding-left: 1.5rem;
}

.col-type,
.col-size,
.col-chunks,
.col-time {
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.col-status {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  align-items: flex-start;
}

.doc-status-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.doc-status-done { background: rgba(34, 197, 94, 0.12); color: #22c55e; }
.doc-status-failed { background: rgba(239, 68, 68, 0.12); color: #ef4444; }
.doc-status-pending { background: rgba(107, 114, 128, 0.12); color: #6b7280; }
.doc-status-processing { background: rgba(59, 130, 246, 0.12); color: #3b82f6; }

.doc-progress-text {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
}

.col-actions {
  display: flex;
  gap: 0.375rem;
}

.edit-btn,
.delete-btn {
  padding: 0.3rem 0.5rem;
  font-size: 0.8125rem;
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 4px;
}

.edit-btn {
  color: var(--color-text-secondary);
}

.edit-btn:hover {
  color: var(--color-button-primary);
  background: var(--color-bg-input);
}

.delete-btn {
  color: var(--color-error);
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.08);
}

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 3.5rem 1rem;
  text-align: center;
}

.table-empty p {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: 0.875rem;
}

/* 基础信息 */
.kb-tab-panel-info {
  max-width: 960px;
}

.kb-info-cards {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.kb-info-card {
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.kb-card-title {
  margin: 0 0 1rem;
  font-size: 0.9375rem;
  font-weight: 600;
}

.kb-card-desc {
  margin: -0.5rem 0 0.75rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.kb-dl {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 1.5rem;
}

.kb-dl-row {
  display: grid;
  grid-template-columns: 7rem 1fr;
  gap: 0.75rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border);
}

.kb-dl-row-full {
  grid-column: 1 / -1;
}

.kb-dl-row dt {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.kb-dl-row dd {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  word-break: break-word;
}

.kb-info-mono {
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
}

.kb-build-inline :deep(.kb-build-section) {
  margin-bottom: 0;
}

.kb-build-inline :deep(.kb-build-header) {
  margin-bottom: 0.5rem;
}

.kb-build-inline :deep(.kb-build-title) {
  display: none;
}

/* 按钮 */
.primary-action-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
}

.primary-action-btn:hover {
  background: var(--color-button-primary-hover);
}

.refresh-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
}

.refresh-btn:hover:not(:disabled) {
  border-color: var(--color-border-focus);
  color: var(--color-text-primary);
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.kb-detail-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 4rem 0;
  color: var(--color-text-secondary);
}

.kb-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-button-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 960px) {
  .kb-page-header {
    flex-direction: column;
    padding: 1rem 1.25rem;
  }

  .kb-tabs,
  .kb-detail-body {
    padding-left: 1.25rem;
    padding-right: 1.25rem;
  }

  .kb-dl {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .doc-table-header .col-type,
  .doc-table-row .col-type,
  .doc-table-header .col-size,
  .doc-table-row .col-size {
    display: none;
  }

  .doc-table-header,
  .doc-table-row {
    grid-template-columns: minmax(140px, 1fr) 64px 96px 88px;
  }

  .doc-file-meta {
    display: block;
  }
}
</style>
