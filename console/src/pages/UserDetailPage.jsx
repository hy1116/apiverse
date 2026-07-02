import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

const TIERS = ['FREE', 'PRO', 'ENTERPRISE']

export default function UserDetailPage() {
  const { id } = useParams()
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    client.get(`/admin/users/${id}`)
      .then((res) => setUser(res.data))
      .catch(() => setError('회원 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [id])

  const changeTier = async (tier) => {
    setSaving(true)
    try {
      const { data } = await client.patch(`/admin/users/${id}/tier`, { tier })
      setUser(data)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Link to="/users" className="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 transition-colors">
          ← 회원 목록으로
        </Link>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-6">불러오는 중...</p>
        ) : error || !user ? (
          <p className="text-sm text-red-500 dark:text-red-400 mt-6">{error || '회원을 찾을 수 없습니다.'}</p>
        ) : (
          <div className="mt-6 bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6 space-y-5">
            <div className="flex items-start justify-between">
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">회원 상세</h1>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                user.role === 'ADMIN'
                  ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400'
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'
              }`}>
                {user.role}
              </span>
            </div>

            <dl className="space-y-3 text-sm">
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">이메일</dt>
                <dd className="text-gray-900 dark:text-white font-medium">{user.email}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">회사명</dt>
                <dd className="text-gray-900 dark:text-white font-medium">{user.companyName ?? '-'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">전화번호</dt>
                <dd className="text-gray-900 dark:text-white font-medium">{user.phone ?? '-'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">가입일</dt>
                <dd className="text-gray-900 dark:text-white font-medium">
                  {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '-'}
                </dd>
              </div>
            </dl>

            <div>
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                등급
              </label>
              <select
                value={user.tier}
                disabled={saving}
                onChange={(e) => changeTier(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 disabled:opacity-50"
              >
                {TIERS.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
