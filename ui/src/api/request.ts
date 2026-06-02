import { toastError } from '@/utils/toast'

export interface ApiResponse<T = unknown> {
  success: boolean
  message?: string
  data?: T
}

const API_PREFIX = '/api'

export function resolveApiUrl(url: string): string {
  return API_PREFIX + (url.startsWith('/') ? url : '/' + url)
}

export function getToken(): string | null {
  return localStorage.getItem('token')
}

function showApiError(message?: string) {
  if (!message) return
  toastError(message)
}

/**
 * Token 失效回调，由 router 设置
 */
let onTokenExpired: (() => void) | null = null

export function setTokenExpiredCallback(callback: () => void) {
  onTokenExpired = callback
}

function handleTokenExpired() {
  if (onTokenExpired) {
    onTokenExpired()
    return
  }
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

function buildHeaders(options: RequestInit): HeadersInit {
  const token = getToken()
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (token) {
    ;(headers as Record<string, string>)['Authorization'] = `Bearer ${token}`
  }
  return headers
}

export async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const fullUrl = resolveApiUrl(url)
  const headers = buildHeaders(options)
  const res = await fetch(fullUrl, { ...options, headers })

  // Token 失效，强制跳转登录
  if (res.status === 401) {
    handleTokenExpired()
    return { success: false, message: '登录已过期，请重新登录' }
  }

  const json = await res.json().catch(() => ({}))
  if (!res.ok) {
    const errorRes = { success: false, message: json.message || res.statusText, data: json }
    showApiError(errorRes.message)
    return errorRes
  }
  const apiRes = json as ApiResponse<T>
  if (apiRes?.success === false) {
    showApiError(apiRes.message || '请求失败')
  }
  return apiRes
}

/**
 * GET 请求，自动附带 token
 * @example $get<SessionItem[]>('/chat/sessions')
 */
export async function $get<T = unknown>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  return request<T>(url, { ...options, method: 'GET' })
}

/**
 * POST 请求，自动附带 token，body 会序列化为 JSON
 * @example $post<MemberInfo>('/member/login', { username, password })
 */
export async function $post<T = unknown>(
  url: string,
  body?: unknown,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  return request<T>(url, {
    ...options,
    method: 'POST',
    ...(body !== undefined && body !== null ? { body: JSON.stringify(body) } : {}),
  })
}

/**
 * 流式 POST 请求，自动附带 token 和 /api 前缀，返回原生 Response 便于 getReader() 读流
 * @example const res = await $postStream('/chat/stream', body, { signal })
 */
export async function $postStream(
  url: string,
  body?: unknown,
  options: RequestInit = {}
): Promise<Response> {
  const fullUrl = resolveApiUrl(url)
  const headers = buildHeaders(options)
  const res = await fetch(fullUrl, {
    ...options,
    method: 'POST',
    headers,
    ...(body !== undefined && body !== null ? { body: JSON.stringify(body) } : {}),
  })
  // Token 失效，清除本地存储并触发回调
  if (res.status === 401) {
    handleTokenExpired()
  }
  return res
}

/**
 * 上传文件（FormData），不设置 Content-Type，由浏览器自动带 boundary
 * @example $upload<{ id: string; name: string }>('/documents/upload', formData)
 */
export async function $upload<T = unknown>(
  url: string,
  formData: FormData,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const fullUrl = resolveApiUrl(url)
  const token = getToken()
  const headers: HeadersInit = {
    ...(options.headers as Record<string, string>),
  }
  if (token) {
    ;(headers as Record<string, string>)['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(fullUrl, { ...options, method: 'POST', headers, body: formData })

  // Token 失效，清除本地存储并触发回调
  if (res.status === 401) {
    handleTokenExpired()
    return { success: false, message: '登录已过期，请重新登录' }
  }

  const json = await res.json().catch(() => ({}))
  if (!res.ok) {
    const errorRes = {
      success: false,
      message: (json as { message?: string }).message || res.statusText,
      data: json,
    }
    showApiError(errorRes.message)
    return errorRes
  }
  const apiRes = json as ApiResponse<T>
  if (apiRes?.success === false) {
    showApiError(apiRes.message || '请求失败')
  }
  return apiRes
}

/**
 * PUT 请求，自动附带 token，body 会序列化为 JSON
 * @example $put('/functioncall/1', { name: 'xxx' })
 */
export async function $put<T = unknown>(
  url: string,
  body?: unknown,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  return request<T>(url, {
    ...options,
    method: 'PUT',
    ...(body !== undefined && body !== null ? { body: JSON.stringify(body) } : {}),
  })
}

/**
 * DELETE 请求，自动附带 token
 * @example $delete('/apikey/1')
 */
export async function $delete<T = unknown>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  return request<T>(url, { ...options, method: 'DELETE' })
}
