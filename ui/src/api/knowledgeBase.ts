import { $get, $post, $put, $delete } from './request'

export type KnowledgeBaseStatus = 'DRAFT' | 'ACTIVE'

export interface KnowledgeBaseItem {
  id: number
  name: string
  description: string | null
  visibility: string
  status?: KnowledgeBaseStatus
  indexName: string
  documentCount: number
  buildTaskId?: string | null
  createdAt: string | null
}

export interface KnowledgeBaseCreatePayload {
  name: string
  description?: string
  visibility?: 'private' | 'public'
}

export interface KnowledgeBaseCreateResult {
  id: number
  name: string
  indexName: string
  status?: KnowledgeBaseStatus
}

/** 创建知识库 */
export async function createKnowledgeBase(payload: KnowledgeBaseCreatePayload) {
  return $post<KnowledgeBaseCreateResult>('/kb', payload)
}

/** 知识库列表 */
export async function getKnowledgeBaseList() {
  return $get<KnowledgeBaseItem[]>('/kb/list')
}

/** 知识库详情 */
export async function getKnowledgeBase(kbId: number) {
  return $get<KnowledgeBaseItem>(`/kb/${kbId}`)
}

/** 更新草稿知识库基本信息 */
export async function updateKnowledgeBaseDraft(
  kbId: number,
  payload: KnowledgeBaseCreatePayload,
) {
  return $put<KnowledgeBaseItem>(`/kb/${kbId}`, payload)
}

/** 将草稿知识库设为正式（向导完成） */
export async function activateKnowledgeBase(kbId: number) {
  return $post<KnowledgeBaseItem>(`/kb/${kbId}/activate`)
}

/** 删除知识库（含草稿） */
export async function deleteKnowledgeBase(kbId: number) {
  return $delete(`/kb/${kbId}`)
}
