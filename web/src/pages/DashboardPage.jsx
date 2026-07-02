import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import Navbar from '../components/Navbar.jsx'
import ApiCallInfoModal from '../components/ApiCallInfoModal.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { useTheme } from '../context/ThemeContext.jsx'
import client from '../api/client.js'

function CopyButton({ text }) {
  const [copied, setCopied] = useState(false)
  return (
    <button
      onClick={() => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
      className={`text-xs px-2.5 py-1 rounded-lg font-medium transition-colors ${
        copied
          ? 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-400'
          : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
      }`}
    >
      {copied ? '복사됨 ✓' : '복사'}
    </button>
  )
}

function StatCard({ label, value, sub, accent }) {
  const accents = {
    indigo: 'from-indigo-500 to-violet-600',
    green: 'from-emerald-500 to-teal-600',
    amber: 'from-amber-500 to-orange-600',
  }
  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5 overflow-hidden relative">
      <div className={`absolute top-0 right-0 w-24 h-24 bg-gradient-to-br ${accents[accent]} opacity-5 rounded-full -translate-y-8 translate-x-8`} />
      <p className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">{label}</p>
      <p className={`text-2xl font-bold ${
        accent === 'green' ? 'text-emerald-500 dark:text-emerald-400' :
        accent === 'amber' ? 'text-amber-500 dark:text-amber-400' :
        'text-gray-900 dark:text-white'
      }`}>{value}</p>
      {sub && <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">{sub}</p>}
    </div>
  )
}

function ApiKeyCard({ apiKey, onRevoke, onViewDetail }) {
  const [revealed, setRevealed] = useState(false)
  const masked = apiKey.apiKeyValue.slice(0, 22) + '••••••••••••'
  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5">
      <div className="flex items-start justify-between mb-4">
        <div>
          <button
            onClick={() => onViewDetail(apiKey.apiProductId)}
            className="block font-semibold text-gray-900 dark:text-white text-sm hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors text-left"
          >
            {apiKey.apiProductName}
          </button>
          <span className="inline-flex items-center gap-1 mt-2 text-xs px-2 py-0.5 bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800 rounded-full font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Sandbox · 활성
          </span>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => onViewDetail(apiKey.apiProductId)} className="text-xs text-indigo-500 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors">
            상세보기
          </button>
          <button onClick={() => onRevoke(apiKey.id)} className="text-xs text-gray-400 dark:text-gray-600 hover:text-red-500 dark:hover:text-red-400 transition-colors">
            폐기
          </button>
        </div>
      </div>

      <div className="flex items-center gap-2 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl px-3 py-2.5 font-mono text-xs text-gray-700 dark:text-gray-300 mb-4">
        <span className="flex-1 truncate">{revealed ? apiKey.apiKeyValue : masked}</span>
        <button onClick={() => setRevealed(!revealed)} className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 shrink-0 transition-colors">
          {revealed ? '숨기기' : '보기'}
        </button>
        <CopyButton text={apiKey.apiKeyValue} />
      </div>

      <div className="flex items-center justify-between">
        <div className="flex-1 mr-4">
          <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
            <span>이번 달 사용량</span>
            <span className="font-medium text-gray-700 dark:text-gray-300">
              {apiKey.usedQuota.toLocaleString()} {apiKey.monthlyQuota === -1 ? '/ 무제한' : `/ ${apiKey.monthlyQuota.toLocaleString()}`}
            </span>
          </div>
          <div className="h-1.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-indigo-500 to-violet-500 rounded-full"
              style={{ width: apiKey.monthlyQuota === -1 ? '20%' : `${Math.min((apiKey.usedQuota / apiKey.monthlyQuota) * 100, 100)}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg px-3 py-2.5 text-xs">
      <p className="font-semibold text-gray-900 dark:text-white mb-1">{label}</p>
      {payload.map((p) => (
        <p key={p.name} style={{ color: p.color }}>
          {p.name}: <span className="font-bold">{p.value}</span>
        </p>
      ))}
    </div>
  )
}

export default function DashboardPage() {
  const { user } = useAuth()
  const { theme } = useTheme()
  const navigate = useNavigate()
  const [apiKeys, setApiKeys] = useState([])
  const [products, setProducts] = useState([])
  const [productsLoading, setProductsLoading] = useState(true)
  const [usageData, setUsageData] = useState([])
  const [issuingFor, setIssuingFor] = useState(null)
  const [callInfoProduct, setCallInfoProduct] = useState(null)
  const isDark = theme === 'dark'

  useEffect(() => {
    client.get('/products')
      .then(({ data }) => setProducts(data))
      .catch(() => setProducts([]))
      .finally(() => setProductsLoading(false))
  }, [])

  useEffect(() => {
    client.get('/keys')
      .then(({ data }) => setApiKeys(data))
      .catch(() => setApiKeys([]))
  }, [])

  useEffect(() => {
    client.get('/usage/daily')
      .then(({ data }) => setUsageData(data))
      .catch(() => setUsageData([]))
  }, [])

  // usage/daily의 requests는 성공(status<500) 건만 집계하고 에러는 errors에 따로 집계되므로,
  // "총 요청"은 둘을 합산해야 실제 전체 호출 수가 된다.
  const successCount  = usageData.reduce((s, d) => s + Number(d.requests ?? 0), 0)
  const totalErrors   = usageData.reduce((s, d) => s + Number(d.errors ?? 0), 0)
  const totalRequests = successCount + totalErrors
  const successRate   = totalRequests > 0
    ? ((successCount / totalRequests) * 100).toFixed(1)
    : '0.0'

  const handleIssueKey = async (productId) => {
    setIssuingFor(productId)
    try {
      const { data } = await client.post('/keys', { apiProductId: productId })
      setApiKeys((prev) => [...prev, data])
    } catch {
      // CONFLICT means key already issued — refresh from server
      const { data } = await client.get('/keys').catch(() => ({ data: [] }))
      setApiKeys(data)
    } finally {
      setIssuingFor(null)
    }
  }

  const handleRevoke = async (keyId) => {
    if (window.confirm('이 API Key를 폐기하시겠습니까?')) {
      try {
        await client.delete(`/keys/${keyId}`)
        setApiKeys((prev) => prev.filter((k) => k.id !== keyId))
      } catch {
        setApiKeys((prev) => prev.filter((k) => k.id !== keyId))
      }
    }
  }

  const openCallInfo = (productId) => {
    const product = products.find((p) => p.id === productId)
    if (product) setCallInfoProduct(product)
  }

  const unconnected = products.filter(
    (p) => !p.isPremium && !apiKeys.find((k) => k.apiProductId === p.id)
  )

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">

        {/* 헤더 */}
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            안녕하세요, {user?.companyName} 👋
          </h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">
            <span className="font-medium text-indigo-600 dark:text-indigo-400">{user?.tier} 플랜</span> 사용 중
          </p>
        </div>

        {/* 통계 */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <StatCard label="7일간 총 요청" value={totalRequests.toLocaleString()} sub="건" accent="indigo" />
          <StatCard label="성공률" value={`${successRate}%`} sub={`에러 ${totalErrors}건`} accent="green" />
          <StatCard label="활성 API Key" value={apiKeys.length} sub={`${unconnected.length}개 미연동`} accent="amber" />
        </div>

        {/* 사용량 차트 */}
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-white mb-5">7일 API 호출량</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={usageData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1f2937' : '#f3f4f6'} vertical={false} />
              <XAxis dataKey="date" tick={{ fontSize: 11, fill: isDark ? '#6b7280' : '#9ca3af' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: isDark ? '#6b7280' : '#9ca3af' }} axisLine={false} tickLine={false} />
              <Tooltip content={<CustomTooltip />} cursor={{ fill: isDark ? '#ffffff08' : '#00000005' }} />
              <Bar dataKey="requests" name="성공" fill="#6366f1" radius={[6, 6, 0, 0]} maxBarSize={40} />
              <Bar dataKey="errors" name="에러" fill={isDark ? '#7f1d1d' : '#fecaca'} radius={[6, 6, 0, 0]} maxBarSize={40} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* API Keys */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-900 dark:text-white">내 Sandbox API Keys</h2>
            <button onClick={() => navigate('/marketplace')} className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline">
              API 마켓플레이스 →
            </button>
          </div>

          {apiKeys.length === 0 ? (
            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-300 dark:border-gray-700 p-10 text-center">
              <p className="text-gray-400 dark:text-gray-600 text-sm mb-3">아직 발급된 API Key가 없습니다.</p>
              <button onClick={() => navigate('/marketplace')} className="text-sm text-indigo-600 dark:text-indigo-400 font-semibold hover:underline">
                API 마켓플레이스에서 연동하기 →
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {apiKeys.map((key) => (
                <ApiKeyCard
                  key={key.id}
                  apiKey={key}
                  onRevoke={handleRevoke}
                  onViewDetail={openCallInfo}
                />
              ))}
            </div>
          )}
        </div>

        {/* 1-Click 연동 */}
        {productsLoading ? (
          <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6 text-center text-sm text-gray-400 dark:text-gray-600">
            API 목록 불러오는 중...
          </div>
        ) : unconnected.length > 0 && (
          <div>
            <h2 className="text-sm font-semibold text-gray-900 dark:text-white mb-4">무료 API 1-Click 연동</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {unconnected.map((product) => (
                <div key={product.id} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-4 flex items-center justify-between group hover:border-indigo-300 dark:hover:border-indigo-700 transition-colors">
                  <div>
                    <button
                      onClick={() => openCallInfo(product.id)}
                      className="text-sm font-medium text-gray-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors text-left"
                    >
                      {product.name}
                    </button>
                    <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">
                      {product.callsPerSec != null ? `${product.callsPerSec} req/s · ` : ''}무제한 쿼터
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => openCallInfo(product.id)}
                      className="px-3 py-1.5 text-xs font-medium text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800 rounded-lg hover:bg-indigo-50 dark:hover:bg-indigo-900/30 transition-colors"
                    >
                      상세보기
                    </button>
                    <button
                      onClick={() => handleIssueKey(product.id)}
                      disabled={issuingFor === product.id}
                      className="px-3 py-1.5 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-xs font-semibold rounded-lg transition-colors disabled:opacity-50"
                    >
                      {issuingFor === product.id ? '발급 중...' : '즉시 발급'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {callInfoProduct && (
        <ApiCallInfoModal
          product={callInfoProduct}
          apiKeyValue={apiKeys.find((k) => k.apiProductId === callInfoProduct.id)?.apiKeyValue ?? null}
          onClose={() => setCallInfoProduct(null)}
          onViewFull={() => {
            setCallInfoProduct(null)
            navigate(`/marketplace/${callInfoProduct.id}`)
          }}
        />
      )}
    </div>
  )
}
