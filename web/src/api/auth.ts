import http from './http'

export interface UserResponse {
  id: number
  email: string
  nickname: string
  role: string
}

export interface AuthData {
  user: UserResponse
  authenticated: boolean
  accessToken?: string
  tokenType?: string
  expiresIn?: number
}

export interface AuthRequest {
  email: string
  password: string
  nickname?: string
}

export async function login(payload: Omit<AuthRequest, 'nickname'>) {
  const response = await http.post<{ data: AuthData }>('/auth/login', payload)
  return response.data.data
}

export async function register(payload: AuthRequest) {
  const response = await http.post<{ data: AuthData }>('/auth/register', payload)
  return response.data.data
}

export async function currentUser() {
  const response = await http.get<{ data: UserResponse }>('/auth/me')
  return response.data.data
}
