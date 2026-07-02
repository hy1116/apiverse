import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

const DAY_OPTIONS = [
  { value: 1, label: '오늘' },
  { value: 7, label: '7일' },
  { value: 30, label: '30일' },
]

const TABS = [
  { key: 'errors', label: '에러' },
  { key: 'usage', label: '사용률' },
]

function statusColor(status) {
  if (status >= 500) return 'text-red-600 dark:text-red-400'
  if (status >= 400) return 'text-amber-600 dark:text-amber-400'
  return 'text-emerald-600 dark:text-emerald-400'
}

function pct(numerator, denominator) {
  if (!denominator) return '0.0%'
  return `${((numerator / denominator) * 100).toFixed(1)}%`
}

function RankingTable({ columns, rows, rowKey, emptyLabel }) {
  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-200 dark:border-gray-800 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            {columns.map((col) => (
              <th key={col.key} className={`px-5 py-3 ${col.align === 'right' ? 'text-right' : ''}`}>{col.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr><td colSpan={columns.length} className="px-5 py-6 text-center text-gray-400 dark:text-gray-500">{emptyLabel}</td></tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)} className="border-b border-gray-100 dark:border-gray-800 last:border-0">
                {columns.map((col) => (
                  <td key={col.key} className={`px-5 py-3 ${col.className ?? ''} ${col.align === 'right' ? 'text-right' : ''}`}>
                    {col.render ? col.render(row) : row[col.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

export default function StatsPage() {
  const [tab, setTab] = useState('errors')
  const [days, setDays] = useState(7)

  const [productErrorStats, setProductErrorStats] = useState([])
  const [topErrorKeys, setTopErrorKeys] = useState([])
  const [statusStats, setStatusStats] = useState([])

  const [productUsageStats, setProductUsageStats] = useState([])
  const [topUsageKeys, setTopUsageKeys] = useState([])
  const [quotaUsage, setQuotaUsage] = useState([])

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    Promise.all([
      client.get('/admin/stats/products-errors', { params: { days } }),
      client.get('/admin/stats/top-error-keys', { params: { days, limit: 20 } }),
      client.get('/admin/stats/status-codes', { params: { days } }),
      client.get('/admin/stats/products-usage', { params: { days } }),
      client.get('/admin/stats/top-usage-keys', { params: { days, limit: 20 } }),
      client.get('/admin/stats/quota-usage', { params: { limit: 20 } }),
    ])
      .then(([productErrorsRes, topErrorKeysRes, statusRes, productUsageRes, topUsageKeysRes, quotaRes]) => {
        setProductErrorStats(productErrorsRes.data)
        setTopErrorKeys(topErrorKeysRes.data)
        setStatusStats(statusRes.data)
        setProductUsageStats(productUsageRes.data)
        setTopUsageKeys(topUsageKeysRes.data)
        setQuotaUsage(quotaRes.data)
      })
      .catch(() => setError('통계를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [days])

  const statusTotal = statusStats.reduce((sum, s) => sum + Number(s.count), 0)

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <h1 className="text-xl font-bold text-gray-900 dark:text-white">통계</h1>
            <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-800/60 p-1 rounded-xl border border-gray-200 dark:border-gray-700">
              {TABS.map((t) => (
                <button
                  key={t.key}
                  onClick={() => setTab(t.key)}
                  className={`px-3.5 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                    tab === t.key
                      ? 'bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-sm border border-gray-200 dark:border-gray-700'
                      : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>
          <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-800/60 p-1 rounded-xl border border-gray-200 dark:border-gray-700">
            {DAY_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setDays(opt.value)}
                className={`px-3.5 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  days === opt.value
                    ? 'bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-sm border border-gray-200 dark:border-gray-700'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {error && <p className="text-sm text-red-500 dark:text-red-400 mb-4">{error}</p>}

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">불러오는 중...</p>
        ) : tab === 'errors' ? (
          <div className="space-y-8">

            {/* 상품별 에러율 랭킹 */}
            <section>
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">상품별 에러율 랭킹</h2>
              <RankingTable
                emptyLabel="데이터가 없습니다"
                rowKey={(s) => s.productCode}
                rows={productErrorStats}
                columns={[
                  { key: 'productName', label: '상품' },
                  { key: 'productCode', label: 'code', className: 'font-mono text-xs text-gray-500 dark:text-gray-400' },
                  { key: 'totalRequests', label: '전체 요청', align: 'right', render: (s) => Number(s.totalRequests).toLocaleString() },
                  { key: 'errorCount', label: '에러', align: 'right', className: 'text-red-500 dark:text-red-400' },
                  { key: 'errorRate', label: '에러율', align: 'right', render: (s) => pct(s.errorCount, s.totalRequests) },
                ]}
              />
            </section>

            {/* 에러 많은 API 키 Top N */}
            <section>
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">에러 많은 API 키 Top 20</h2>
              <RankingTable
                emptyLabel="에러가 없습니다"
                rowKey={(s) => s.apiKeyValue}
                rows={topErrorKeys}
                columns={[
                  { key: 'apiKeyValue', label: 'API Key', className: 'font-mono text-xs text-gray-500 dark:text-gray-400 truncate max-w-[160px]' },
                  { key: 'userEmail', label: '회원' },
                  { key: 'productName', label: '상품', className: 'text-gray-500 dark:text-gray-400' },
                  { key: 'totalRequests', label: '전체 요청', align: 'right', render: (s) => Number(s.totalRequests).toLocaleString() },
                  { key: 'errorCount', label: '에러', align: 'right', className: 'text-red-500 dark:text-red-400' },
                ]}
              />
            </section>

            {/* 상태코드 분포 */}
            <section>
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">상태코드 분포</h2>
              <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5 space-y-3">
                {statusStats.length === 0 ? (
                  <p className="text-sm text-center text-gray-400 dark:text-gray-500">데이터가 없습니다</p>
                ) : (
                  statusStats.map((s) => {
                    const barPct = statusTotal > 0 ? (Number(s.count) / statusTotal) * 100 : 0
                    return (
                      <div key={s.responseStatus} className="flex items-center gap-3">
                        <span className={`w-12 text-sm font-mono font-semibold ${statusColor(s.responseStatus)}`}>{s.responseStatus}</span>
                        <div className="flex-1 h-2 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full ${s.responseStatus >= 500 ? 'bg-red-500' : s.responseStatus >= 400 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                            style={{ width: `${barPct}%` }}
                          />
                        </div>
                        <span className="w-28 text-right text-sm text-gray-500 dark:text-gray-400">{Number(s.count).toLocaleString()}건 ({barPct.toFixed(1)}%)</span>
                      </div>
                    )
                  })
                )}
              </div>
            </section>
          </div>
        ) : (
          <div className="space-y-8">

            {/* 상품별 사용량 랭킹 */}
            <section>
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">상품별 사용량 랭킹</h2>
              <RankingTable
                emptyLabel="데이터가 없습니다"
                rowKey={(s) => s.productCode}
                rows={productUsageStats}
                columns={[
                  { key: 'productName', label: '상품' },
                  { key: 'productCode', label: 'code', className: 'font-mono text-xs text-gray-500 dark:text-gray-400' },
                  { key: 'totalRequests', label: '전체 요청', align: 'right', className: 'text-gray-900 dark:text-white font-medium', render: (s) => Number(s.totalRequests).toLocaleString() },
                  { key: 'errorCount', label: '에러', align: 'right', className: 'text-gray-500 dark:text-gray-400' },
                ]}
              />
            </section>

            {/* 사용량 많은 API 키 Top N */}
            <section>
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">사용량 많은 API 키 Top 20</h2>
              <RankingTable
                emptyLabel="데이터가 없습니다"
                rowKey={(s) => s.apiKeyValue}
                rows={topUsageKeys}
                columns={[
                  { key: 'apiKeyValue', label: 'API Key', className: 'font-mono text-xs text-gray-500 dark:text-gray-400 truncate max-w-[160px]' },
                  { key: 'userEmail', label: '회원' },
                  { key: 'productName', label: '상품', className: 'text-gray-500 dark:text-gray-400' },
                  { key: 'totalRequests', label: '전체 요청', align: 'right', className: 'text-gray-900 dark:text-white font-medium', render: (s) => Number(s.totalRequests).toLocaleString() },
                  { key: 'errorCount', label: '에러', align: 'right', className: 'text-gray-500 dark:text-gray-400' },
                ]}
              />
            </section>

            {/* 쿼터 사용률 Top N */}
            <section>
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1">쿼터 사용률 Top 20</h2>
              <p className="text-xs text-gray-400 dark:text-gray-600 mb-3">월간 한도(monthlyQuota)가 무제한(-1)인 키는 제외됩니다.</p>
              <RankingTable
                emptyLabel="쿼터가 설정된 키가 없습니다"
                rowKey={(s) => s.apiKeyValue}
                rows={quotaUsage}
                columns={[
                  { key: 'apiKeyValue', label: 'API Key', className: 'font-mono text-xs text-gray-500 dark:text-gray-400 truncate max-w-[160px]' },
                  { key: 'userEmail', label: '회원' },
                  { key: 'productName', label: '상품', className: 'text-gray-500 dark:text-gray-400' },
                  { key: 'usedQuota', label: '사용량 / 한도', align: 'right', render: (s) => `${s.usedQuota.toLocaleString()} / ${s.monthlyQuota.toLocaleString()}` },
                  {
                    key: 'usageRate', label: '사용률', align: 'right',
                    className: 'font-semibold',
                    render: (s) => {
                      const rate = (s.usedQuota / s.monthlyQuota) * 100
                      const color = rate >= 90 ? 'text-red-500 dark:text-red-400' : rate >= 70 ? 'text-amber-500 dark:text-amber-400' : 'text-gray-500 dark:text-gray-400'
                      return <span className={color}>{rate.toFixed(1)}%</span>
                    },
                  },
                ]}
              />
            </section>
          </div>
        )}
      </main>
    </div>
  )
}
