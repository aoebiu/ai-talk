import { $get, $put, $delete } from './request'

export interface BizConfigItem {
  id: number
  configKey: string
  displayValue: string
  configValue?: string | null
  encryptStorage: boolean
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface BizConfigSaveBody {
  configValue: string
  encryptStorage: boolean
  remark?: string | null
}

export function getBizConfigList() {
  return $get<BizConfigItem[]>('/configs/list')
}

export function getBizConfigItem(key: string) {
  return $get<BizConfigItem>(`/configs/${encodeURIComponent(key)}`)
}

export function saveBizConfigItem(key: string, body: BizConfigSaveBody) {
  return $put<BizConfigItem>(`/configs/${encodeURIComponent(key)}`, body)
}

export function deleteBizConfigItem(key: string) {
  return $delete(`/configs/${encodeURIComponent(key)}`)
}
