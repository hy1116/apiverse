import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const admin = JSON.parse(localStorage.getItem('av_admin_user') || '{}')
  if (admin.token) config.headers.Authorization = `Bearer ${admin.token}`
  return config
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthEndpoint = err.config?.url?.startsWith('/admin/auth/')
    if (err.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('av_admin_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default client
