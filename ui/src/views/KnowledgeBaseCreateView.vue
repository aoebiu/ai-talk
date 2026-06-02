<template>
  <div class="kb-create-page">
    <header class="kb-header">
      <button type="button" class="kb-back-btn" @click="handleBack">&lt;</button>
      <h1 class="kb-breadcrumb">
        <span class="kb-breadcrumb-parent" @click="handleBreadcrumbBack">文档知识库</span>
        <span class="kb-breadcrumb-sep">/</span>
        <template v-if="isUploadMode">
          <span class="kb-breadcrumb-parent" @click="goToKbDetail">{{ kbName || '知识库' }}</span>
          <span class="kb-breadcrumb-sep">/</span>
          <span class="kb-breadcrumb-current">上传文档</span>
        </template>
        <span v-else class="kb-breadcrumb-current">新建知识库</span>
      </h1>
    </header>

    <main class="kb-body">
      <div class="kb-content" :class="{ 'kb-content--wide': step === 3 }">
        <!-- 步骤指示器 -->
        <div class="kb-steps">
          <template v-if="isUploadMode">
            <div class="kb-step" :class="{ active: step === 2, done: step > 2 }">
              <div class="step-circle">{{ step > 2 ? '✓' : '1' }}</div>
              <span class="step-label">上传文件</span>
            </div>
            <div class="step-line" :class="{ done: step > 2 }" />
            <div class="kb-step" :class="{ active: step === 3 }">
              <div class="step-circle">2</div>
              <span class="step-label">分块预览</span>
            </div>
          </template>
          <template v-else>
            <div class="kb-step" :class="{ active: step === 1, done: step > 1 }">
              <div class="step-circle">{{ step > 1 ? '✓' : '1' }}</div>
              <span class="step-label">基本信息</span>
            </div>
            <div class="step-line" :class="{ done: step > 1 }" />
            <div class="kb-step" :class="{ active: step === 2, done: step > 2 }">
              <div class="step-circle">{{ step > 2 ? '✓' : '2' }}</div>
              <span class="step-label">上传文件</span>
            </div>
            <div class="step-line" :class="{ done: step > 2 }" />
            <div class="kb-step" :class="{ active: step === 3 }">
              <div class="step-circle">3</div>
              <span class="step-label">分块预览</span>
            </div>
          </template>
        </div>

        <!-- 步骤一：基本信息（仅新建知识库） -->
        <section v-if="!isUploadMode && step === 1" class="kb-section">
          <h2 class="kb-section-title">知识库基本信息</h2>
          <div class="kb-form">
            <div class="form-field">
              <label class="form-label">知识库名称 <span class="required">*</span></label>
              <input
                v-model="form.name"
                type="text"
                class="form-input"
                placeholder="请输入知识库名称"
                maxlength="50"
              />
            </div>
            <div class="form-field">
              <label class="form-label">知识库描述</label>
              <textarea
                v-model="form.description"
                class="form-textarea"
                placeholder="请输入知识库描述（选填）"
                rows="4"
                maxlength="200"
              />
            </div>
            <div class="form-field">
              <label class="form-label">可见范围</label>
              <div class="radio-group">
                <label class="radio-option" :class="{ selected: form.visibility === 'private' }">
                  <input v-model="form.visibility" type="radio" value="private" />
                  <span class="radio-label">私有</span>
                  <span class="radio-desc">仅自己可见</span>
                </label>
                <label class="radio-option" :class="{ selected: form.visibility === 'public' }">
                  <input v-model="form.visibility" type="radio" value="public" />
                  <span class="radio-label">公开</span>
                  <span class="radio-desc">所有人可见</span>
                </label>
              </div>
            </div>
            <p v-if="step1Error" class="form-error">{{ step1Error }}</p>
            <div class="form-actions">
              <button type="button" class="btn-secondary" @click="cancelWizard">取消</button>
              <button type="button" class="btn-primary" :disabled="creatingKb" @click="goToStep2">
                {{ creatingKb ? '创建中…' : '下一步' }}
              </button>
            </div>
          </div>
        </section>

        <!-- 步骤二：上传文件 -->
        <section v-else-if="step === 2" class="kb-section">
          <h2 class="kb-section-title">上传文件</h2>
          <p class="kb-section-desc">支持 PDF、Word、TXT、Markdown 格式，可同时上传多个文件。</p>

          <div
            class="upload-zone"
            :class="{ 'drag-over': isDragging }"
            @click="triggerFileInput"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="onDrop"
          >
            <div class="upload-zone-icon">📂</div>
            <p class="upload-zone-text">点击或拖拽文件到此处上传</p>
            <p class="upload-zone-hint">支持 .pdf .doc .docx .txt .md</p>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            class="file-input-hidden"
            accept=".pdf,.doc,.docx,.txt,.md"
            multiple
            @change="onFileChange"
          />

          <!-- 待上传文件列表 -->
          <div v-if="pendingFiles.length > 0" class="upload-file-list">
            <div
              v-for="item in pendingFiles"
              :key="item.name"
              class="upload-file-item"
            >
              <span class="file-name" :title="item.name">{{ item.name }}</span>
              <span class="file-size">{{ formatFileSize(item.size) }}</span>
              <span class="file-status" :class="item.status">
                <span v-if="item.status === 'pending'">待上传</span>
                <span v-else-if="item.status === 'uploading'">上传中…</span>
                <span v-else-if="item.status === 'done'">已完成</span>
                <span v-else-if="item.status === 'error'" :title="item.error">失败</span>
              </span>
              <button
                v-if="item.status === 'pending'"
                type="button"
                class="file-remove-btn"
                @click="removeFile(item.name)"
              >
                ×
              </button>
            </div>
          </div>

          <!-- 文档类型 -->
          <div class="config-block">
            <div class="config-block-title">文档类型</div>
            <div class="type-radio-group">
              <label
                v-for="opt in typeOptions"
                :key="opt.value"
                class="type-radio-option"
                :class="{ selected: documentType === opt.value }"
              >
                <input v-model="documentType" type="radio" :value="opt.value" />
                <span class="type-radio-label">{{ opt.label }}</span>
                <span class="type-radio-desc">{{ opt.desc }}</span>
              </label>
            </div>
          </div>

          <!-- 清洗配置 -->
          <div class="config-block">
            <button type="button" class="config-block-toggle" @click="showCleaning = !showCleaning">
              <span class="config-block-title">清洗配置</span>
              <span class="toggle-arrow" :class="{ open: showCleaning }">›</span>
            </button>
            <div v-if="showCleaning" class="cleaning-options">
              <label class="cleaning-option">
                <input v-model="cleaningConfig.normalizeWhitespace" type="checkbox" />
                <div class="cleaning-option-text">
                  <span class="cleaning-option-label">空白规范化</span>
                  <span class="cleaning-option-desc">合并连续空格/Tab，压缩多余换行，去除行首尾空白</span>
                </div>
              </label>
              <label class="cleaning-option">
                <input v-model="cleaningConfig.mergeLineBreaks" type="checkbox" />
                <div class="cleaning-option-text">
                  <span class="cleaning-option-label">断行合并</span>
                  <span class="cleaning-option-desc">将单个换行合并为空格，保留双换行作为段落边界（适合 PDF）</span>
                </div>
              </label>
              <label class="cleaning-option">
                <input v-model="cleaningConfig.filterLowValueParagraphs" type="checkbox" />
                <div class="cleaning-option-text">
                  <span class="cleaning-option-label">低价值段落过滤</span>
                  <span class="cleaning-option-desc">丢弃极短段落及纯符号/数字段落</span>
                </div>
              </label>
              <div v-if="cleaningConfig.filterLowValueParagraphs" class="cleaning-suboption">
                <label class="cleaning-suboption-label">最小段落长度</label>
                <input
                  v-model.number="cleaningConfig.minParagraphLength"
                  type="number"
                  class="cleaning-number-input"
                  min="1"
                  max="500"
                />
                <span class="cleaning-suboption-unit">字符</span>
              </div>
              <label class="cleaning-option">
                <input v-model="cleaningConfig.deduplicateParagraphs" type="checkbox" />
                <div class="cleaning-option-text">
                  <span class="cleaning-option-label">重复段落去重</span>
                  <span class="cleaning-option-desc">移除内容完全相同的重复段落（常见于模板文档）</span>
                </div>
              </label>
            </div>
          </div>

          <p v-if="uploadError" class="form-error">{{ uploadError }}</p>

          <div class="form-actions">
            <button v-if="isUploadMode" type="button" class="btn-secondary" @click="goToKbDetail">取消</button>
            <button v-else type="button" class="btn-secondary" @click="step = 1">上一步</button>
            <button
              type="button"
              class="btn-primary"
              :disabled="pendingFiles.length === 0 || uploading"
              @click="startUpload"
            >
              {{ uploading ? '上传中…' : '开始上传' }}
            </button>
          </div>
        </section>

        <!-- 步骤三：构建进度 + 分块预览 -->
        <section v-else-if="step === 3" class="kb-section kb-section-preview">
          <KbBuildProgress
            v-if="buildTaskId || hasProcessingPreviewDocs"
            :steps="buildSteps"
            :task-status="buildTaskStatus"
            :task-result="buildTaskResult"
            :has-processing-docs="hasProcessingPreviewDocs"
            :last-update-time="buildLastUpdate"
          />

          <div class="preview-section-head">
            <h2 class="kb-section-title preview-title">分块预览</h2>
            <p class="kb-section-desc preview-desc">文档处理完成后，可按分块切换查看拆分结果。</p>
          </div>

          <div class="preview-docs">
            <div v-for="doc in uploadedDocs" :key="doc.documentId" class="preview-doc-item">
              <div class="preview-doc-header">
                <div class="preview-doc-title-wrap">
                  <span class="preview-doc-icon" aria-hidden="true">📄</span>
                  <span class="preview-doc-name" :title="doc.originalName">{{ doc.originalName }}</span>
                </div>
                <span class="preview-doc-status" :class="previewStatusClass(doc.status)">
                  {{ previewStatusLabel(doc.status) }}
                </span>
              </div>

              <!-- 处理中 -->
              <div v-if="!isPreviewDone(doc.status) && !isPreviewFailed(doc.status)" class="preview-processing">
                <span class="preview-spinner" />
                <span class="preview-processing-text">处理中，请稍候…</span>
              </div>

              <!-- 失败 -->
              <div v-else-if="isPreviewFailed(doc.status)" class="preview-error">
                {{ doc.error || '处理失败' }}
              </div>

              <!-- 分块内容 -->
              <div v-else-if="doc.contentLoaded && doc.segments.length > 0" class="preview-segments">
                <div class="preview-segments-toolbar">
                  <span class="preview-segments-meta">共 {{ doc.segmentCount }} 个分块</span>
                  <div class="preview-segment-tabs" role="tablist">
                    <button
                      v-for="(seg, idx) in doc.segments"
                      :key="idx"
                      type="button"
                      role="tab"
                      class="preview-segment-tab"
                      :class="{ active: getActiveSegment(doc.documentId) === idx }"
                      :aria-selected="getActiveSegment(doc.documentId) === idx"
                      @click="setActiveSegment(doc.documentId, idx)"
                    >
                      {{ idx + 1 }}
                    </button>
                  </div>
                </div>
                <article class="preview-segment-panel">
                  <header class="preview-segment-header">
                    <span class="preview-segment-index">
                      第 {{ getActiveSegment(doc.documentId) + 1 }} / {{ doc.segments.length }} 块
                    </span>
                    <span class="preview-segment-len">
                      {{ doc.segments[getActiveSegment(doc.documentId)]?.length ?? 0 }} 字
                    </span>
                  </header>
                  <p class="preview-segment-text">{{ doc.segments[getActiveSegment(doc.documentId)] }}</p>
                </article>
                <div v-if="doc.segments.length > 1" class="preview-segment-nav">
                  <button
                    type="button"
                    class="preview-nav-btn"
                    :disabled="getActiveSegment(doc.documentId) <= 0"
                    @click="shiftActiveSegment(doc.documentId, -1)"
                  >
                    上一块
                  </button>
                  <button
                    type="button"
                    class="preview-nav-btn"
                    :disabled="getActiveSegment(doc.documentId) >= doc.segments.length - 1"
                    @click="shiftActiveSegment(doc.documentId, 1)"
                  >
                    下一块
                  </button>
                </div>
              </div>

              <div v-else-if="doc.contentLoaded" class="preview-empty">暂无可展示内容</div>
            </div>
          </div>

          <div class="form-actions preview-actions">
            <button
              v-if="!isUploadMode"
              type="button"
              class="btn-secondary"
              @click="cancelWizard"
            >
              放弃
            </button>
            <button
              type="button"
              class="btn-primary"
              :disabled="finishingCreate"
              @click="finishCreate"
            >
              {{ finishingCreate ? '提交中…' : (isUploadMode ? '完成' : '完成创建') }}
            </button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import KbBuildProgress from '@/components/KbBuildProgress.vue'
import { getKnowledgeBase } from '@/api/knowledgeBase'
import { getTaskInfo } from '@/api/task'
import type { AsyncTaskStep } from '@/api/task'
import { KB_BUILD_DEFAULT_STEPS, normalizeTaskSteps } from '@/utils/buildProgress'
import { useRoute, useRouter } from 'vue-router'
import type { DocumentType, CleaningConfig } from '@/api/document'
import { uploadDocument, getDocumentList, getDocumentContent } from '@/api/document'
import {
  activateKnowledgeBase,
  createKnowledgeBase,
  deleteKnowledgeBase,
  updateKnowledgeBaseDraft,
} from '@/api/knowledgeBase'
import { toastSuccess, toastError } from '@/utils/toast'

const route = useRoute()
const router = useRouter()

const isUploadMode = computed(() => route.name === 'kbUpload')
const kbName = ref('')

const step = ref<1 | 2 | 3>(1)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadError = ref('')
const step1Error = ref('')
const showCleaning = ref(false)

interface UploadedDocPreview {
  documentId: number
  originalName: string
  status: string
  progress: number
  segments: string[]
  segmentCount: number
  contentLoaded: boolean
  error: string | null
}

const uploadedDocs = ref<UploadedDocPreview[]>([])
const activeSegmentByDoc = ref<Record<number, number>>({})
let previewPollTimer: ReturnType<typeof setInterval> | null = null

function getActiveSegment(docId: number): number {
  return activeSegmentByDoc.value[docId] ?? 0
}

function setActiveSegment(docId: number, idx: number) {
  activeSegmentByDoc.value = { ...activeSegmentByDoc.value, [docId]: idx }
}

function shiftActiveSegment(docId: number, delta: number) {
  const doc = uploadedDocs.value.find((d) => d.documentId === docId)
  if (!doc?.segments.length) return
  const next = Math.max(0, Math.min(doc.segments.length - 1, getActiveSegment(docId) + delta))
  setActiveSegment(docId, next)
}

const buildTaskId = ref<string | null>(null)
const buildSteps = ref<AsyncTaskStep[]>([...KB_BUILD_DEFAULT_STEPS])
const buildTaskStatus = ref<string | undefined>()
const buildTaskResult = ref<string | null>(null)
const buildLastUpdate = ref<string | null>(null)

const hasProcessingPreviewDocs = computed(() =>
  uploadedDocs.value.some((d) => d.status !== 'DONE' && d.status !== 'FAILED'),
)

const documentType = ref<DocumentType>('default')

const typeOptions: { value: DocumentType; label: string; desc: string }[] = [
  { value: 'default', label: '通用文档', desc: '适合大多数文档' },
  { value: 'short_text', label: '短文本', desc: '适合短内容片段' },
  { value: 'paper', label: '论文', desc: '适合学术论文' },
  { value: 'contract', label: '合同', desc: '适合合同协议' },
  { value: 'novel', label: '小说', desc: '适合长篇叙事' },
]

const cleaningConfig = ref<CleaningConfig>({
  normalizeWhitespace: true,
  mergeLineBreaks: false,
  filterLowValueParagraphs: false,
  deduplicateParagraphs: false,
  minParagraphLength: 10,
})

const form = ref({
  name: '',
  description: '',
  visibility: 'private' as 'private' | 'public',
})

interface PendingFile {
  name: string
  size: number
  file: File
  status: 'pending' | 'uploading' | 'done' | 'error'
  error?: string
}

const pendingFiles = ref<PendingFile[]>([])
const kbId = ref<number | null>(null)
const creatingKb = ref(false)
const finishingCreate = ref(false)

function goToKbDetail() {
  if (!kbId.value) {
    router.push({ name: 'settings', query: { section: 'documents' } })
    return
  }
  router.push({ name: 'kbDetail', params: { kbId: kbId.value }, query: { tab: 'docs' } })
}

function goToKbList() {
  router.push({ name: 'settings', query: { section: 'documents' } })
}

async function abandonDraft(): Promise<void> {
  if (!kbId.value || isUploadMode.value) return
  try {
    await deleteKnowledgeBase(kbId.value)
  } catch {
    // ignore
  }
  kbId.value = null
}

async function abandonDraftWithConfirm(): Promise<boolean> {
  if (!kbId.value || isUploadMode.value) return true
  if (!window.confirm('放弃将删除未完成的草稿知识库，确定吗？')) return false
  await abandonDraft()
  return true
}

async function cancelWizard() {
  if (isUploadMode.value) {
    goToKbDetail()
    return
  }
  if (kbId.value) {
    const ok = await abandonDraftWithConfirm()
    if (!ok) return
  }
  goToKbList()
}

async function handleBreadcrumbBack() {
  if (isUploadMode.value) {
    goToKbDetail()
    return
  }
  if (kbId.value) {
    const ok = await abandonDraftWithConfirm()
    if (!ok) return
  }
  goToKbList()
}

function handleBack() {
  if (step.value === 3) {
    stopPreviewPoll()
    step.value = 2
  } else if (step.value === 2) {
    if (isUploadMode.value) {
      goToKbDetail()
    } else {
      step.value = 1
    }
  } else {
    cancelWizard()
  }
}

onMounted(async () => {
  if (isUploadMode.value) {
    const id = Number(route.params.kbId)
    if (!id || Number.isNaN(id)) {
      goToKbList()
      return
    }
    kbId.value = id
    step.value = 2
    try {
      const res = await getKnowledgeBase(id)
      if (res.success && res.data) {
        kbName.value = res.data.name
      }
    } catch {
      kbName.value = ''
    }
    return
  }

  const resumeId = route.query.resumeKbId ? Number(route.query.resumeKbId) : null
  if (resumeId && !Number.isNaN(resumeId)) {
    kbId.value = resumeId
    step.value = 2
    try {
      const res = await getKnowledgeBase(resumeId)
      if (res.success && res.data) {
        form.value.name = res.data.name
        form.value.description = res.data.description || ''
        form.value.visibility = (res.data.visibility === 'public' ? 'public' : 'private')
      }
    } catch {
      // ignore
    }
  }
})

async function goToStep2() {
  step1Error.value = ''
  if (!form.value.name.trim()) {
    step1Error.value = '请输入知识库名称'
    return
  }
  creatingKb.value = true
  const payload = {
    name: form.value.name.trim(),
    description: form.value.description.trim() || undefined,
    visibility: form.value.visibility,
  }
  try {
    if (kbId.value) {
      const res = await updateKnowledgeBaseDraft(kbId.value, payload)
      if (res.success) {
        step.value = 2
      } else {
        step1Error.value = res.message || '更新知识库失败'
      }
    } else {
      const res = await createKnowledgeBase(payload)
      if (res.success && res.data?.id) {
        kbId.value = res.data.id
        step.value = 2
      } else {
        step1Error.value = res.message || '创建知识库失败'
      }
    }
  } catch (e: any) {
    step1Error.value = e.message || '创建知识库失败'
  } finally {
    creatingKb.value = false
  }
}

async function finishCreate() {
  if (isUploadMode.value) {
    goToKbDetail()
    return
  }
  if (!kbId.value) {
    toastError('知识库未创建')
    return
  }
  finishingCreate.value = true
  try {
    const res = await activateKnowledgeBase(kbId.value)
    if (res.success) {
      toastSuccess('知识库创建成功')
      router.push({ name: 'kbDetail', params: { kbId: kbId.value }, query: { tab: 'docs' } })
    } else {
      toastError(res.message || '完成创建失败')
    }
  } catch (e: any) {
    toastError(e.message || '完成创建失败')
  } finally {
    finishingCreate.value = false
  }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function addFiles(files: FileList | File[]) {
  for (const file of Array.from(files)) {
    if (!pendingFiles.value.some((f) => f.name === file.name)) {
      pendingFiles.value.push({
        name: file.name,
        size: file.size,
        file,
        status: 'pending',
      })
    }
  }
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) {
    addFiles(input.files)
    input.value = ''
  }
}

function onDrop(e: DragEvent) {
  isDragging.value = false
  if (e.dataTransfer?.files.length) {
    addFiles(e.dataTransfer.files)
  }
}

function removeFile(name: string) {
  pendingFiles.value = pendingFiles.value.filter((f) => f.name !== name)
}

async function startUpload() {
  uploadError.value = ''
  uploading.value = true
  let hasError = false
  const uploaded: UploadedDocPreview[] = []

  for (const item of pendingFiles.value) {
    if (item.status !== 'pending') continue
    item.status = 'uploading'
    try {
      if (!kbId.value) {
        item.status = 'error'
        item.error = isUploadMode.value ? '知识库无效' : '知识库未创建'
        hasError = true
        continue
      }
      const res = await uploadDocument(kbId.value, item.file, documentType.value, cleaningConfig.value)
      if (res.success && res.data) {
        item.status = 'done'
        uploaded.push({
          documentId: res.data.documentId,
          originalName: res.data.originalName,
          status: 'PENDING',
          progress: 0,
          segments: [],
          segmentCount: 0,
          contentLoaded: false,
          error: null,
        })
      } else {
        item.status = 'error'
        item.error = res.message || '上传失败'
        hasError = true
      }
    } catch (e: any) {
      item.status = 'error'
      item.error = e.message || '网络错误'
      hasError = true
    }
  }

  uploading.value = false
  if (hasError && uploaded.length === 0) {
    uploadError.value = '部分文件上传失败，请检查后重试'
    toastError('部分文件上传失败')
  } else {
    if (hasError) {
      toastError('部分文件上传失败')
    } else {
      toastSuccess('上传成功，处理中…')
    }
    uploadedDocs.value = uploaded
    activeSegmentByDoc.value = {}
    buildTaskId.value = null
    buildSteps.value = KB_BUILD_DEFAULT_STEPS.map((s) => ({ ...s }))
    buildTaskStatus.value = undefined
    buildTaskResult.value = null
    buildLastUpdate.value = null
    step.value = 3
    await loadBuildTaskId()
    await refreshKbBuildProgress()
    startPreviewPoll()
  }
}

async function loadBuildTaskId() {
  if (!kbId.value) return
  try {
    const res = await getKnowledgeBase(kbId.value)
    if (res.success && res.data?.buildTaskId) {
      buildTaskId.value = res.data.buildTaskId
    }
  } catch {
    // ignore
  }
}

async function refreshKbBuildProgress() {
  if (!buildTaskId.value) {
    await loadBuildTaskId()
  }
  if (!buildTaskId.value) return
  try {
    const res = await getTaskInfo(buildTaskId.value)
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

// ── 步骤三：分块预览辅助函数 ──────────────────────────────────────

function isPreviewDone(status: string) {
  return status === 'DONE'
}

function isPreviewFailed(status: string) {
  return status === 'FAILED'
}

function previewStatusLabel(status: string): string {
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

function previewStatusClass(status: string): string {
  switch (status) {
    case 'DONE': return 'preview-status-done'
    case 'FAILED': return 'preview-status-failed'
    case 'PENDING': return 'preview-status-pending'
    default: return 'preview-status-processing'
  }
}

async function pollPreviewDocs() {
  if (!kbId.value) return
  await refreshKbBuildProgress()
  try {
    const res = await getDocumentList(kbId.value)
    if (!res.success || !Array.isArray(res.data)) return
    for (const doc of uploadedDocs.value) {
      const found = res.data.find((d) => d.id === doc.documentId)
      if (!found) continue
      doc.status = found.status
      doc.progress = found.progress
      if (found.status === 'DONE' && !doc.contentLoaded) {
        loadPreviewContent(doc)
      }
      if (found.status === 'FAILED' && !doc.contentLoaded) {
        doc.error = found.errorMessage || '处理失败'
        doc.contentLoaded = true
      }
    }
  } catch {
    // ignore poll errors
  }
  const allFinished = uploadedDocs.value.every(
    (d) => d.status === 'DONE' || d.status === 'FAILED',
  )
  if (allFinished) {
    stopPreviewPoll()
  }
}

async function loadPreviewContent(doc: UploadedDocPreview) {
  try {
    const res = await getDocumentContent(doc.documentId)
    if (res.success && res.data) {
      const segments = Array.isArray(res.data.segments)
        ? res.data.segments.filter((s) => !!s && !!s.trim())
        : (res.data.content ?? '')
            .split('\n\n')
            .map((s) => s.trim())
            .filter((s) => !!s)
      doc.segments = segments
      doc.segmentCount = res.data.segmentCount ?? segments.length
      setActiveSegment(doc.documentId, 0)
    }
  } catch {
    // ignore
  } finally {
    doc.contentLoaded = true
  }
}

function startPreviewPoll() {
  if (previewPollTimer) return
  // Trigger once immediately, then every 2s
  pollPreviewDocs()
  previewPollTimer = setInterval(pollPreviewDocs, 2000)
}

function stopPreviewPoll() {
  if (previewPollTimer) {
    clearInterval(previewPollTimer)
    previewPollTimer = null
  }
}

onBeforeUnmount(() => {
  stopPreviewPoll()
})

// ──────────────────────────────────────────────────────────────────

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.kb-create-page {
  min-height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}

.kb-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  position: sticky;
  top: 0;
  z-index: 10;
}

.kb-back-btn {
  padding: 0.375rem 0.625rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1;
}

.kb-back-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}

.kb-breadcrumb {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin: 0;
}

.kb-breadcrumb-parent {
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color 0.2s ease;
}

.kb-breadcrumb-parent:hover {
  color: var(--color-text-accent);
}

.kb-breadcrumb-sep {
  color: var(--color-text-tertiary);
}

.kb-body {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 2rem 1.5rem;
}

.kb-content {
  width: 100%;
  max-width: 600px;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.kb-content--wide {
  max-width: 720px;
}

/* 步骤指示器 */
.kb-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.kb-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
}

.step-circle {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid var(--color-border-hover);
  background: var(--color-bg-page);
  color: var(--color-text-tertiary);
  font-size: 0.875rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.kb-step.active .step-circle {
  border-color: var(--color-button-primary);
  background: var(--color-button-primary);
  color: #fff;
}

.kb-step.done .step-circle {
  border-color: var(--color-button-primary);
  background: var(--color-button-primary);
  color: #fff;
}

.step-label {
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.kb-step.active .step-label,
.kb-step.done .step-label {
  color: var(--color-text-accent);
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  margin: 0 0.5rem;
  margin-bottom: 1.25rem;
  transition: background-color 0.2s ease;
}

.step-line.done {
  background: var(--color-button-primary);
}

/* 表单区域 */
.kb-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 1.75rem;
}

.kb-section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 1.25rem 0;
}

.kb-section-desc {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0 0 1.25rem 0;
}

.kb-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.required {
  color: var(--color-error);
  margin-left: 0.125rem;
}

.form-input,
.form-textarea {
  padding: 0.625rem 0.875rem;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s ease;
  width: 100%;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  border-color: var(--color-border-focus);
}

.form-textarea {
  resize: vertical;
  min-height: 6rem;
}

/* 可见范围单选 */
.radio-group {
  display: flex;
  gap: 0.75rem;
}

.radio-option {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.radio-option input[type="radio"] {
  accent-color: var(--color-button-primary);
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  flex-shrink: 0;
}

.radio-option.selected {
  border-color: var(--color-button-primary);
  background: var(--color-bg-input);
}

.radio-label {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
}

.radio-desc {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.form-error {
  font-size: 0.875rem;
  color: var(--color-error);
  margin: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

.btn-primary {
  padding: 0.625rem 1.5rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, opacity 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-button-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 0.625rem 1.25rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}

/* 上传区域 */
.upload-zone {
  border: 2px dashed var(--color-border-hover);
  border-radius: 10px;
  padding: 2.5rem 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 1rem;
}

.upload-zone:hover,
.upload-zone.drag-over {
  border-color: var(--color-button-primary);
  background: var(--color-bg-input);
}

.upload-zone-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
}

.upload-zone-text {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  margin: 0 0 0.375rem 0;
}

.upload-zone-hint {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  margin: 0;
}

.file-input-hidden {
  display: none;
}

/* 文件列表 */
.upload-file-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.upload-file-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.875rem;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

.file-size {
  color: var(--color-text-tertiary);
  white-space: nowrap;
  flex-shrink: 0;
}

.file-status {
  flex-shrink: 0;
  font-size: 0.8125rem;
  white-space: nowrap;
}

.file-status.pending { color: var(--color-text-tertiary); }
.file-status.uploading { color: var(--color-text-accent); }
.file-status.done { color: #22c55e; }
.file-status.error { color: var(--color-error); }

.file-remove-btn {
  padding: 0.125rem 0.375rem;
  font-size: 1rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  line-height: 1;
  transition: color 0.2s ease;
  flex-shrink: 0;
}

.file-remove-btn:hover {
  color: var(--color-error);
}

/* 文档类型 & 清洗配置 */
.config-block {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.config-block-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--color-bg-input);
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s ease;
}

.config-block-toggle:hover {
  background: var(--color-bg-card);
}

.config-block-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text-primary);
  padding: 0.75rem 1rem;
  background: var(--color-bg-input);
}

.config-block > .config-block-title {
  display: block;
}

.toggle-arrow {
  font-size: 1.25rem;
  color: var(--color-text-tertiary);
  transition: transform 0.2s ease;
  line-height: 1;
}

.toggle-arrow.open {
  transform: rotate(90deg);
}

/* 文档类型单选 */
.type-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
}

.type-radio-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.type-radio-option input[type="radio"] {
  accent-color: var(--color-button-primary);
  width: 0.9rem;
  height: 0.9rem;
  cursor: pointer;
  flex-shrink: 0;
}

.type-radio-option.selected {
  border-color: var(--color-button-primary);
  background: var(--color-bg-input);
}

.type-radio-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
}

.type-radio-desc {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

/* 清洗选项 */
.cleaning-options {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-top: 1px solid var(--color-border);
}

.cleaning-option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-bottom: 1px solid var(--color-border);
}

.cleaning-option:last-child {
  border-bottom: none;
}

.cleaning-option:hover {
  background: var(--color-bg-input);
}

.cleaning-option input[type="checkbox"] {
  accent-color: var(--color-button-primary);
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.cleaning-option-text {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.cleaning-option-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-primary);
}

.cleaning-option-desc {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  line-height: 1.4;
}

.cleaning-suboption {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem 0.75rem 2.75rem;
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
}

.cleaning-suboption-label {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.cleaning-number-input {
  width: 5rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s ease;
  text-align: center;
}

.cleaning-number-input:focus {
  border-color: var(--color-border-focus);
}

.cleaning-suboption-unit {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

/* ── 步骤三：分块预览 ──────────────────────────── */
.kb-section-preview {
  padding-top: 1.25rem;
}

.kb-section-preview :deep(.kb-build-section) {
  margin-bottom: 1rem;
}

.preview-section-head {
  margin-bottom: 1rem;
  padding-top: 0.25rem;
  border-top: 1px solid var(--color-border);
}

.preview-title {
  margin-bottom: 0.375rem;
}

.preview-desc {
  margin-bottom: 0;
}

.preview-docs {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1rem;
}

.preview-doc-item {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--color-bg-page);
}

.preview-doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
}

.preview-doc-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  flex: 1;
}

.preview-doc-icon {
  flex-shrink: 0;
  font-size: 1rem;
  line-height: 1;
}

.preview-doc-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-doc-status {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  flex-shrink: 0;
}

.preview-status-done { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
.preview-status-failed { background: rgba(239, 68, 68, 0.1); color: var(--color-error); }
.preview-status-pending { background: rgba(107, 114, 128, 0.1); color: #6b7280; }
.preview-status-processing { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }

.preview-processing {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 1.25rem 1rem;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.preview-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-text-accent);
  border-radius: 50%;
  animation: preview-spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes preview-spin {
  to { transform: rotate(360deg); }
}

.preview-error {
  padding: 1rem;
  font-size: 0.875rem;
  color: var(--color-error);
}

.preview-segments {
  padding: 0.875rem 1rem 1rem;
}

.preview-segments-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.preview-segments-meta {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.preview-segment-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
}

.preview-segment-tab {
  min-width: 2rem;
  height: 2rem;
  padding: 0 0.5rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preview-segment-tab:hover {
  border-color: var(--color-border-focus);
  color: var(--color-text-primary);
}

.preview-segment-tab.active {
  color: #fff;
  background: var(--color-button-primary);
  border-color: var(--color-button-primary);
}

.preview-segment-panel {
  padding: 0.875rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-card);
  min-height: 8rem;
  max-height: 360px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.preview-segment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}

.preview-segment-index {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.preview-segment-len {
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

.preview-segment-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.75;
}

.preview-segment-nav {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.preview-nav-btn {
  padding: 0.375rem 0.875rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preview-nav-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
}

.preview-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.preview-actions {
  margin-top: 0.25rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

.preview-empty {
  padding: 1rem;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  text-align: center;
}
</style>
