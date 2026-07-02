import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function ApiKeysPage() {
  const [keys, setKeys] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    client.get('/admin/keys')
      .then((res) => setKeys(res.data))
      .finally(() => setLoading(false))
  }, [])

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
                  <th className="px-5 py-3">허용 IP</th>
                  <th className="px-5 py-3">상태</th>
                </tr>
              </thead>
              <tbody>
                {keys.length === 0 && (
                  <tr><td colSpan={6} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">발급된 키가 없습니다</td></tr>
                )}
                {keys.map((k) => (
                  <tr
                    key={k.id}
                    onClick={() => navigate(`/keys/${k.id}`)}
                    className="border-b border-gray-100 dark:border-gray-800 last:border-0 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <td className="px-5 py-3 text-gray-900 dark:text-white font-medium">{k.userEmail}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{k.apiProductName}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">
                      {k.apiKeyValue.slice(0, 22)}••••••••
                    </td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">
                      {k.usedQuota}{k.monthlyQuota === -1 ? ' / 무제한' : ` / ${k.monthlyQuota}`}
                    </td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">
                      {k.whiteListIp || '제한 없음'}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        k.isActive
                          ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
                          : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'
                      }`}>
                        {k.isActive ? '활성' : '폐기됨'}
                      </span>
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
