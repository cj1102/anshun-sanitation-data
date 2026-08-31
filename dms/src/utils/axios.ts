import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    // Controller modules that have completed the Java layering refactor use ApiResponse<T>.
    // Unwrap it here so existing Vue pages can keep consuming response.data directly.
    if (response.data && typeof response.data.code === 'number' && 'message' in response.data) {
      response.data = response.data.data
    }
    return response
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // Only redirect if we are not already on the login page to avoid redirect loops
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = '/login'
      }
    }
    const payload = error.response?.data
    if (payload && typeof payload.code === 'number' && payload.message) {
      return Promise.reject({ error: payload.message, code: payload.code })
    }
    return Promise.reject(payload || error)
  }
)

export default instance
