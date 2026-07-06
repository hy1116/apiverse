import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function MyProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    client.get('/products/my')
      .then(({ data }) => setProducts(data))
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-start justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">내 등록 상품</h1>
            <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">내가 등록한 API 상품의 승인 진행 상황을 확인하세요.</p>
          </div>
          <Link
            to="/marketplace/register"
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm whitespace-nowrap"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
            API 등록
          </Link>
        </div>

        {loading ? (
          <div className="space-y-3">
            {[1, 2].map((i) => (
              <div key={i} className="h-20 bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 animate-pulse" />
            ))}
          </div>
        ) : products.length === 0 ? (
          <div className="text-center py-20">
            <p className="text-gray-400 dark:text-gray-600 mb-4">등록한 API 상품이 없습니다.</p>
            <Link to="/marketplace/register" className="text-sm font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors">
              첫 API 상품 등록하기 →
            </Link>
          </div>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                  <th className="px-5 py-3">이름</th>
                  <th className="px-5 py-3">카테고리</th>
                  <th className="px-5 py-3">상태</th>
                  <th className="px-5 py-3 text-right">Rate Limit</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr
                    key={p.id}
                    onClick={() => p.isActive && navigate(`/marketplace/${p.code}`)}
                    className={`border-b border-gray-100 dark:border-gray-800 last:border-0 transition-colors ${
                      p.isActive ? 'cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50' : ''
                    }`}
                  >
                    <td className="px-5 py-3 text-gray-900 dark:text-white font-medium">{p.name}</td>
                    <td className="px-5 py-3 text-gray-500 dark:text-gray-400">{p.category ?? '-'}</td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        p.isActive
                          ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
                          : 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
                      }`}>
                        {p.isActive ? '승인됨' : '승인 대기중'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-right text-gray-500 dark:text-gray-400">{p.callsPerSec} req/s</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
