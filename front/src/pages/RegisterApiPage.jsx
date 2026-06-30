import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import SwaggerUI from 'swagger-ui-react'
import 'swagger-ui-react/swagger-ui.css'
import Navbar from '../components/Navbar.jsx'

const CATEGORIES = ['Weather', 'Location', 'Finance', 'Tourism', 'Government', 'AI/ML', 'Communication', 'IoT', 'Other']
const STEPS = ['스펙 URL 입력', '상품 정보 작성', '등록 완료']

function StepIndicator({ current }) {
  return (
    <div className="flex items-center gap-2 mb-8">
      {STEPS.map((label, i) => {
        const step = i + 1
        const done = step < current
        const active = step === current
        return (
          <div key={step} className="flex items-center gap-2">
            <div className="flex items-center gap-2">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all
                ${active ? 'border-indigo-500 bg-indigo-500 text-white' :
                  done ? 'border-emerald-500 bg-emerald-500 text-white' :
                  'border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-400 dark:text-gray-600'}`}>
                {done ? '✓' : step}
              </div>
              <span className={`text-sm font-medium hidden sm:block ${active ? 'text-gray-900 dark:text-white' : done ? 'text-emerald-600 dark:text-emerald-400' : 'text-gray-400 dark:text-gray-600'}`}>
                {label}
              </span>
            </div>
            {i < STEPS.length - 1 && (
              <div className={`h-px w-8 mx-1 ${done ? 'bg-emerald-400' : 'bg-gray-200 dark:bg-gray-800'}`} />
            )}
          </div>
        )
      })}
    </div>
  )
}

function InputClass() {
  return 'w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent'
}

function SpecUrlStep({ onSuccess }) {
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [corsBlocked, setCorsBlocked] = useState(false)
  const [manualJson, setManualJson] = useState('')

  const validate = (data) => {
    if (!data.openapi && !data.swagger) throw new Error('OpenAPI / Swagger 스펙이 아닙니다. (openapi 또는 swagger 필드가 없음)')
    if (!data.info?.title) throw new Error('스펙에 info.title 이 없습니다.')
  }

  const extractForm = (data) => ({
    name: data.info?.title || '',
    description: data.info?.description || '',
    baseUrl: data.servers?.[0]?.url || (data.host ? `${data.schemes?.[0] ?? 'https'}://${data.host}${data.basePath ?? ''}` : ''),
    category: '', callsPerSec: 5, isPremium: false,
  })

  const handleFetch = async () => {
    if (!url.trim()) { setError('URL을 입력해주세요.'); return }
    setLoading(true); setError(''); setCorsBlocked(false)
    try {
      const res = await fetch(url)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      validate(data)
      onSuccess({ spec: data, form: extractForm(data), specUrl: url })
    } catch (err) {
      if (err instanceof TypeError) {
        setCorsBlocked(true)
        setError('브라우저 CORS 정책으로 직접 불러올 수 없습니다. 실서비스에서는 백엔드가 프록시합니다.')
      } else {
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleManualParse = () => {
    setError('')
    try {
      const data = JSON.parse(manualJson)
      validate(data)
      onSuccess({ spec: data, form: extractForm(data), specUrl: url || '직접 입력' })
    } catch (err) {
      setError(err instanceof SyntaxError ? 'JSON 형식이 올바르지 않습니다.' : err.message)
    }
  }

  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-8">
      <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-1">Swagger / OpenAPI 스펙 URL 입력</h2>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
        등록하려는 API의 <code className="bg-gray-100 dark:bg-gray-800 px-1.5 py-0.5 rounded text-xs">/v3/api-docs</code> 또는{' '}
        <code className="bg-gray-100 dark:bg-gray-800 px-1.5 py-0.5 rounded text-xs">swagger.json</code> URL을 입력하세요.
        Swagger 문서가 없는 API는 등록이 불가합니다.
      </p>

      <div className="flex gap-2 mb-4">
        <input
          type="url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleFetch()}
          placeholder="https://your-api.com/v3/api-docs"
          className={InputClass()}
        />
        <button
          onClick={handleFetch}
          disabled={loading}
          className="px-5 py-2.5 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 whitespace-nowrap shadow-sm"
        >
          {loading ? '불러오는 중...' : '스펙 불러오기'}
        </button>
      </div>

      {error && (
        <div className="mb-4 px-4 py-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl text-sm text-red-600 dark:text-red-400">
          {error}
        </div>
      )}

      {corsBlocked && (
        <div className="mt-4 border border-dashed border-gray-300 dark:border-gray-700 rounded-2xl p-5">
          <p className="text-sm font-semibold text-gray-900 dark:text-white mb-1">스펙 JSON 직접 붙여넣기</p>
          <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">
            API 서버에서 <code className="bg-gray-100 dark:bg-gray-800 px-1 rounded">/v3/api-docs</code> 접속 후 JSON을 복사해 붙여넣으세요.
          </p>
          <textarea
            value={manualJson}
            onChange={(e) => setManualJson(e.target.value)}
            rows={8}
            placeholder='{"openapi": "3.0.0", "info": {"title": "...", ...}, ...}'
            className="w-full px-3.5 py-3 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl font-mono text-xs text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 resize-none"
          />
          <button
            onClick={handleManualParse}
            disabled={!manualJson.trim()}
            className="mt-3 px-4 py-2 bg-gray-900 dark:bg-gray-700 hover:bg-gray-800 dark:hover:bg-gray-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-40 shadow-sm"
          >
            스펙 검증 및 다음 단계 →
          </button>
        </div>
      )}

      <div className="mt-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800 rounded-xl text-xs text-blue-700 dark:text-blue-300 space-y-1.5">
        <p className="font-semibold">지원 형식</p>
        <p>· OpenAPI 3.x: <code className="bg-white dark:bg-blue-900/30 px-1 rounded">/v3/api-docs</code> (Spring Boot 기본 경로)</p>
        <p>· Swagger 2.0: <code className="bg-white dark:bg-blue-900/30 px-1 rounded">/v2/api-docs</code> 또는 <code className="bg-white dark:bg-blue-900/30 px-1 rounded">swagger.json</code></p>
      </div>
    </div>
  )
}

function ProductInfoStep({ spec, form, onFormChange, onSubmit, loading }) {
  const [previewOpen, setPreviewOpen] = useState(false)

  return (
    <div className="space-y-4">
      <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
        <button
          onClick={() => setPreviewOpen(!previewOpen)}
          className="w-full flex items-center justify-between px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
        >
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-gray-900 dark:text-white">Swagger UI 미리보기</span>
            <span className="text-xs px-2 py-0.5 bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800 rounded-full font-medium">검증 완료 ✓</span>
          </div>
          <svg className={`w-4 h-4 text-gray-400 transition-transform ${previewOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        {previewOpen && (
          <div className="border-t border-gray-200 dark:border-gray-800 max-h-[480px] overflow-y-auto scrollbar-hide">
            <SwaggerUI spec={spec} tryItOutEnabled={false} defaultModelsExpandDepth={-1} />
          </div>
        )}
      </div>

      <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6">
        <h2 className="text-base font-semibold text-gray-900 dark:text-white mb-5">API 상품 정보</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">API 상품명 *</label>
            <input type="text" value={form.name} onChange={(e) => onFormChange('name', e.target.value)} className={InputClass()} />
            <p className="text-xs text-gray-400 dark:text-gray-600 mt-1">스펙의 info.title에서 자동 입력됐습니다.</p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">설명</label>
            <textarea value={form.description} onChange={(e) => onFormChange('description', e.target.value)} rows={3}
              className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 resize-none" />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">Base URL *</label>
            <input type="url" value={form.baseUrl} onChange={(e) => onFormChange('baseUrl', e.target.value)} className={`${InputClass()} font-mono`} />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">카테고리 *</label>
              <select value={form.category} onChange={(e) => onFormChange('category', e.target.value)}
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400">
                <option value="">선택하세요</option>
                {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">Rate Limit (req/s) *</label>
              <input type="number" min={1} max={1000} value={form.callsPerSec} onChange={(e) => onFormChange('callsPerSec', Number(e.target.value))} className={InputClass()} />
            </div>
          </div>

          <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-200 dark:border-gray-700">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-white">유료 상품 (Pro 플랜 이상)</p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">활성화 시 Free 플랜 사용자 접근 불가</p>
            </div>
            <button onClick={() => onFormChange('isPremium', !form.isPremium)}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${form.isPremium ? 'bg-indigo-600 dark:bg-indigo-500' : 'bg-gray-300 dark:bg-gray-700'}`}>
              <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${form.isPremium ? 'translate-x-6' : 'translate-x-1'}`} />
            </button>
          </div>
        </div>

        <div className="flex justify-end mt-6">
          <button
            onClick={onSubmit}
            disabled={loading || !form.name || !form.baseUrl || !form.category}
            className="px-6 py-2.5 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-40 disabled:cursor-not-allowed shadow-sm"
          >
            {loading ? '등록 중...' : 'API 상품 등록'}
          </button>
        </div>
      </div>
    </div>
  )
}

function SuccessStep({ form, navigate }) {
  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-10 text-center">
      <div className="w-16 h-16 bg-gradient-to-br from-emerald-400 to-teal-500 rounded-2xl flex items-center justify-center mx-auto mb-5 text-3xl shadow-lg shadow-emerald-500/20">
        🎉
      </div>
      <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-2">API 상품이 등록됐습니다!</h2>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-8">
        <span className="font-semibold text-gray-900 dark:text-white">{form.name}</span>이 검토 후 마켓플레이스에 게시됩니다.
      </p>

      <div className="bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 rounded-2xl p-5 text-left mb-8">
        <dl className="space-y-2.5 text-sm">
          {[
            ['상품명', form.name],
            ['Base URL', form.baseUrl],
            ['카테고리', form.category],
            ['Rate Limit', `${form.callsPerSec} req/s`],
            ['플랜', form.isPremium ? 'Pro+ (유료)' : '무료'],
          ].map(([label, value]) => (
            <div key={label} className="flex items-start justify-between gap-4">
              <dt className="text-gray-500 dark:text-gray-400 shrink-0">{label}</dt>
              <dd className="text-gray-900 dark:text-white font-medium text-right truncate">{value}</dd>
            </div>
          ))}
        </dl>
      </div>

      <div className="flex gap-3 justify-center">
        <button onClick={() => navigate('/marketplace')} className="px-5 py-2.5 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm">
          마켓플레이스 보기
        </button>
        <button onClick={() => window.location.reload()} className="px-5 py-2.5 border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 text-sm font-semibold rounded-xl transition-colors">
          또 다른 API 등록
        </button>
      </div>
    </div>
  )
}

export default function RegisterApiPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [spec, setSpec] = useState(null)
  const [form, setForm] = useState({ name: '', description: '', baseUrl: '', category: '', callsPerSec: 5, isPremium: false })
  const [submitting, setSubmitting] = useState(false)

  const handleSpecSuccess = ({ spec, form: autoForm }) => {
    setSpec(spec)
    setForm((prev) => ({ ...prev, ...autoForm }))
    setStep(2)
  }

  const handleSubmit = async () => {
    setSubmitting(true)
    await new Promise((r) => setTimeout(r, 1000))
    setSubmitting(false)
    setStep(3)
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button onClick={() => navigate('/marketplace')}
          className="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6 transition-colors">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          마켓플레이스
        </button>

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">API 상품 등록</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Swagger / OpenAPI 스펙이 적용된 API만 등록 가능합니다.</p>
        </div>

        <StepIndicator current={step} />

        {step === 1 && <SpecUrlStep onSuccess={handleSpecSuccess} />}
        {step === 2 && <ProductInfoStep spec={spec} form={form} onFormChange={(k, v) => setForm((p) => ({ ...p, [k]: v }))} onSubmit={handleSubmit} loading={submitting} />}
        {step === 3 && <SuccessStep form={form} navigate={navigate} />}
      </div>
    </div>
  )
}
