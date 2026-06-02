import { $get } from './request'

export interface AsyncTaskStep {
  step: number
  label: string
  status: 'pending' | 'running' | 'completed' | 'failed'
}

export interface AsyncTaskInfo {
  taskId: string
  taskType: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  currentStep: number
  totalSteps: number
  steps: string // JSON string of AsyncTaskStep[]
  result: string | null
  errorMessage: string | null
}

export function getTaskInfo(taskId: string) {
  return $get<AsyncTaskInfo>(`/task/${taskId}`)
}
