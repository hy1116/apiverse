import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function BlockedIpsPage() {
  const [ips, setIps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [ipAddress, setIpAddress] = useState('')
  const [reason, setReason] = useState('')
  const [adding, setAdding] = useState(false)
  const [addError, setAddError] = useState('')

  const load = () => {
    setLoading(true)
    client.get('/admin/blocked-ips')
      .then(({ data }) => setIps(data))
      .catch(() => setError('차단 IP 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const add = async (e) => {
    e.preventDefault()
    if (!ipAddress.trim()) return
    setAdding(true)
    setAddError('')
    try {
      await client.post('/admin/blocked-ips', { ipAddress: ipAddress.trim(), reason: reason.trim() || null })
      setIpAddress('')
      setReason('')
      load()
    } catch (err) {
      setAddError(err.response?.data?.detail ?? '차단 IP 추가에 실패했습니다.')
    } finally {
      setAdding(false)
    }
  }

  const remove = async (id) => {
    if (!window.confirm('이 IP의 차단을 해제하시겠습니까?')) return
    await client.delete(`/admin/blocked-ips/${id}`)
    setIps((prev) => prev.filter((ip) => ip.id !== id))
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-1">차단 IP 관리</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
          여기 등록된 IP는 API 키/상품과 무관하게 <code className="bg-gray-100 dark:bg-gray-800 px-1 rounded">/gateway/**</code> 전체 요청이 차단됩니다.
        </p>

        <form onSubmit={add} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5 mb-6 space-y-3">
          <div className="flex gap-3">
            <input
              value={ipAddress}
              onChange={(e) => setIpAddress(e.target.value)}
              placeholder="차단할 IP (예: 1.2.3.4)"
              className="flex-1 px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm font-mono text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
            />
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="사유 (선택)"
              className="flex-1 px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
            />
            <button
              type="submit"
              disabled={adding || !ipAddress.trim()}
              className="px-5 py-2.5 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 whitespace-nowrap"
            >
              {adding ? '추가 중...' : '차단 추가'}
            </button>
          </div>
          {addError && <p className="text-sm text-red-500 dark:text-red-400">{addError}</p>}
        </form>

        {error && <p className="text-sm text-red-500 dark:text-red-400 mb-4">{error}</p>}

        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                <th className="px-5 py-3">IP</th>
                <th className="px-5 py-3">사유</th>
                <th className="px-5 py-3">등록일</th>
                <th className="px-5 py-3 text-right">작업</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={4} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">불러오는 중...</td></tr>
              ) : ips.length === 0 ? (
                <tr><td colSpan={4} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">차단된 IP가 없습니다</td></tr>
              ) : (
                ips.map((ip) => (
                  <tr key={ip.id} className="border-b border-gray-100 dark:border-gray-800 last:border-0">
                    <td className="px-5 py-3 font-mono text-gray-900 dark:text-white">{ip.ipAddress}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{ip.reason ?? '-'}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{ip.createdAt?.replace('T', ' ').slice(0, 19)}</td>
                    <td className="px-5 py-3 text-right">
                      <button
                        onClick={() => remove(ip.id)}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors"
                      >
                        차단 해제
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  )
}
