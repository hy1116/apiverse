import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem('av_user') || '{}')
  if (user.token) config.headers.Authorization = `Bearer ${user.token}`
  return config
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('av_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default client
