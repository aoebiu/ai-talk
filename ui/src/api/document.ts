import { $get, $upload, $delete } from './request'

/** 文档类型，与后端 /api/documents/upload 的 type 参数一致 */
export type DocumentType = 'short_text' | 'paper' | 'contract' | 'novel' | 'default'

/** 文档清洗规则配置，对应后端 CleaningConfig */
export interface CleaningConfig {
  /** 空白规范化：合并连续空格/Tab，压缩多余换行，去除行首尾空白 */
  normalizeWhitespace: boolean
  /** 断行合并：将单个换行合并为空格，保留双换行作为段落边界（PDF 常见问题） */
  mergeLineBreaks: boolean
  /** 低价值段落过滤：丢弃极短段落及纯符号/数字段落 */
  filterLowValueParagraphs: boolean
  /** 重复段落去重：移除内容完全相同的重复段落 */
  deduplicateParagraphs: boolean
  /** 低价值段落的最小字符长度阈值，仅在 filterLowValueParagraphs=true 时生效 */
  minParagraphLength: number
}

/** 后端 /api/documents/upload 返回的 data 结构 */
export interface UploadResponse {
  documentId: number
  taskId: string
  originalName: string
}

/** 后端 /api/documents/list 返回的单条文档结构 */
export interface DocumentInfoItem {
  id: number
  kbId?: number
  originalName: string
  indexName: string
  fileType: string
  docType: string
  fileSize: number
  /** 当前处理状态：PENDING/PARSING/CLEANING/CHUNKING/EMBEDDING/DONE/FAILED */
  status: string
  totalChunks: number | null
  processedChunks: number | null
  /** 处理进度 0-100 */
  progress: number
  errorMessage: string | null
  taskId: string | null
  createdAt: string | null
}

/** 后端 /api/documents/{documentId}/content 返回结构 */
export interface DocumentContentItem {
  documentId: number
  originalName: string
  status: string
  segmentCount: number
  content: string
  segments?: string[]
}

/**
 * 上传文档并触发向量化
 * @param file 文件
 * @param type 文档类型，决定分块策略
 * @param cleaningConfig 清洗规则配置
 */
export async function uploadDocument(
  kbId: number,
  file: File,
  type: DocumentType = 'default',
  cleaningConfig?: CleaningConfig,
) {
  const formData = new FormData()
  formData.append('file', file)
  let url = `/documents/upload?kbId=${kbId}&type=${encodeURIComponent(type)}`
  if (cleaningConfig) {
    url += `&cleaningConfig=${encodeURIComponent(JSON.stringify(cleaningConfig))}`
  }
  return $upload<UploadResponse>(url, formData)
}

/**
 * 获取已上传文档列表
 */
export async function getDocumentList(kbId: number) {
  return $get<DocumentInfoItem[]>(`/documents/list?kbId=${kbId}`)
}

/**
 * 获取单个文档详情
 */
export async function getDocument(documentId: number) {
  return $get<DocumentInfoItem>(`/documents/${documentId}`)
}

/**
 * 删除指定文档
 * @param documentId document_info.id
 */
export async function deleteDocument(documentId: number) {
  return $delete(`/documents/${documentId}`)
}

/**
 * 获取文档拆分后的全文内容（按分块顺序拼接）
 */
export async function getDocumentContent(documentId: number, maxSegments = 500) {
  return $get<DocumentContentItem>(`/documents/${documentId}/content?maxSegments=${maxSegments}`)
}
