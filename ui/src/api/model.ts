import { $get, $post, $delete } from './request'

export interface ModelApiKeyItem {
  id: number
  modelName: string
  modelProvider: string
  keyType: string
  maskedApiKey: string
  createdAt: string | null
}

export function getModelKeyList() {
  return $get<ModelApiKeyItem[]>('/model/list')
}

export function createModelKey(params: {
  modelName: string
  modelProvider: string
  keyType: string
  apiKey: string
}) {
  const search = new URLSearchParams()
  search.set('modelName', params.modelName)
  search.set('modelProvider', params.modelProvider)
  search.set('keyType', params.keyType)
  search.set('apiKey', params.apiKey)
  return $post<ModelApiKeyItem>(`/model/create?${search.toString()}`)
}

export function deleteModelKey(id: number) {
  return $delete(`/model/${id}`)
}
