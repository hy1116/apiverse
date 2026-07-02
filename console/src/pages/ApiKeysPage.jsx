import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function ApiKeysPage() {
  const [keys, setKeys] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    client.get('/admin/keys')
      .then((res) => setKeys(res.data))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const revoke = async (id) => {
    if (!window.confirm('이 API 키를 강제로 폐기하시겠습니까?')) return
    await client.delete(`/admin/keys/${id}`)
    load()
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-6">API 키 관리</h1>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">불러오는 중...</p>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                  <th className="px-5 py-3">회원</th>
                  <th className="px-5 py-3">API 상품</th>
                  <th className="px-5 py-3">키 값</th>
                  <th className="px-5 py-3">사용량</th>
                  <th className="px-5 py-3 text-right">작업</th>
                </tr>
              </thead>
              <tbody>
                {keys.length === 0 && (
                  <tr><td colSpan={5} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">발급된 키가 없습니다</td></tr>
                )}
                {keys.map((k) => (
                  <tr key={k.id} className="border-b border-gray-100 dark:border-gray-800 last:border-0">
                    <td className="px-5 py-3 text-gray-900 dark:text-white font-medium">{k.userEmail}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{k.apiProductName}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">
                      {k.apiKeyValue.slice(0, 22)}••••••••
                    </td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">
                      {k.usedQuota}{k.monthlyQuota === -1 ? ' / 무제한' : ` / ${k.monthlyQuota}`}
                    </td>
                    <td className="px-5 py-3 text-right">
                      <button
                        onClick={() => revoke(k.id)}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors"
                      >
                        폐기
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  )
}
