import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function ApiKeyDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [key, setKey] = useState(null)
  const [loading, setLoading] = useState(true)
  const [revoking, setRevoking] = useState(false)
  const [error, setError] = useState('')
  const [whiteListIpInput, setWhiteListIpInput] = useState('')
  const [savingIp, setSavingIp] = useState(false)
  const [ipSaved, setIpSaved] = useState(false)

  const [unlimited, setUnlimited] = useState(true)
  const [quotaInput, setQuotaInput] = useState(0)
  const [savingQuota, setSavingQuota] = useState(false)
  const [quotaSaved, setQuotaSaved] = useState(false)
  const [quotaError, setQuotaError] = useState('')

  useEffect(() => {
    client.get(`/admin/keys/${id}`)
      .then((res) => {
        setKey(res.data)
        setWhiteListIpInput(res.data.whiteListIp ?? '')
        setUnlimited(res.data.monthlyQuota === -1)
        setQuotaInput(res.data.monthlyQuota === -1 ? 0 : res.data.monthlyQuota)
      })
      .catch(() => setError('키 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [id])

  const saveWhiteListIp = async () => {
    setSavingIp(true)
    setIpSaved(false)
    try {
      const { data } = await client.patch(`/admin/keys/${id}/whitelist-ip`, { whiteListIp: whiteListIpInput })
      setKey(data)
      setIpSaved(true)
      setTimeout(() => setIpSaved(false), 2000)
    } finally {
      setSavingIp(false)
    }
  }

  const saveQuota = async () => {
    setSavingQuota(true)
    setQuotaSaved(false)
    setQuotaError('')
    try {
      const { data } = await client.patch(`/admin/keys/${id}/quota`, {
        monthlyQuota: unlimited ? -1 : Number(quotaInput),
      })
      setKey(data)
      setQuotaSaved(true)
      setTimeout(() => setQuotaSaved(false), 2000)
    } catch {
      setQuotaError('쿼터 저장에 실패했습니다.')
    } finally {
      setSavingQuota(false)
    }
  }

  const revoke = async () => {
    if (!window.confirm('이 API 키를 강제로 폐기하시겠습니까?')) return
    setRevoking(true)
    try {
      await client.delete(`/admin/keys/${id}`)
      navigate('/keys')
    } finally {
      setRevoking(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Link to="/keys" className="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 transition-colors">
          ← API 키 목록으로
        </Link>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-6">불러오는 중...</p>
        ) : error || !key ? (
          <p className="text-sm text-red-500 dark:text-red-400 mt-6">{error || '키를 찾을 수 없습니다.'}</p>
        ) : (
          <div className="mt-6 bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6 space-y-5">
            <div className="flex items-start justify-between">
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">API 키 상세</h1>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                key.isActive
                  ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'
              }`}>
                {key.isActive ? '활성' : '폐기됨'}
              </span>
            </div>

            <dl className="space-y-3 text-sm">
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">회원</dt>
                <dd className="text-gray-900 dark:text-white font-medium">{key.userEmail}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">API 상품</dt>
                <dd className="text-gray-900 dark:text-white font-medium">{key.apiProductName}</dd>
              </div>
              <div className="flex justify-between items-center">
                <dt className="text-gray-500 dark:text-gray-400">키 값</dt>
                <dd className="font-mono text-xs text-gray-700 dark:text-gray-300">{key.apiKeyValue}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">사용량</dt>
                <dd className="text-gray-900 dark:text-white font-medium">
                  {key.usedQuota}{key.monthlyQuota === -1 ? ' / 무제한' : ` / ${key.monthlyQuota}`}
                </dd>
              </div>
            </dl>

            <div className="border-t border-gray-200 dark:border-gray-800 pt-5">
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                허용 IP (화이트리스트)
              </label>
              <div className="flex gap-2">
                <input
                  value={whiteListIpInput}
                  onChange={(e) => setWhiteListIpInput(e.target.value)}
                  placeholder="예: 1.2.3.4,5.6.7.8 (비우면 제한 없음)"
                  className="flex-1 px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm font-mono text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                />
                <button
                  onClick={saveWhiteListIp}
                  disabled={savingIp}
                  className={`px-4 py-2.5 text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 shrink-0 ${
                    ipSaved
                      ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                      : 'bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white'
                  }`}
                >
                  {savingIp ? '저장 중...' : ipSaved ? '저장됨 ✓' : '저장'}
                </button>
              </div>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1.5">
                등록된 IP 외에서 이 키로 요청하면 게이트웨이가 403으로 거부합니다. 여러 IP는 콤마로 구분하세요.
              </p>
            </div>

            <div className="border-t border-gray-200 dark:border-gray-800 pt-5">
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                월간 쿼터
              </label>
              <div className="flex items-center gap-3 mb-2">
                <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                  <input
                    type="checkbox"
                    checked={unlimited}
                    onChange={(e) => setUnlimited(e.target.checked)}
                    className="rounded border-gray-300 dark:border-gray-700"
                  />
                  무제한
                </label>
              </div>
              <div className="flex gap-2">
                <input
                  type="number"
                  min={0}
                  value={quotaInput}
                  onChange={(e) => setQuotaInput(e.target.value)}
                  disabled={unlimited}
                  placeholder="월간 허용 호출 수"
                  className="flex-1 px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 disabled:opacity-50"
                />
                <button
                  onClick={saveQuota}
                  disabled={savingQuota}
                  className={`px-4 py-2.5 text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 shrink-0 ${
                    quotaSaved
                      ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                      : 'bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white'
                  }`}
                >
                  {savingQuota ? '저장 중...' : quotaSaved ? '저장됨 ✓' : '저장'}
                </button>
              </div>
              {quotaError && <p className="text-xs text-red-500 dark:text-red-400 mt-1.5">{quotaError}</p>}
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1.5">
                초과 시 게이트웨이가 429(Too Many Requests)로 거부합니다. 매월 1일 자정에 사용량이 초기화됩니다.
              </p>
            </div>

            {key.isActive && (
              <button
                onClick={revoke}
                disabled={revoking}
                className="w-full py-2.5 bg-red-600 hover:bg-red-700 dark:bg-red-500 dark:hover:bg-red-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-50"
              >
                {revoking ? '폐기 중...' : '이 API 키 폐기'}
              </button>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
