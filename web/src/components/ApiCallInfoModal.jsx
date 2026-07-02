import { useState } from 'react'
import { gatewayCallUrl } from '../utils/gateway.js'

function CopyButton({ text }) {
  const [copied, setCopied] = useState(false)
  return (
    <button
      onClick={() => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
      className={`text-xs px-2.5 py-1 rounded-lg font-medium transition-colors shrink-0 ${
        copied
          ? 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-400'
          : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
      }`}
    >
      {copied ? '복사됨 ✓' : '복사'}
    </button>
  )
}

export default function ApiCallInfoModal({ product, apiKeyValue, onClose, onViewFull }) {
  const callUrl = gatewayCallUrl(product.code)
  const key = apiKeyValue ?? 'YOUR_SANDBOX_KEY'
  const curlExample = `curl "${callUrl}/{엔드포인트}" \\\n  -H "X-API-KEY: ${key}"`

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-black/50 dark:bg-black/70" onClick={onClose} />

      <div className="relative w-full max-w-lg bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-xl p-6">
        <div className="flex items-start justify-between mb-5">
          <div>
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">{product.name}</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">API 호출 정보</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 dark:text-gray-500 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
            aria-label="닫기"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <p className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">호출 URL</p>
            <div className="flex items-center gap-2 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl px-3 py-2.5">
              <code className="flex-1 text-xs font-mono text-gray-700 dark:text-gray-300 truncate">{callUrl}/…</code>
              <CopyButton text={callUrl} />
            </div>
            <p className="text-xs text-gray-400 dark:text-gray-500 mt-1.5">
              위 URL 뒤에 실제 엔드포인트 경로(예: <code className="font-mono">/current?region=서울</code>)를 붙여서 호출하세요.
              업스트림 주소(<code className="font-mono">{product.baseUrl}</code>)에 직접 요청하면 안 됩니다.
            </p>
          </div>

          <div>
            <p className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">API Key (X-API-KEY 헤더)</p>
            {apiKeyValue ? (
              <div className="flex items-center gap-2 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl px-3 py-2.5">
                <code className="flex-1 text-xs font-mono text-gray-700 dark:text-gray-300 truncate">{apiKeyValue}</code>
                <CopyButton text={apiKeyValue} />
              </div>
            ) : (
              <p className="text-xs text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 px-3 py-2 rounded-lg">
                아직 Sandbox 키가 발급되지 않았습니다. 마켓플레이스에서 먼저 발급받아 주세요.
              </p>
            )}
          </div>

          <div>
            <p className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">curl 예시</p>
            <div className="bg-gray-900 dark:bg-gray-950 rounded-xl border border-gray-800 p-3">
              <pre className="text-xs text-emerald-300 font-mono leading-relaxed whitespace-pre-wrap break-all">{curlExample}</pre>
            </div>
          </div>
        </div>

        <button
          onClick={onViewFull}
          className="w-full mt-5 py-2.5 bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-200 text-sm font-semibold rounded-xl transition-colors"
        >
          전체 보기 (Swagger UI / 코드 스니펫) →
        </button>
      </div>
    </div>
  )
}
