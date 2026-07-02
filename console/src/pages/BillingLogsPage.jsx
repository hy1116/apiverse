import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

const PAGE_SIZE = 50

const DAY_OPTIONS = [
  { value: 1, label: '오늘' },
  { value: 7, label: '7일' },
  { value: 30, label: '30일' },
  { value: 90, label: '90일' },
]

function formatTime(requestTime) {
  if (!requestTime) return ''
  return requestTime.replace('T', ' ').slice(0, 19)
}

function statusColor(status) {
  if (status >= 500) return 'text-red-600 dark:text-red-400 font-semibold'
  if (status >= 400) return 'text-amber-600 dark:text-amber-400 font-semibold'
  return 'text-emerald-600 dark:text-emerald-400'
}

export default function BillingLogsPage() {
  const [onlyErrors, setOnlyErrors] = useState(false)
  const [days, setDays] = useState(7)
  const [page, setPage] = useState(0)
  const [items, setItems] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    client.get('/admin/usage/logs', { params: { onlyErrors, days, page, size: PAGE_SIZE } })
      .then(({ data }) => {
        setItems(data.items)
        setTotal(data.total)
      })
      .catch(() => setError('로그를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [onlyErrors, days, page])

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const switchFilter = (value) => {
    setOnlyErrors(value)
    setPage(0)
  }

  const switchDays = (value) => {
    setDays(value)
    setPage(0)
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
          <h1 className="text-xl font-bold text-gray-900 dark:text-white">요청 로그</h1>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-800/60 p-1 rounded-xl border border-gray-200 dark:border-gray-700">
              {DAY_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  onClick={() => switchDays(opt.value)}
                  className={`px-3.5 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                    days === opt.value
                      ? 'bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-sm border border-gray-200 dark:border-gray-700'
                      : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-800/60 p-1 rounded-xl border border-gray-200 dark:border-gray-700">
              <button
                onClick={() => switchFilter(false)}
                className={`px-3.5 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  !onlyErrors
                    ? 'bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-sm border border-gray-200 dark:border-gray-700'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                }`}
              >
                전체
              </button>
              <button
                onClick={() => switchFilter(true)}
                className={`px-3.5 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  onlyErrors
                    ? 'bg-white dark:bg-gray-900 text-red-600 dark:text-red-400 shadow-sm border border-gray-200 dark:border-gray-700'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                }`}
              >
                에러만
              </button>
            </div>
          </div>
        </div>

        {error && <p className="text-sm text-red-500 dark:text-red-400 mb-4">{error}</p>}

        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                <th className="px-5 py-3">시간</th>
                <th className="px-5 py-3">API Key</th>
                <th className="px-5 py-3">Method</th>
                <th className="px-5 py-3">Path</th>
                <th className="px-5 py-3">상태</th>
                <th className="px-5 py-3">Client IP</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">불러오는 중...</td></tr>
              ) : items.length === 0 ? (
                <tr><td colSpan={6} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">로그가 없습니다</td></tr>
              ) : (
                items.map((log) => (
                  <tr key={log.id} className="border-b border-gray-100 dark:border-gray-800 last:border-0">
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap">{formatTime(log.requestTime)}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500 dark:text-gray-400 truncate max-w-[160px]">{log.apiKeyValue}</td>
                    <td className="px-5 py-3 text-gray-700 dark:text-gray-300">{log.httpMethod}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-700 dark:text-gray-300">{log.requestPath}</td>
                    <td className={`px-5 py-3 ${statusColor(log.responseStatus)}`}>{log.responseStatus}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{log.clientIp}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && total > 0 && (
          <div className="flex items-center justify-between mt-4 text-sm text-gray-500 dark:text-gray-400">
            <span>총 {total.toLocaleString()}건 · {page + 1} / {totalPages} 페이지</span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 disabled:opacity-40 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
              >
                이전
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 disabled:opacity-40 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
              >
                다음
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
