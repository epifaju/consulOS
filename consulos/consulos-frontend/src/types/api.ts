export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
  errorCode: string | null
  timestamp: string
}
