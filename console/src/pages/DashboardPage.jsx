import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

function StatCard({ label, value, accent = 'indigo' }) {
  const accents = {
    indigo: 'text-gray-900 dark:text-white',
    amber: 'text-amber-500 dark:text-amber-400',
    red: 'text-red-500 dark:text-red-400',
  }
  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5">
      <p className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">{label}</p>
      <p className={`text-2xl font-bold ${accents[accent]}`}>{value}</p>
    </div>
  )
}

export default function DashboardPage() {
  const [stats, setStats] = useState([])
  const [pendingCount, setPendingCount] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      client.get('/admin/usage/daily'),
      client.get('/admin/products/pending'),
    ])
      .then(([usageRes, pendingRes]) => {
        setStats(usageRes.data)
        setPendingCount(pendingRes.data.length)
      })
      .finally(() => setLoading(false))
  }, [])

  const totalRequests = stats.reduce((sum, s) => sum + s.requests, 0)
  const totalErrors = stats.reduce((sum, s) => sum + s.errors, 0)

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-6">대시보드</h1>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">불러오는 중...</p>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
              <StatCard label="승인 대기 상품" value={pendingCount} accent="amber" />
              <StatCard label="최근 7일 요청 수" value={totalRequests.toLocaleString()} />
              <StatCard label="최근 7일 에러 수" value={totalErrors.toLocaleString()} accent="red" />
            </div>

            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                    <th className="px-5 py-3">날짜</th>
                    <th className="px-5 py-3">요청 수</th>
                    <th className="px-5 py-3">에러 수</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.length === 0 && (
                    <tr><td colSpan={3} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">데이터가 없습니다</td></tr>
                  )}
                  {stats.map((s) => (
                    <tr key={s.date} className="border-b border-gray-100 dark:border-gray-800 last:border-0">
                      <td className="px-5 py-3 text-gray-700 dark:text-gray-300">{s.date}</td>
                      <td className="px-5 py-3 text-gray-900 dark:text-white font-medium">{s.requests}</td>
                      <td className="px-5 py-3 text-red-500 dark:text-red-400">{s.errors}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </main>
    </div>
  )
}
