import { $get, $post, $delete } from './request'

export interface ApiKeyListItem {
  id: number
  name: string | null
  status: number
  expiresAt: string | null
  lastUsedAt: string | null
  createdAt: string | null
  apiKey: string
}

export interface CreateApiKeyResult {
  id: number
  apiKey: string
  name: string | null
  expiresAt: string | null
}

export function getApiKeyList() {
  return $get<ApiKeyListItem[]>('/apikey/list')
}

export function createApiKey(params?: { name?: string; expiresInDays?: number }) {
  const search = new URLSearchParams()
  if (params?.name != null) search.set('name', params.name)
  if (params?.expiresInDays != null) search.set('expiresInDays', String(params.expiresInDays))
  const qs = search.toString()
  return $post<CreateApiKeyResult>(`/apikey/create${qs ? '?' + qs : ''}`)
}

export function disableApiKey(id: number) {
  return $post(`/apikey/disable/${id}`)
}

export function deleteApiKey(id: number) {
  return $delete(`/apikey/${id}`)
}
