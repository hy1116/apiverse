import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

function parseUpstreamKeyParam(param) {
  if (!param) return { type: 'header', name: '' }
  const [type, name] = param.split(':', 2)
  return { type: type === 'query' ? 'query' : 'header', name: name ?? '' }
}

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState(null)
  const [approving, setApproving] = useState(false)
  const [rejecting, setRejecting] = useState(false)

  useEffect(() => {
    client.get(`/admin/products/${id}`)
      .then((res) => {
        setProduct(res.data)
        const { type, name } = parseUpstreamKeyParam(res.data.upstreamKeyParam)
        setForm({
          description: res.data.description ?? '',
          baseUrl: res.data.baseUrl ?? '',
          category: res.data.category ?? '',
          callsPerSec: res.data.callsPerSec ?? 5,
          responseType: res.data.responseType ?? 'JSON',
          isPremium: !!res.data.isPremium,
          code: res.data.code ?? '',
          upstreamApiKey: res.data.upstreamApiKey ?? '',
          upstreamKeyType: type,
          upstreamKeyName: name,
        })
      })
      .catch(() => setError('상품 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [id])

  const save = async () => {
    setSaving(true)
    setSaved(false)
    try {
      const { data } = await client.patch(`/admin/products/${id}`, {
        description: form.description,
        baseUrl: form.baseUrl,
        category: form.category,
        callsPerSec: Number(form.callsPerSec),
        responseType: form.responseType,
        isPremium: form.isPremium,
        code: form.code,
        upstreamApiKey: form.upstreamApiKey || null,
        upstreamKeyParam: form.upstreamKeyName ? `${form.upstreamKeyType}:${form.upstreamKeyName}` : null,
      })
      setProduct(data)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } finally {
      setSaving(false)
    }
  }

  const approve = async () => {
    setApproving(true)
    try {
      const { data } = await client.patch(`/admin/products/${id}/approve`)
      setProduct(data)
    } finally {
      setApproving(false)
    }
  }

  const reject = async () => {
    if (!window.confirm('이 상품을 반려(삭제)하시겠습니까?')) return
    setRejecting(true)
    try {
      await client.delete(`/admin/products/${id}/reject`)
      navigate('/products')
    } finally {
      setRejecting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Link to="/products" className="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 transition-colors">
          ← 상품 목록으로
        </Link>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-6">불러오는 중...</p>
        ) : error || !product || !form ? (
          <p className="text-sm text-red-500 dark:text-red-400 mt-6">{error || '상품을 찾을 수 없습니다.'}</p>
        ) : (
          <div className="mt-6 bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6 space-y-5">
            <div className="flex items-start justify-between">
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">{product.name}</h1>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                product.isActive
                  ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
                  : 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
              }`}>
                {product.isActive ? '승인됨' : '대기중'}
              </span>
            </div>

            {!product.isActive && (
              <div className="flex gap-2">
                <button
                  onClick={approve}
                  disabled={approving || rejecting}
                  className="flex-1 py-2.5 text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 dark:hover:bg-indigo-900/50"
                >
                  {approving ? '승인 중...' : '승인'}
                </button>
                <button
                  onClick={reject}
                  disabled={approving || rejecting}
                  className="flex-1 py-2.5 text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/40"
                >
                  {rejecting ? '반려 중...' : '반려'}
                </button>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                code (게이트웨이 호출 경로: /gateway/{'{code}'}/**)
              </label>
              <input
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm font-mono text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                설명
              </label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={2}
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                업스트림 base URL
              </label>
              <input
                value={form.baseUrl}
                onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm font-mono text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                  카테고리
                </label>
                <input
                  value={form.category}
                  onChange={(e) => setForm({ ...form, category: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                  초당 호출 제한
                </label>
                <input
                  type="number"
                  value={form.callsPerSec}
                  onChange={(e) => setForm({ ...form, callsPerSec: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                  응답 타입
                </label>
                <select
                  value={form.responseType}
                  onChange={(e) => setForm({ ...form, responseType: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                >
                  <option value="JSON">JSON</option>
                  <option value="XML">XML</option>
                  <option value="TEXT">TEXT</option>
                </select>
              </div>
            </div>

            <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
              <input
                type="checkbox"
                checked={form.isPremium}
                onChange={(e) => setForm({ ...form, isPremium: e.target.checked })}
                className="rounded border-gray-300 dark:border-gray-700"
              />
              프리미엄 상품 (Pro 플랜 이상)
            </label>

            <div className="border-t border-gray-200 dark:border-gray-800 pt-5">
              <p className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-3">
                업스트림 API 키 (업스트림이 자체 인증을 요구하는 경우에만 입력)
              </p>
              <div className="space-y-3">
                <input
                  value={form.upstreamApiKey}
                  onChange={(e) => setForm({ ...form, upstreamApiKey: e.target.value })}
                  placeholder="업스트림에서 발급받은 키 값"
                  className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm font-mono text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                />
                <div className="flex gap-2">
                  <select
                    value={form.upstreamKeyType}
                    onChange={(e) => setForm({ ...form, upstreamKeyType: e.target.value })}
                    className="px-3 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                  >
                    <option value="header">헤더</option>
                    <option value="query">쿼리파라미터</option>
                  </select>
                  <input
                    value={form.upstreamKeyName}
                    onChange={(e) => setForm({ ...form, upstreamKeyName: e.target.value })}
                    placeholder={form.upstreamKeyType === 'query' ? '예: serviceKey' : '예: X-Api-Key'}
                    className="flex-1 px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm font-mono text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
                  />
                </div>
                <p className="text-xs text-gray-400 dark:text-gray-500">
                  프록시가 업스트림 호출 시 이 위치에 키 값을 실어 보냅니다. 비워두면 업스트림 키를 주입하지 않습니다.
                </p>
              </div>
            </div>

            <button
              onClick={save}
              disabled={saving}
              className={`w-full py-2.5 text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 ${
                saved
                  ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                  : 'bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white'
              }`}
            >
              {saving ? '저장 중...' : saved ? '저장됨 ✓' : '저장'}
            </button>
          </div>
        )}
      </main>
    </div>
  )
}
