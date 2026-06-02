import type { AsyncTaskStep } from '@/api/task'

export interface AsyncTaskStepSummary {
  message?: string
  durationMs?: number
  processedChunks?: number
  totalChunks?: number
  doneDocuments?: number
  totalDocuments?: number
}

export interface BuildTaskStep extends AsyncTaskStep {
  summary?: AsyncTaskStepSummary | null
  durationMs?: number | null
  startedAt?: string | null
  finishedAt?: string | null
}

export const KB_BUILD_DEFAULT_STEPS: BuildTaskStep[] = [
  { step: 1, label: '文档解析', status: 'pending' },
  { step: 2, label: '内容清洗', status: 'pending' },
  { step: 3, label: '文本分块', status: 'pending' },
  { step: 4, label: '文档向量化', status: 'pending' },
]

export interface BuildOverallStatus {
  status: 'building' | 'done' | 'partial' | 'failed' | 'idle'
  label: string
  message: string
}

function normalizeStatus(status: string | undefined): BuildTaskStep['status'] {
  if (status === 'running' || status === 'completed' || status === 'failed' || status === 'pending') {
    return status
  }
  return 'pending'
}

/** 将接口返回的步骤与默认四步对齐，并保留 summary / 耗时等字段 */
export function normalizeTaskSteps(raw: Array<AsyncTaskStep | BuildTaskStep>): BuildTaskStep[] {
  return KB_BUILD_DEFAULT_STEPS.map((def) => {
    const found = raw.find((s) => s.step === def.step)
    if (!found) {
      return { ...def }
    }
    const extended = found as BuildTaskStep
    return {
      step: found.step ?? def.step,
      label: found.label || def.label,
      status: normalizeStatus(found.status),
      summary: extended.summary ?? null,
      durationMs: extended.durationMs ?? null,
      startedAt: extended.startedAt ?? null,
      finishedAt: extended.finishedAt ?? null,
    }
  })
}

export function formatStepDuration(ms: number | null | undefined): string | null {
  if (ms == null || ms < 0) return null
  if (ms < 1000) return `${ms}ms`
  const totalSec = Math.floor(ms / 1000)
  if (totalSec < 60) return `${totalSec}s`
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  if (min < 60) {
    return sec > 0 ? `${min}m ${sec}s` : `${min}m`
  }
  const hour = Math.floor(min / 60)
  const remainMin = min % 60
  return remainMin > 0 ? `${hour}h ${remainMin}m` : `${hour}h`
}

export function getStepDetailMessage(step: BuildTaskStep): string | null {
  const msg = step.summary?.message
  if (msg) return msg
  if (step.status === 'completed') {
    return `${step.label}完成`
  }
  if (step.status === 'running') {
    return `${step.label}进行中…`
  }
  if (step.status === 'failed') {
    return `${step.label}失败`
  }
  return null
}

export function getStepDurationText(step: BuildTaskStep): string | null {
  const text = formatStepDuration(step.durationMs ?? step.summary?.durationMs)
  return text ? `耗时: ${text}` : null
}

export function resolveBuildOverallStatus(
  taskStatus?: string,
  taskResult?: string | null,
  hasProcessingDocs = false,
): BuildOverallStatus {
  if (taskStatus === 'FAILED') {
    return { status: 'failed', label: '构建失败', message: '部分文档处理失败，请查看详情' }
  }
  if (taskStatus === 'COMPLETED' && !hasProcessingDocs) {
    return { status: 'done', label: '构建完成', message: '知识库文档已全部处理完成' }
  }
  if (taskStatus === 'COMPLETED' && hasProcessingDocs) {
    return { status: 'building', label: '构建中', message: '正在同步最新进度…' }
  }
  if (taskStatus === 'RUNNING' || hasProcessingDocs) {
    return { status: 'building', label: '构建中', message: '知识库文档正在处理，请稍候' }
  }
  if (taskResult) {
    try {
      const parsed = JSON.parse(taskResult) as { message?: string }
      if (parsed.message) {
        return { status: 'done', label: '构建完成', message: parsed.message }
      }
    } catch {
      // ignore
    }
  }
  if (hasProcessingDocs) {
    return { status: 'building', label: '构建中', message: '文档处理进行中' }
  }
  return { status: 'idle', label: '待构建', message: '上传文档后将自动开始构建' }
}
