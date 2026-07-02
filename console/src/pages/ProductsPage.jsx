import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function ProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const load = () => {
    setLoading(true)
    client.get('/admin/products')
      .then((res) => setProducts(res.data))
      .catch(() => setError('상품 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const approve = async (e, id) => {
    e.stopPropagation()
    await client.patch(`/admin/products/${id}/approve`)
    load()
  }

  const reject = async (e, id) => {
    e.stopPropagation()
    if (!window.confirm('이 상품을 반려(삭제)하시겠습니까?')) return
    await client.delete(`/admin/products/${id}/reject`)
    load()
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-6">API 상품 관리</h1>

        {error && <p className="text-sm text-red-500 dark:text-red-400 mb-4">{error}</p>}
        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">불러오는 중...</p>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                  <th className="px-5 py-3">이름</th>
                  <th className="px-5 py-3">code</th>
                  <th className="px-5 py-3">카테고리</th>
                  <th className="px-5 py-3">상태</th>
                  <th className="px-5 py-3">프리미엄</th>
                  <th className="px-5 py-3 text-right">작업</th>
                </tr>
              </thead>
              <tbody>
                {products.length === 0 && (
                  <tr><td colSpan={6} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">등록된 상품이 없습니다</td></tr>
                )}
                {products.map((p) => (
                  <tr
                    key={p.id}
                    onClick={() => navigate(`/products/${p.id}`)}
                    className="border-b border-gray-100 dark:border-gray-800 last:border-0 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <td className="px-5 py-3 text-gray-900 dark:text-white font-medium">{p.name}</td>
                    <td className="px-5 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">{p.code ?? '-'}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{p.category ?? '-'}</td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        p.isActive
                          ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
                          : 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
                      }`}>
                        {p.isActive ? '승인됨' : '대기중'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{p.isPremium ? '예' : '아니오'}</td>
                    <td className="px-5 py-3 text-right space-x-2">
                      {!p.isActive && (
                        <>
                          <button
                            onClick={(e) => approve(e, p.id)}
                            className="text-xs px-2.5 py-1 rounded-lg font-medium bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 dark:hover:bg-indigo-900/50 transition-colors"
                          >
                            승인
                          </button>
                          <button
                            onClick={(e) => reject(e, p.id)}
                            className="text-xs px-2.5 py-1 rounded-lg font-medium bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors"
                          >
                            반려
                          </button>
                        </>
                      )}
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
