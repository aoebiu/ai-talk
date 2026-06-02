import { $get, $post } from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface MemberInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  token: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  phone?: string
}

export async function login(body: LoginRequest) {
  return $post<MemberInfo>('/member/login', body)
}

export async function logout() {
  return $post('/member/logout')
}

export async function getMemberInfo() {
  return $get<MemberInfo>('/member/info')
}

export async function getMemberList() {
  return $get<MemberInfo[]>('/member/list')
}

export async function createMember(body: RegisterRequest) {
  return $post('/member/register', body)
}

export async function deleteMember(id: number) {
  return $post(`/member/delete/${id}`)
}
