import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

const CATEGORY_META = {
  Weather:     { label: '날씨',    color: 'bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-800' },
  Location:    { label: '위치/주소', color: 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800' },
  Tourism:     { label: '관광',    color: 'bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800' },
  Finance:     { label: '금융',    color: 'bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300 border-purple-200 dark:border-purple-800' },
  Government:  { label: '공공',    color: 'bg-teal-50 dark:bg-teal-900/20 text-teal-700 dark:text-teal-300 border-teal-200 dark:border-teal-800' },
}

function ApiProductCard({ product }) {
  const navigate = useNavigate()
  const meta = CATEGORY_META[product.category] || { label: product.category, color: 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 border-gray-200 dark:border-gray-700' }

  return (
    <button
      onClick={() => navigate(`/marketplace/${product.code}`)}
      className="text-left bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5 hover:border-indigo-300 dark:hover:border-indigo-700 hover:shadow-md dark:hover:shadow-black/30 transition-all group"
    >
      <div className="flex items-start justify-between mb-3">
        <span className={`text-xs px-2.5 py-0.5 rounded-full font-medium border ${meta.color}`}>
          {meta.label}
        </span>
        {product.isPremium ? (
          <span className="text-xs px-2 py-0.5 bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 border border-amber-200 dark:border-amber-800 rounded-full font-medium">Pro+</span>
        ) : (
          <span className="text-xs px-2 py-0.5 bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800 rounded-full font-medium">무료</span>
        )}
      </div>

      <h3 className="font-semibold text-gray-900 dark:text-white mb-1.5 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
        {product.name}
      </h3>
      <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-4 leading-relaxed">
        {product.description}
      </p>

      <div className="flex items-center justify-between text-xs">
        <code className="text-gray-400 dark:text-gray-600 truncate max-w-[70%]">{product.baseUrl}</code>
        <span className="text-gray-400 dark:text-gray-600 shrink-0">{product.callsPerSec} req/s</span>
      </div>
    </button>
  )
}

export default function MarketplacePage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all')

  useEffect(() => {
    client.get('/products')
      .then(({ data }) => setProducts(data))
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [])

  const filtered = products.filter((p) => {
    const matchSearch = p.name.toLowerCase().includes(search.toLowerCase()) || p.description.toLowerCase().includes(search.toLowerCase())
    const matchFilter = filter === 'all' || (filter === 'free' && !p.isPremium) || (filter === 'premium' && p.isPremium)
    return matchSearch && matchFilter
  })

  const tabs = [
    { key: 'all', label: '전체', count: products.length },
    { key: 'free', label: '무료', count: products.filter((p) => !p.isPremium).length },
    { key: 'premium', label: '유료', count: products.filter((p) => p.isPremium).length },
  ]

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        <div className="flex items-start justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">API 마켓플레이스</h1>
            <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">가입 즉시 Sandbox 키를 발급받아 테스트하세요.</p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Link
              to="/marketplace/my"
              className="flex items-center gap-2 px-4 py-2 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 text-sm font-semibold rounded-xl transition-colors whitespace-nowrap"
            >
              내 등록 상품
            </Link>
            <Link
              to="/marketplace/register"
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm whitespace-nowrap"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              API 등록
            </Link>
          </div>
        </div>

        {/* 검색 + 필터 */}
        <div className="flex flex-col sm:flex-row gap-3 mb-6">
          <div className="relative flex-1">
            <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="API 이름, 설명으로 검색..."
              className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent"
            />
          </div>
          <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-800/60 p-1 rounded-xl border border-gray-200 dark:border-gray-700">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setFilter(tab.key)}
                className={`px-3.5 py-1.5 text-sm font-medium rounded-lg transition-colors flex items-center gap-1.5 ${
                  filter === tab.key
                    ? 'bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-sm border border-gray-200 dark:border-gray-700'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                }`}
              >
                {tab.label}
                <span className={`text-xs ${filter === tab.key ? 'text-indigo-500 dark:text-indigo-400' : 'text-gray-400 dark:text-gray-600'}`}>
                  {tab.count}
                </span>
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5 animate-pulse">
                <div className="h-4 bg-gray-100 dark:bg-gray-800 rounded w-1/3 mb-3" />
                <div className="h-5 bg-gray-100 dark:bg-gray-800 rounded w-2/3 mb-2" />
                <div className="h-3 bg-gray-100 dark:bg-gray-800 rounded w-full mb-1" />
                <div className="h-3 bg-gray-100 dark:bg-gray-800 rounded w-4/5" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-20">
            <p className="text-gray-400 dark:text-gray-600">
              {products.length === 0 ? 'API 목록을 불러올 수 없습니다.' : '검색 결과가 없습니다.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filtered.map((product) => <ApiProductCard key={product.id} product={product} />)}
          </div>
        )}
      </div>
    </div>
  )
}
