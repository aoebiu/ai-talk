<template>
  <div class="fc-detail-page">
    <!-- 顶部导航栏 -->
    <header class="fc-detail-header">
      <button type="button" class="fc-back-btn" @click="goBack">&lt;</button>
      <h1 class="fc-breadcrumb">
        <span class="fc-breadcrumb-parent" @click="goBack">Function</span>
        <span class="fc-breadcrumb-sep">/</span>
        <span class="fc-breadcrumb-current">{{ isEdit ? functionCallForm.name || '...' : '新建工具' }}</span>
      </h1>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="fc-detail-loading">
      <span class="fc-spinner" />
      <span>加载中…</span>
    </div>

    <!-- 主体内容 -->
    <main v-else class="fc-detail-body">
      <div class="fc-detail-content">
        <!-- AI 生成区域 -->
        <section class="fc-section">
          <div class="fc-section-header">
            <h2 class="fc-section-title">AI 生成工具描述</h2>
            <button
              type="button"
              class="btn-generate"
              :disabled="generatingScript"
              @click="handleGenerateScript"
            >{{ generatingScript ? '生成中...' : '开始生成' }}</button>
          </div>
          <div class="generate-area">
            <textarea
              v-model="generatePrompt"
              class="fc-input fc-textarea"
              placeholder="描述你想要的工具功能，例如：查询指定城市的天气信息，需要传入城市名称"
              rows="3"
            />
            <div class="generate-progress">
              <div
                v-for="(s, idx) in generatingSteps"
                :key="s.step"
                class="progress-step"
                :class="'step-' + s.status"
              >
                <span class="step-dot">
                  <span v-if="s.status === 'completed'" class="step-check">&#10003;</span>
                  <span v-if="s.status === 'running'" class="step-spinner"></span>
                </span>
                <span class="step-label">{{ s.label }}</span>
                <span v-if="idx < generatingSteps.length - 1" class="step-line" :class="{ done: s.status === 'completed' }"></span>
              </div>
            </div>
          </div>
        </section>

        <hr class="fc-divider" />

        <!-- 工具名称 -->
        <section class="fc-section">
          <h2 class="fc-section-title">工具名称 <span class="required">*</span></h2>
          <input
            v-model="functionCallForm.name"
            type="text"
            class="fc-input"
            :disabled="isEdit"
            placeholder="例如：weatherQuery"
          />
        </section>

        <!-- 工具描述 -->
        <section class="fc-section">
          <h2 class="fc-section-title">工具描述 <span class="required">*</span></h2>
          <input
            v-model="functionCallForm.description"
            type="text"
            class="fc-input"
            :disabled="isEdit"
            placeholder="例如：查询指定城市的天气信息"
          />
        </section>

        <!-- 属性列表 -->
        <section class="fc-section">
          <h2 class="fc-section-title">属性列表</h2>
          <div class="property-list">
            <div
              v-for="(prop, index) in functionCallProperties"
              :key="index"
              class="property-row"
            >
              <label class="property-checkbox">
                <input type="checkbox" v-model="prop.required" />
                <span>必需</span>
              </label>
              <input
                v-model="prop.key"
                type="text"
                class="fc-input property-input"
                placeholder="参数名"
              />
              <input
                v-model="prop.description"
                type="text"
                class="fc-input property-input property-input-desc"
                placeholder="参数描述"
              />
              <button type="button" class="property-remove-btn" @click="removeFunctionCallProperty(index)">删除</button>
            </div>
            <button type="button" class="property-add-btn" @click="addFunctionCallProperty">+ 添加属性</button>
          </div>
        </section>

        <!-- 执行脚本 -->
        <section class="fc-section">
          <div class="fc-section-header">
            <h2 class="fc-section-title">执行脚本</h2>
            <div class="fc-section-actions">
              <button type="button" class="btn-secondary" @click="formatExecuteScript">格式化</button>
            </div>
          </div>
          <Codemirror
            v-model="functionCallForm.execute"
            :extensions="cmExtensions"
            placeholder="function execute(params) { ... }"
            :style="{ minHeight: '240px' }"
            class="fc-code-editor"
          />

          <!-- 调试区域（可折叠） -->
          <div v-if="isEdit" class="debug-panel">
            <div class="debug-panel-toggle" @click="showDebugPanel = !showDebugPanel">
              <span class="debug-toggle-left">
                <span class="debug-toggle-arrow" :class="{ expanded: showDebugPanel }">&#9654;</span>
                <span>调试工具</span>
              </span>
              <div class="debug-mode-toggle" @click.stop>
                <button
                  type="button"
                  class="debug-mode-btn"
                  :class="{ active: debugMode === 'manual' }"
                  @click="setDebugMode('manual')"
                >手动测试</button>
                <button
                  type="button"
                  class="debug-mode-btn"
                  :class="{ active: debugMode === 'auto' }"
                  @click="setDebugMode('auto')"
                >自动测试</button>
              </div>
            </div>
            <div v-if="showDebugPanel" class="debug-panel-body">
              <!-- 手动测试 -->
              <template v-if="debugMode === 'manual'">
              <div v-if="debugProperties.length > 0" class="debug-property-list">
                <div v-for="prop in debugProperties" :key="prop.key" class="debug-property-row">
                  <label class="debug-property-label">
                    {{ prop.key }}
                    <span v-if="prop.required" class="debug-required-badge">必填</span>
                  </label>
                  <input
                    v-model="prop.value"
                    type="text"
                    class="fc-input"
                    :placeholder="prop.description"
                  />
                </div>
              </div>
              <p v-else class="form-hint">该工具未定义属性参数，将使用空对象测试</p>
              <div class="debug-actions">
                <button type="button" class="btn-confirm btn-sm" :disabled="testingFunctionCall" @click="submitDebug">
                  {{ testingFunctionCall ? '测试中...' : '执行测试' }}
                </button>
              </div>
              <div v-if="debugResult" class="debug-result">
                <div class="fc-section-header">
                  <label class="form-label">执行结果</label>
                  <button type="button" class="btn-copy-result" @click="copyDebugResult">复制</button>
                </div>
                <pre class="result-display">{{ debugResult }}</pre>
              </div>
              <p v-if="debugError" class="fc-error">{{ debugError }}</p>
              </template>

              <!-- 自动测试 -->
              <template v-if="debugMode === 'auto'">
              <div class="batch-header-actions">
                  <label class="form-label batch-loop-label">生成个数</label>
                  <input
                    v-model.number="batchGenerateCount"
                    type="number"
                    min="1"
                    max="100"
                    class="fc-input batch-loop-input"
                  />
                  <button
                    type="button"
                    class="btn-secondary btn-sm"
                    :disabled="generatingTestCases"
                    @click="generateBatchTestCases"
                  >
                    {{ generatingTestCases ? '生成中...' : '生成测试用例' }}
                  </button>
                </div>

              <!-- 测试用例卡片列表 -->
              <div class="batch-case-list">
                <div
                  v-for="(testCase, caseIdx) in batchTestCases"
                  :key="caseIdx"
                  class="batch-case-item"
                >
                  <div class="batch-result-header">
                    <span class="batch-result-index">#{{ caseIdx + 1 }}</span>
                    <button type="button" class="batch-case-delete" @click="removeBatchTestCase(caseIdx)">删除</button>
                  </div>
                  <div class="batch-result-body">
                    <div class="batch-case-params-section">
                      <span class="batch-label">输入</span>
                      <div class="batch-param-rows">
                        <div
                          v-for="(param, paramIdx) in testCase.params"
                          :key="paramIdx"
                          class="batch-param-row"
                        >
                          <input
                            v-model="param.key"
                            type="text"
                            class="fc-input batch-param-key"
                            placeholder="参数名"
                          />
                          <span class="batch-param-sep">:</span>
                          <input
                            v-model="param.value"
                            type="text"
                            class="fc-input batch-param-val"
                            placeholder="值"
                          />
                          <button
                            type="button"
                            class="batch-param-remove"
                            :disabled="isBatchParamRequired(param.key)"
                            @click="removeBatchTestCaseParam(caseIdx, paramIdx)"
                          >×</button>
                        </div>
                        <span v-if="testCase.params.length === 0" class="form-hint">无参数</span>
                      </div>
                      <button type="button" class="batch-add-param-btn" @click="addBatchTestCaseParam(caseIdx)">+ 参数</button>
                    </div>
                    <div class="batch-case-expected-section">
                      <span class="batch-label">期望</span>
                      <input
                        v-model="testCase.expected"
                        type="text"
                        class="fc-input"
                        placeholder="可选，结果中应包含的字符串"
                      />
                    </div>
                  </div>
                </div>
                <button type="button" class="property-add-btn" @click="addBatchTestCase">+ 添加测试用例</button>
              </div>

              <div class="batch-test-actions">
                <button
                  type="button"
                  class="btn-confirm btn-sm"
                  :disabled="batchTesting"
                  @click="startBatchTest"
                >
                  {{ batchTesting ? `测试中 (${batchTestProgress}/${batchTestTotal})...` : '开始批量测试' }}
                </button>
              </div>

              <!-- 批量测试日志 -->
              <div v-if="batchTestLog" class="batch-test-log">
                <div class="fc-section-header">
                  <h3 class="batch-test-title">测试日志</h3>
                  <button type="button" class="btn-copy-result" @click="copyBatchLog">复制日志</button>
                </div>
                <div class="batch-summary">
                  <span class="batch-summary-item">
                    总数: <strong>{{ batchTestLog.total }}</strong>
                  </span>
                  <span class="batch-summary-item batch-summary-success">
                    成功: <strong>{{ batchTestLog.successCount }}</strong>
                  </span>
                  <span class="batch-summary-item batch-summary-fail">
                    失败: <strong>{{ batchTestLog.failCount }}</strong>
                  </span>
                  <span class="batch-summary-item batch-summary-accuracy">
                    正确率: <strong>{{ batchTestLog.accuracy }}%</strong>
                  </span>
                </div>
                <div class="batch-results">
                  <div
                    v-for="(item, idx) in batchTestLog.results"
                    :key="idx"
                    class="batch-result-item"
                    :class="item.success ? 'result-success' : 'result-fail'"
                  >
                    <div class="batch-result-header">
                      <span class="batch-result-index">#{{ idx + 1 }}</span>
                      <span class="batch-result-status" :class="item.success ? 'status-success' : 'status-fail'">
                        {{ item.success ? '成功' : '失败' }}
                      </span>
                    </div>
                    <div class="batch-result-body">
                      <div><span class="batch-label">输入:</span> {{ JSON.stringify(item.params) }}</div>
                      <div v-if="item.expected"><span class="batch-label">期望:</span> {{ item.expected }}</div>
                      <div><span class="batch-label">结果:</span> {{ item.result || item.error }}</div>
                    </div>
                  </div>
                </div>
              </div>
              </template>
            </div>
          </div>
        </section>

        <p v-if="formError" class="fc-error">{{ formError }}</p>

        <!-- 底部操作栏 -->
        <div class="fc-detail-footer">
          <button type="button" class="btn-cancel" @click="goBack">返回列表</button>
          <div class="fc-footer-save-pair" role="presentation">
            <button
              type="button"
              class="btn-secondary fc-footer-save-btn"
              :disabled="submitting"
              @click="submitFunctionCall(false)"
            >
              {{ submitting ? '保存中...' : '仅保存' }}
            </button>
            <button
              type="button"
              class="btn-confirm fc-footer-save-btn"
              :disabled="submitting"
              @click="submitFunctionCall(true)"
            >
              {{ submitting ? '保存中...' : '保存并返回' }}
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFunctionCallById, createFunctionCall, updateFunctionCall, generateToolMetadata, testFunctionCall, generateTestCasesForTool } from '@/api/functioncall'
import type { FunctionCallForm, FunctionCallItem, PropertyDetail } from '@/api/functioncall'
import { getTaskInfo } from '@/api/task'
import type { AsyncTaskStep } from '@/api/task'
import { toastError, toastSuccess } from '@/utils/toast'
import { js_beautify } from 'js-beautify'
import { Codemirror } from 'vue-codemirror'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'

const cmExtensions = [javascript(), oneDark]

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.name === 'functionCallDetail')
const functionCallId = computed(() => isEdit.value ? Number(route.params.id) : null)

// 加载状态
const loading = ref(false)

// 表单
const functionCallForm = ref<FunctionCallForm>({
  name: '',
  description: '',
  property: '',
  required: '',
  execute: '',
  generatePrompt: '',
})

interface PropertyItem {
  key: string
  description: string
  required: boolean
}
const functionCallProperties = ref<PropertyItem[]>([])

// 提交状态
const submitting = ref(false)
const formError = ref('')

// AI 生成
const generatePrompt = ref('')
const showGenerateArea = ref(true)
const generateDone = ref(false)
const generatingTaskId = ref<string | null>(null)
const generatingSteps = ref<AsyncTaskStep[]>([
  { step: 1, label: '能力分析', status: 'pending' },
  { step: 2, label: '生成工具元数据', status: 'pending' },
  { step: 3, label: '填充结果', status: 'pending' },
])
const generatingScript = computed(() => generatingTaskId.value !== null)
let generatePollTimer: ReturnType<typeof setInterval> | null = null

// 调试
interface DebugPropertyItem {
  key: string
  description: string
  required: boolean
  value: string
}
const showDebugPanel = ref(false)
const debugMode = ref<'manual' | 'auto'>('manual')
const testingFunctionCall = ref(false)
const debugProperties = ref<DebugPropertyItem[]>([])
const debugResult = ref<string | null>(null)
const debugError = ref('')

// 批量测试
interface BatchTestCaseParam {
  key: string
  value: string
}
interface BatchTestCaseItem {
  params: BatchTestCaseParam[]
  expected: string
}
const batchTestCases = ref<BatchTestCaseItem[]>([])
const batchGenerateCount = ref(3)
const batchTesting = ref(false)
const batchTestProgress = ref(0)
const batchTestTotal = ref(0)
const generatingTestCases = ref(false)

interface BatchTestResult {
  params: Record<string, string>
  expected?: string
  result?: string
  error?: string
  success: boolean
}

interface BatchTestLog {
  total: number
  successCount: number
  failCount: number
  accuracy: string
  results: BatchTestResult[]
}

const batchTestLog = ref<BatchTestLog | null>(null)

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function setDebugMode(mode: 'manual' | 'auto') {
  debugMode.value = mode
  showDebugPanel.value = true
}

function goBack() {
  router.push({ name: 'settings', query: { section: 'functionCall' } })
}

// 加载数据（编辑模式）
async function loadDetail() {
  if (!functionCallId.value) return
  loading.value = true
  try {
    const res = await getFunctionCallById(functionCallId.value)
    if (res.success && res.data) {
      const item = res.data
      functionCallForm.value = {
        name: item.name,
        description: item.description,
        property: item.property || '',
        required: item.required || '',
        execute: item.execute || '',
        generatePrompt: item.generatePrompt || '',
      }
      generatePrompt.value = item.generatePrompt || ''
      // 解析属性
      const properties: PropertyItem[] = []
      try {
        const propObj = item.property ? JSON.parse(item.property) : {}
        const reqArr: string[] = item.required ? JSON.parse(item.required) : []
        for (const [key, desc] of Object.entries(propObj)) {
          properties.push({ key, description: String(desc), required: reqArr.includes(key) })
        }
      } catch {
        // JSON 解析失败
      }
      functionCallProperties.value = properties
      if (item.generatePrompt) {
        generatingSteps.value = [
          { step: 1, label: '能力分析', status: 'completed' },
          { step: 2, label: '生成工具元数据', status: 'completed' },
          { step: 3, label: '填充结果', status: 'completed' },
        ]
        generateDone.value = true
      }
    } else {
      toastError('加载工具详情失败')
      goBack()
    }
  } catch {
    toastError('加载工具详情失败')
    goBack()
  } finally {
    loading.value = false
  }
}

// 属性操作
function addFunctionCallProperty() {
  functionCallProperties.value.push({ key: '', description: '', required: false })
}

function removeFunctionCallProperty(index: number) {
  functionCallProperties.value.splice(index, 1)
}

// 脚本格式化
function formatExecuteScript() {
  if (!functionCallForm.value.execute?.trim()) return
  functionCallForm.value.execute = js_beautify(functionCallForm.value.execute, {
    indent_size: 2,
    space_in_empty_paren: false,
  })
}

// AI 生成相关
function stopGeneratePoll() {
  if (generatePollTimer) {
    clearInterval(generatePollTimer)
    generatePollTimer = null
  }
}

function fillFormFromResult(resultJson: string) {
  try {
    const data = JSON.parse(resultJson) as {
      name?: string
      description?: string
      execute?: string
      properties?: Record<string, PropertyDetail>
      required?: string[]
    }
    if (data.name) functionCallForm.value.name = data.name
    if (data.description) functionCallForm.value.description = data.description
    if (data.execute) functionCallForm.value.execute = data.execute
    const properties: PropertyItem[] = []
    const reqArr: string[] = Array.isArray(data.required) ? data.required : []
    if (data.properties && typeof data.properties === 'object') {
      for (const [key, prop] of Object.entries(data.properties as Record<string, PropertyDetail>)) {
        properties.push({
          key,
          description: prop.description || '',
          required: reqArr.includes(key),
        })
      }
    }
    functionCallProperties.value = properties
  } catch {
    formError.value = '解析生成结果失败'
  }
}

async function handleGenerateScript() {
  formError.value = ''
  generateDone.value = false
  if (!generatePrompt.value.trim()) {
    formError.value = '请先输入工具描述提示词'
    return
  }

  try {
    const res = await generateToolMetadata({
      prompt: generatePrompt.value.trim(),
    })
    if (!res.success || !res.data) {
      formError.value = res.message || '提交任务失败'
      return
    }

    generatingTaskId.value = res.data.taskId
    generatingSteps.value = [
      { step: 1, label: '能力分析', status: 'pending' },
      { step: 2, label: '生成工具元数据', status: 'pending' },
      { step: 3, label: '填充结果', status: 'pending' },
    ]

    generatePollTimer = setInterval(async () => {
      if (!generatingTaskId.value) {
        stopGeneratePoll()
        return
      }
      try {
        const taskRes = await getTaskInfo(generatingTaskId.value)
        if (!taskRes.success || !taskRes.data) return

        const task = taskRes.data
        if (task.steps) {
          const steps: AsyncTaskStep[] = typeof task.steps === 'string' ? JSON.parse(task.steps) : task.steps
          const fillStep = generatingSteps.value[2] || { step: 3, label: '填充结果', status: 'pending' }
          generatingSteps.value = [...steps, fillStep]
        }

        if (task.status === 'COMPLETED') {
          stopGeneratePoll()
          generatingSteps.value[2] = { step: 3, label: '填充结果', status: 'running' }
          if (task.result) fillFormFromResult(task.result)
          generatingSteps.value[2] = { step: 3, label: '填充结果', status: 'completed' }
          generatingTaskId.value = null
          generateDone.value = true
        } else if (task.status === 'FAILED') {
          stopGeneratePoll()
          formError.value = task.errorMessage || '生成失败'
          generatingTaskId.value = null
          generateDone.value = true
        }
      } catch {
        // 轮询异常不中断
      }
    }, 1500)
  } catch {
    formError.value = '生成请求失败'
  }
}

// 提交表单；exitAfter 为 true 时保存后返回工具列表
async function submitFunctionCall(exitAfter: boolean) {
  formError.value = ''
  if (!functionCallForm.value.name.trim()) {
    formError.value = '请输入工具名称'
    return
  }
  if (!functionCallForm.value.description.trim()) {
    formError.value = '请输入工具描述'
    return
  }

  const validProps = functionCallProperties.value.filter((p) => p.key.trim())
  let propertyStr = ''
  let requiredStr = ''
  if (validProps.length > 0) {
    const propObj: Record<string, string> = {}
    const reqArr: string[] = []
    for (const p of validProps) {
      propObj[p.key.trim()] = p.description.trim()
      if (p.required) reqArr.push(p.key.trim())
    }
    propertyStr = JSON.stringify(propObj)
    requiredStr = JSON.stringify(reqArr)
  }

  submitting.value = true
  try {
    const params: FunctionCallForm = {
      name: functionCallForm.value.name.trim(),
      description: functionCallForm.value.description.trim(),
      property: propertyStr,
      required: requiredStr,
      execute: functionCallForm.value.execute.trim(),
      generatePrompt: generatePrompt.value.trim(),
    }
    const res = isEdit.value
      ? await updateFunctionCall(functionCallId.value!, params)
      : await createFunctionCall(params)
    if (res.success) {
      toastSuccess(isEdit.value ? '更新成功' : '创建成功')
      if (exitAfter) {
        goBack()
      } else if (!isEdit.value) {
        const created = res.data as FunctionCallItem | undefined
        if (created?.id != null) {
          await router.replace({ name: 'functionCallDetail', params: { id: String(created.id) } })
        }
      }
    } else {
      formError.value = res.message || (isEdit.value ? '更新失败' : '创建失败')
    }
  } catch (error: unknown) {
    formError.value = getErrorMessage(error, '操作失败')
  } finally {
    submitting.value = false
  }
}

// 调试
watch(showDebugPanel, (open) => {
  if (open) {
    debugResult.value = null
    debugError.value = ''
    const props: DebugPropertyItem[] = []
    for (const p of functionCallProperties.value) {
      if (p.key.trim()) {
        props.push({ key: p.key, description: p.description, required: p.required, value: '' })
      }
    }
    debugProperties.value = props
  }
})

async function copyDebugResult() {
  if (!debugResult.value) return
  try {
    await navigator.clipboard.writeText(debugResult.value)
    toastSuccess('已复制到剪贴板')
  } catch {
    toastError('复制失败，请手动复制')
  }
}

async function submitDebug() {
  debugError.value = ''
  if (!functionCallId.value) return

  for (const prop of debugProperties.value) {
    if (prop.required && !prop.value.trim()) {
      debugError.value = `参数 "${prop.key}" 为必填项`
      return
    }
  }

  let parametersStr: string | undefined
  if (debugProperties.value.length > 0) {
    const params: Record<string, string> = {}
    for (const prop of debugProperties.value) {
      if (prop.value.trim()) {
        params[prop.key] = prop.value.trim()
      }
    }
    parametersStr = Object.keys(params).length > 0 ? JSON.stringify(params) : undefined
  }

  testingFunctionCall.value = true
  try {
    const res = await testFunctionCall(functionCallId.value, {
      parameters: parametersStr,
    })
    if (res.success && res.data) {
      debugResult.value = typeof res.data.result === 'string'
        ? res.data.result
        : JSON.stringify(res.data.result, null, 2)
    } else {
      debugError.value = res.message || '测试失败'
    }
  } catch (error: unknown) {
    debugError.value = getErrorMessage(error, '测试请求失败')
  } finally {
    testingFunctionCall.value = false
  }
}

/** 将 AI 返回的 params（对象或 {key,value}[]）转为键值表 */
function generatedParamsToRecord(raw: unknown): Record<string, unknown> {
  if (raw == null || raw === '') return {}
  if (Array.isArray(raw)) {
    const out: Record<string, unknown> = {}
    for (const item of raw) {
      if (item && typeof item === 'object' && 'key' in item) {
        const k = String((item as { key: unknown }).key ?? '').trim()
        if (!k) continue
        out[k] = 'value' in item ? (item as { value: unknown }).value : ''
      }
    }
    return out
  }
  if (typeof raw === 'object') return raw as Record<string, unknown>
  return {}
}

/** 当前参数名是否为工具定义的必填属性（用于禁用批量测试里该行的删除按钮） */
function isBatchParamRequired(paramKey: string): boolean {
  const k = paramKey.trim()
  if (!k) return false
  const prop = functionCallProperties.value.find((p) => p.key.trim() === k)
  return prop?.required ?? false
}

/** 与手动测试一致：按属性列表顺序为每条用例补齐参数名 key，并用生成结果填充 value */
function batchParamsAlignedWithSchema(gen: Record<string, unknown>): BatchTestCaseParam[] {
  const schemaKeys = functionCallProperties.value.map((p) => p.key.trim()).filter(Boolean)
  if (schemaKeys.length === 0) {
    return Object.entries(gen).map(([key, value]) => ({ key, value: String(value ?? '') }))
  }
  return schemaKeys.map((key) => ({
    key,
    value: String(gen[key] ?? ''),
  }))
}

async function generateBatchTestCases() {
  if (!functionCallId.value) return
  generatingTestCases.value = true
  try {
    const res = await generateTestCasesForTool(functionCallId.value, { count: batchGenerateCount.value })
    if (res.success && res.data?.testCases) {
      try {
        const parsed = JSON.parse(res.data.testCases)
        if (Array.isArray(parsed)) {
          batchTestCases.value = parsed.map((item: unknown) => {
            const generatedCase = (item && typeof item === 'object'
              ? (item as { params?: unknown; expected?: unknown })
              : {}) as { params?: unknown; expected?: unknown }
            return {
              params: batchParamsAlignedWithSchema(generatedParamsToRecord(generatedCase.params)),
              expected: typeof generatedCase.expected === 'string' ? generatedCase.expected : '',
            }
          })
        }
      } catch {
        toastError('解析生成结果失败')
      }
    } else {
      toastError(res.message || '生成测试用例失败')
    }
  } catch (error: unknown) {
    toastError(getErrorMessage(error, '生成请求失败'))
  } finally {
    generatingTestCases.value = false
  }
}

function addBatchTestCase() {
  batchTestCases.value.push({
    params: batchParamsAlignedWithSchema({}),
    expected: '',
  })
}

function removeBatchTestCase(caseIdx: number) {
  batchTestCases.value.splice(caseIdx, 1)
}

function addBatchTestCaseParam(caseIdx: number) {
  const testCase = batchTestCases.value[caseIdx]
  if (!testCase) return
  testCase.params.push({ key: '', value: '' })
}

function removeBatchTestCaseParam(caseIdx: number, paramIdx: number) {
  const testCase = batchTestCases.value[caseIdx]
  if (!testCase) return
  const row = testCase.params[paramIdx]
  if (row && isBatchParamRequired(row.key)) return
  testCase.params.splice(paramIdx, 1)
}

async function startBatchTest() {
  if (!functionCallId.value) return
  batchTestLog.value = null

  if (batchTestCases.value.length === 0) {
    toastError('请至少添加一个测试用例')
    return
  }

  batchTesting.value = true
  batchTestTotal.value = batchTestCases.value.length
  batchTestProgress.value = 0
  const results: BatchTestResult[] = []

  for (const testCase of batchTestCases.value) {
    const params: Record<string, string> = {}
    for (const p of testCase.params) {
      if (p.key.trim()) params[p.key.trim()] = p.value
    }
    const expected = testCase.expected.trim() || undefined

    try {
      const res = await testFunctionCall(functionCallId.value, {
        parameters: Object.keys(params).length > 0 ? JSON.stringify(params) : undefined,
      })

      const resultStr = res.success && res.data
        ? (typeof res.data.result === 'string' ? res.data.result : JSON.stringify(res.data.result, null, 2))
        : ''

      let success = res.success && !!res.data
      if (success && expected) {
        success = resultStr.includes(expected)
      }

      results.push({
        params,
        expected,
        result: resultStr || (res.message || '无结果'),
        success,
      })
    } catch (error: unknown) {
      results.push({
        params,
        expected,
        error: getErrorMessage(error, '请求失败'),
        success: false,
      })
    }

    batchTestProgress.value++
  }

  const successCount = results.filter((r) => r.success).length
  batchTestLog.value = {
    total: results.length,
    successCount,
    failCount: results.length - successCount,
    accuracy: results.length > 0 ? (successCount / results.length * 100).toFixed(1) : '0',
    results,
  }

  batchTesting.value = false
}

async function copyBatchLog() {
  if (!batchTestLog.value) return
  const log = batchTestLog.value
  const lines: string[] = [
    `=== 批量测试日志 ===`,
    `总数: ${log.total}  成功: ${log.successCount}  失败: ${log.failCount}  正确率: ${log.accuracy}%`,
    ``,
  ]
  for (const [index, r] of log.results.entries()) {
    lines.push(`#${index + 1} [${r.success ? '成功' : '失败'}]`)
    lines.push(`  输入: ${JSON.stringify(r.params)}`)
    if (r.expected) lines.push(`  期望: ${r.expected}`)
    lines.push(`  结果: ${r.result || r.error}`)
  }
  try {
    await navigator.clipboard.writeText(lines.join('\n'))
    toastSuccess('日志已复制到剪贴板')
  } catch {
    toastError('复制失败，请手动复制')
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadDetail()
  }
})

onBeforeUnmount(() => {
  stopGeneratePoll()
})
</script>

<style scoped>
.fc-detail-page {
  min-height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

/* 顶部导航 */
.fc-detail-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 2rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.fc-back-btn {
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

.fc-back-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
  background: var(--color-bg-input);
}

.fc-breadcrumb {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.fc-breadcrumb-parent {
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color 0.2s;
}

.fc-breadcrumb-parent:hover {
  color: var(--color-text-accent);
}

.fc-breadcrumb-sep {
  color: var(--color-text-tertiary);
  font-weight: 400;
}

.fc-breadcrumb-current {
  color: var(--color-text-primary);
}

/* 加载状态 */
.fc-detail-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.fc-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-text-accent);
  border-radius: 50%;
  animation: fc-spin 0.7s linear infinite;
}

@keyframes fc-spin {
  to { transform: rotate(360deg); }
}

/* 主体 */
.fc-detail-body {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.fc-detail-content {
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 1.5rem 2rem 3rem;
}

/* 区块 */
.fc-section {
  margin-bottom: 1.5rem;
}

.fc-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.fc-section-header .fc-section-title {
  margin-bottom: 0;
}

.fc-section-title {
  margin: 0 0 0.5rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.fc-section-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.required {
  color: var(--color-error);
}

/* 分割线 */
.fc-divider {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0.5rem 0 1.5rem;
}

/* 输入框 */
.fc-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  box-sizing: border-box;
}

.fc-input:focus {
  outline: none;
  border-color: var(--color-border-focus);
}

.fc-input:disabled {
  opacity: 0.55;
  color: var(--color-text-tertiary);
  background: var(--color-bg-card);
  cursor: not-allowed;
}

.fc-textarea {
  resize: vertical;
  min-height: 2.5rem;
  font-family: inherit;
  line-height: 1.5;
}

.fc-code-editor {
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  overflow: hidden;
  font-size: 0.8125rem;
}

.fc-code-editor:focus-within {
  border-color: var(--color-border-focus);
}

/* 错误信息 */
.fc-error {
  margin: 0.5rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-error);
}

/* 按钮 */
.btn-generate {
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
  color: #fff;
  background: var(--color-primary, #7c3aed);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: opacity 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-generate:hover:not(:disabled) {
  opacity: 0.85;
}

.btn-generate:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

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

.btn-secondary:hover {
  color: var(--color-primary, #7c3aed);
  border-color: var(--color-primary, #7c3aed);
}

/* 底部操作栏 */
.fc-detail-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-border);
  margin-top: 1rem;
}

.fc-detail-footer .btn-cancel {
  margin-right: auto;
}

/* 两颗保存按钮等宽（按较长文案对齐）；inline-grid 避免在 flex 页脚里被横向撑满 */
.fc-footer-save-pair {
  display: inline-grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  align-items: stretch;
  vertical-align: middle;
}

.fc-detail-footer .fc-footer-save-btn {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
  min-height: 2.375rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  line-height: 1.25;
  border-radius: 8px;
}

.btn-cancel,
.btn-confirm {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel {
  color: var(--color-text-primary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
}

.btn-cancel:hover {
  border-color: var(--color-border-focus);
}

.btn-confirm {
  color: #fff;
  background: var(--color-text-accent);
  border: 1px solid var(--color-text-accent);
}

.btn-confirm:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 生成区域 */
.generate-area {
  margin-top: 0.5rem;
}

.generate-input-row {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}

.generate-input-row .fc-textarea {
  flex: 1;
}

/* 生成进度条 */
.generate-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.75rem;
  padding: 0.625rem 0.875rem;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.step-line {
  display: block;
  width: 3rem;
  height: 2px;
  background: var(--color-border, #d1d5db);
  margin: 0 0.5rem;
  transition: background 0.3s;
}

.step-line.done {
  background: #22c55e;
}

.step-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-border, #d1d5db);
  transition: background 0.3s, box-shadow 0.3s;
  font-size: 12px;
  color: #fff;
}

.step-check {
  font-size: 13px;
  font-weight: bold;
  line-height: 1;
}

.step-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin-dot 0.8s linear infinite;
}

.step-label {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  white-space: nowrap;
  font-weight: 500;
}

.step-pending .step-dot {
  background: var(--color-border, #d1d5db);
}

.step-running .step-dot {
  background: var(--color-primary, #7c3aed);
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.15);
}

.step-running .step-label {
  color: var(--color-primary, #7c3aed);
}

.step-completed .step-dot {
  background: #22c55e;
}

.step-completed .step-label {
  color: #22c55e;
}

@keyframes spin-dot {
  to { transform: rotate(360deg); }
}

/* 属性列表 */
.property-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.property-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.property-checkbox {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  white-space: nowrap;
  cursor: pointer;
}

.property-checkbox input[type="checkbox"] {
  cursor: pointer;
}

.property-input {
  flex: 1;
  min-width: 0;
}

.property-input-desc {
  flex: 2;
}

.property-remove-btn {
  padding: 0.35rem 0.6rem;
  font-size: 0.75rem;
  color: var(--color-error);
  background: transparent;
  border: 1px solid var(--color-error);
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.property-remove-btn:hover {
  color: #fff;
  background: var(--color-error);
}

.property-add-btn {
  align-self: flex-start;
  padding: 0.35rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-accent);
  background: transparent;
  border: 1px dashed var(--color-text-accent);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.property-add-btn:hover {
  background: var(--color-bg-input);
}

/* 调试折叠面板 */
.debug-panel {
  margin-top: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

.debug-panel-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.6rem 0.75rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: none;
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
}

.debug-panel-toggle:hover {
  color: var(--color-text-primary);
}

.debug-toggle-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.debug-toggle-arrow {
  font-size: 0.625rem;
  transition: transform 0.2s;
}

.debug-toggle-arrow.expanded {
  transform: rotate(90deg);
}

/* 手动/自动测试滑钮 */
.debug-mode-toggle {
  display: flex;
  align-items: center;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 2px;
  gap: 2px;
}

.debug-mode-btn {
  padding: 0.2rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.debug-mode-btn:hover:not(.active) {
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
}

.debug-mode-btn.active {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}

.debug-panel-body {
  padding: 1rem;
  border-top: 1px solid var(--color-border);
}

.debug-actions {
  margin-top: 0.75rem;
}

.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.8125rem;
}

.form-label {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-primary);
}

.form-hint {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  line-height: 1.4;
}

/* 调试 */
.debug-property-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.debug-property-row {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.debug-property-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.debug-required-badge {
  font-size: 0.6875rem;
  font-weight: 500;
  color: var(--color-error);
  background: rgba(239, 68, 68, 0.1);
  padding: 0.05rem 0.35rem;
  border-radius: 3px;
}

.debug-result {
  margin-top: 1rem;
}

.result-display {
  margin: 0.5rem 0 0;
  padding: 0.75rem;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  font-family: ui-monospace, monospace;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

.btn-copy-result {
  padding: 0.25rem 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-copy-result:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus);
}

/* 批量测试 */
.batch-divider {
  border: none;
  border-top: 1px dashed var(--color-border);
  margin: 1.25rem 0;
}

.batch-test-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.batch-header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.batch-loop-label {
  margin-bottom: 0 !important;
  white-space: nowrap;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.batch-loop-input {
  width: 64px !important;
}

.batch-test-actions {
  margin-top: 0.75rem;
}

/* 测试用例卡片列表 */
.batch-case-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.batch-case-item {
  padding: 0.625rem 0.75rem;
  border: 1px solid var(--color-border);
  border-left: 3px solid var(--color-border-focus);
  border-radius: 6px;
  font-size: 0.8125rem;
}

.batch-case-delete {
  padding: 0.15rem 0.5rem;
  font-size: 0.75rem;
  color: var(--color-error);
  background: transparent;
  border: 1px solid var(--color-error);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.batch-case-delete:hover {
  color: #fff;
  background: var(--color-error);
}

.batch-case-params-section,
.batch-case-expected-section {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-top: 0.4rem;
}

.batch-case-params-section .batch-label,
.batch-case-expected-section .batch-label {
  flex-shrink: 0;
  min-width: 2.5rem;
  padding-top: 0.4rem;
}

.batch-case-expected-section .fc-input {
  flex: 1;
}

.batch-param-rows {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex: 1;
}

.batch-param-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.batch-param-key {
  width: 110px !important;
  flex-shrink: 0;
}

.batch-param-val {
  flex: 1;
}

.batch-param-sep {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.batch-param-remove {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  line-height: 1;
  color: var(--color-text-tertiary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.batch-param-remove:hover:not(:disabled) {
  color: var(--color-error);
  border-color: var(--color-error);
}

.batch-param-remove:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.batch-add-param-btn {
  align-self: flex-start;
  margin-top: 0.2rem;
  padding: 0.15rem 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-accent);
  background: transparent;
  border: 1px dashed var(--color-text-accent);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.batch-add-param-btn:hover {
  background: var(--color-bg-input);
}

/* 批量测试日志 */
.batch-test-log {
  margin-top: 1.25rem;
}

.batch-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  margin-bottom: 0.75rem;
}

.batch-summary-item {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.batch-summary-item strong {
  color: var(--color-text-primary);
}

.batch-summary-success strong {
  color: #22c55e;
}

.batch-summary-fail strong {
  color: var(--color-error);
}

.batch-summary-accuracy strong {
  color: var(--color-text-accent);
}

.batch-results {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 400px;
  overflow-y: auto;
}

.batch-result-item {
  padding: 0.625rem 0.75rem;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  font-size: 0.8125rem;
}

.batch-result-item.result-success {
  border-left: 3px solid #22c55e;
}

.batch-result-item.result-fail {
  border-left: 3px solid var(--color-error);
}

.batch-result-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
}

.batch-result-index {
  font-weight: 600;
  color: var(--color-text-secondary);
}

.batch-result-status {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.1rem 0.4rem;
  border-radius: 3px;
}

.status-success {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.1);
}

.status-fail {
  color: var(--color-error);
  background: rgba(239, 68, 68, 0.1);
}

.batch-result-body {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: var(--color-text-secondary);
  word-break: break-word;
}

.batch-label {
  font-weight: 500;
  color: var(--color-text-primary);
}
</style>
