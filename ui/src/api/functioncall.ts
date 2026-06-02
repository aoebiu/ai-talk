import { $get, $post, $put, $delete } from './request'

export interface FunctionCallItem {
  id: number
  memberId: number
  name: string
  description: string
  property: string | null
  required: string | null
  execute: string | null
  generatePrompt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface FunctionCallForm {
  name: string
  description: string
  property: string
  required: string
  execute: string
  generatePrompt: string
}

export function getFunctionCallList() {
  return $get<FunctionCallItem[]>('/functioncall/list')
}

export function getFunctionCallById(id: number) {
  return $get<FunctionCallItem>(`/functioncall/${id}`)
}

export function createFunctionCall(params: FunctionCallForm) {
  return $post<FunctionCallItem>('/functioncall/create', params)
}

export function updateFunctionCall(id: number, params: FunctionCallForm) {
  return $put<FunctionCallItem>(`/functioncall/${id}`, params)
}

export function deleteFunctionCall(id: number) {
  return $delete(`/functioncall/${id}`)
}

export interface FunctionCallGenerateForm {
  prompt: string
}

export interface PropertyDetail {
  type: string
  description: string
}

export interface FunctionCallGenerateResult {
  name: string
  description: string
  properties: Record<string, PropertyDetail>
  required: string[]
  execute: string
}

export interface FunctionCallGenerateSubmitResult {
  taskId: string
}

export function generateToolMetadata(params: FunctionCallGenerateForm) {
  return $post<FunctionCallGenerateSubmitResult>('/functioncall/generate/script', params)
}

export interface FunctionCallTestForm {
  parameters?: string
}

export function testFunctionCall(id: number, params: FunctionCallTestForm) {
  return $post<{ result: string }>(`/functioncall/${id}/test`, params)
}

export interface FunctionCallGenerateTestCasesForm {
  count: number
}

export function generateTestCasesForTool(id: number, params: FunctionCallGenerateTestCasesForm) {
  return $post<{ testCases: string }>(`/functioncall/${id}/generate/testcases`, params)
}
