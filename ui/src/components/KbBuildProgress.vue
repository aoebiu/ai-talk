<template>
  <section class="kb-build-section">
    <div class="kb-build-header">
      <h2 class="kb-build-title">{{ title }}</h2>
    </div>

    <div class="kb-build-card">
      <div class="kb-build-summary">
        <span class="kb-build-badge" :class="'badge-' + overall.status">{{ overall.label }}</span>
        <span class="kb-build-message">{{ overall.message }}</span>
        <span v-if="totalDurationText" class="kb-build-total-duration">{{ totalDurationText }}</span>
        <span v-if="lastUpdateTime" class="kb-build-updated">最近更新: {{ lastUpdateTime }}</span>
      </div>

      <div class="kb-build-pipeline">
        <div
          v-for="(step, idx) in steps"
          :key="step.step"
          class="pipeline-step"
          :class="'step-' + step.status"
        >
          <div class="pipeline-track">
            <span class="step-dot">
              <span v-if="step.status === 'completed'" class="step-check">✓</span>
              <span v-else-if="step.status === 'running'" class="step-spinner" />
              <span v-else-if="step.status === 'failed'" class="step-failed-mark">✕</span>
              <span v-else class="step-num">{{ step.step }}</span>
            </span>
            <span
              v-if="idx < steps.length - 1"
              class="pipeline-connector"
              :class="{ done: step.status === 'completed' }"
            />
          </div>
          <div class="step-info">
            <span class="step-label">{{ step.label }}</span>
            <span v-if="getStepDetailMessage(step)" class="step-detail">{{ getStepDetailMessage(step) }}</span>
            <span v-if="getStepDurationText(step)" class="step-duration">{{ getStepDurationText(step) }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BuildTaskStep } from '@/utils/buildProgress'
import {
  formatStepDuration,
  getStepDetailMessage,
  getStepDurationText,
  resolveBuildOverallStatus,
} from '@/utils/buildProgress'

const props = withDefaults(
  defineProps<{
    title?: string
    steps: BuildTaskStep[]
    taskStatus?: string
    taskResult?: string | null
    hasProcessingDocs?: boolean
    lastUpdateTime?: string | null
  }>(),
  {
    title: '构建进度',
    hasProcessingDocs: false,
  },
)

const overall = computed(() =>
  resolveBuildOverallStatus(props.taskStatus, props.taskResult ?? null, props.hasProcessingDocs),
)

const totalDurationText = computed(() => {
  let total = 0
  let hasAny = false
  for (const step of props.steps) {
    const ms = step.durationMs ?? step.summary?.durationMs
    if (ms != null && ms >= 0) {
      total += ms
      hasAny = true
    }
  }
  if (!hasAny) return null
  const formatted = formatStepDuration(total)
  return formatted ? `总耗时 ${formatted}` : null
})
</script>

<style scoped>
.kb-build-section {
  margin-bottom: 1.25rem;
}

.kb-build-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.625rem;
}

.kb-build-title {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
}

.kb-build-card {
  padding: 1rem 1.125rem;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  transition: padding 0.2s ease;
}

.kb-build-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem 0.75rem;
  margin-bottom: 0;
}

.kb-build-summary {
  margin-bottom: 1rem;
}

.kb-build-badge {
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-building { background: rgba(59, 130, 246, 0.12); color: #3b82f6; }
.badge-done { background: rgba(34, 197, 94, 0.12); color: #22c55e; }
.badge-partial { background: rgba(234, 179, 8, 0.15); color: #ca8a04; }
.badge-failed { background: rgba(239, 68, 68, 0.12); color: #ef4444; }
.badge-idle { background: rgba(107, 114, 128, 0.12); color: #6b7280; }

.kb-build-message {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.kb-build-total-duration {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.kb-build-updated {
  margin-left: auto;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

/* 展开：横向流水线 */
.kb-build-pipeline {
  display: flex;
  align-items: stretch;
  --connector-gap: 10px;
}

.pipeline-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  position: relative;
}

.pipeline-track {
  position: relative;
  width: 100%;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pipeline-connector {
  position: absolute;
  left: calc(50% + 12px + var(--connector-gap));
  right: calc(-50% + 12px + var(--connector-gap));
  top: 50%;
  height: 2px;
  background: var(--color-border);
  border-radius: 999px;
  z-index: 0;
  transition: background 0.3s;
  transform: translateY(-50%);
}

.pipeline-connector.done {
  background: #22c55e;
}

.step-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-border);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  position: relative;
  z-index: 1;
}

.step-num {
  color: var(--color-text-tertiary);
  font-size: 0.6875rem;
}

.step-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin-dot 0.8s linear infinite;
}

.step-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  margin-top: 0.5rem;
  text-align: center;
  width: 100%;
}

.step-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.step-detail {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.step-duration {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
}

.step-running .step-dot { background: var(--color-button-primary, #6366f1); }
.step-running .step-label { color: var(--color-button-primary, #6366f1); }
.step-completed .step-dot { background: #22c55e; }
.step-completed .step-label { color: #22c55e; }
.step-failed .step-dot { background: #ef4444; }
.step-failed .step-label { color: #ef4444; }

@keyframes spin-dot {
  to { transform: rotate(360deg); }
}

@media (max-width: 520px) {
  .kb-build-updated {
    margin-left: 0;
    width: 100%;
  }

  .kb-build-pipeline {
    flex-direction: column;
    gap: 0.75rem;
  }

  .pipeline-connector {
    display: none;
  }

  .pipeline-step {
    align-items: flex-start;
    padding: 0;
  }

  .pipeline-track {
    width: auto;
    height: auto;
    justify-content: flex-start;
  }

  .step-info {
    margin-top: 0.25rem;
    align-items: flex-start;
    text-align: left;
  }
}
</style>
