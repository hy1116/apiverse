import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

const TIERS = ['FREE', 'PRO', 'ENTERPRISE']

export default function UsersPage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    client.get('/admin/users')
      .then((res) => setUsers(res.data))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const changeTier = async (id, tier) => {
    await client.patch(`/admin/users/${id}/tier`, { tier })
    load()
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-6">회원 관리</h1>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">불러오는 중...</p>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                  <th className="px-5 py-3">이메일</th>
                  <th className="px-5 py-3">회사명</th>
                  <th className="px-5 py-3">가입일</th>
                  <th className="px-5 py-3">권한</th>
                  <th className="px-5 py-3">등급</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 && (
                  <tr><td colSpan={5} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">회원이 없습니다</td></tr>
                )}
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-gray-100 dark:border-gray-800 last:border-0">
                    <td className="px-5 py-3 text-gray-900 dark:text-white font-medium">{u.email}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{u.companyName ?? '-'}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        u.role === 'ADMIN'
                          ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400'
                          : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'
                      }`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <select
                        value={u.tier}
                        onChange={(e) => changeTier(u.id, e.target.value)}
                        className="px-2.5 py-1.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg text-xs text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                      >
                        {TIERS.map((t) => <option key={t} value={t}>{t}</option>)}
                      </select>
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
