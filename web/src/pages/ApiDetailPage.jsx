import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import SwaggerUI from 'swagger-ui-react'
import 'swagger-ui-react/swagger-ui.css'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'
import { gatewayCallUrl } from '../utils/gateway.js'

const LANGS = ['curl', 'java', 'javascript', 'python', 'go']
const LANG_LABELS = { curl: 'Curl', java: 'Java', javascript: 'JavaScript', python: 'Python', go: 'Go' }

function generateSnippet(lang, callBaseUrl, apiKey) {
  const key = apiKey || 'YOUR_SANDBOX_KEY'
  const url = `${callBaseUrl}/{end-point}`
  return {
    curl:
`curl -X GET "${url}" \\
  -H "X-API-KEY: ${key}"`,
    java:
`RestClient client = RestClient.create();
String response = client.get()
    .uri("${url}")
    .header("X-API-KEY", "${key}")
    .retrieve()
    .body(String.class);`,
    javascript:
`const response = await fetch("${url}", {
  headers: { "X-API-KEY": "${key}" },
});
const data = await response.json();`,
    python:
`import requests

response = requests.get(
    "${url}",
    headers={"X-API-KEY": "${key}"}
)
data = response.json()`,
    go:
`req, _ := http.NewRequest("GET", "${url}", nil)
req.Header.Set("X-API-KEY", "${key}")
resp, _ := http.DefaultClient.Do(req)
defer resp.Body.Close()`,
  }[lang]
}

function CopyButton({ text, className = '' }) {
  const [copied, setCopied] = useState(false)
  return (
    <button
      onClick={() => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
      className={`text-xs px-2.5 py-1 rounded-lg font-medium transition-colors ${
        copied
          ? 'bg-green-500/20 text-green-400'
          : 'bg-white/10 hover:bg-white/20 text-gray-300'
      } ${className}`}
    >
      {copied ? '복사됨 ✓' : '복사'}
    </button>
  )
}

export default function ApiDetailPage() {
  const { code } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [activeTab, setActiveTab] = useState('snippets')
  const [activeLang, setActiveLang] = useState('curl')
  const [myKey, setMyKey] = useState(null)
  const [issuing, setIssuing] = useState(false)

  useEffect(() => {
    client.get(`/products/by-code/${code}`)
      .then(({ data }) => setProduct(data))
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false))
  }, [code])

  useEffect(() => {
    if (!product) return
    client.get(`/products/${product.id}/my-key`)
      .then(({ data }) => { if (data.apiKeyValue) setMyKey(data.apiKeyValue) })
      .catch(() => {})
  }, [product])

  const displayKey = myKey

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <Navbar />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="animate-pulse space-y-4">
            <div className="h-6 bg-gray-100 dark:bg-gray-800 rounded w-1/4" />
            <div className="h-32 bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800" />
          </div>
        </div>
      </div>
    )
  }

  if (notFound || !product) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <Navbar />
        <div className="text-center py-32 text-gray-400">API를 찾을 수 없습니다.</div>
      </div>
    )
  }

  const handleIssueKey = async () => {
    setIssuing(true)
    try {
      const { data } = await client.post('/keys', { apiProductId: product.id })
      setMyKey(data.apiKeyValue)
    } catch {
      // CONFLICT — key already exists, fetch it
      const { data } = await client.get(`/products/${product.id}/my-key`).catch(() => ({ data: {} }))
      if (data.apiKeyValue) setMyKey(data.apiKeyValue)
    } finally {
      setIssuing(false)
    }
  }

  const callUrl = gatewayCallUrl(product.code)
  const snippet = generateSnippet(activeLang, callUrl, displayKey)

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        <button
          onClick={() => navigate('/marketplace')}
          className="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          마켓플레이스
        </button>

        {/* API 헤더 카드 */}
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-6 mb-6">
          <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-2">
                <h1 className="text-xl font-bold text-gray-900 dark:text-white">{product.name}</h1>
                {product.isPremium
                  ? <span className="text-xs px-2 py-0.5 bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 border border-amber-200 dark:border-amber-800 rounded-full font-medium">Pro+</span>
                  : <span className="text-xs px-2 py-0.5 bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800 rounded-full font-medium">무료</span>
                }
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-3 leading-relaxed">{product.description}</p>
              <div className="mb-1.5">
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 mr-2">호출 URL</span>
                <code className="inline-block text-xs font-mono bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 px-3 py-1.5 rounded-lg">
                  {callUrl}/…
                </code>
              </div>
              <div className="mb-1.5">
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 mr-2">업스트림</span>
                <code className="inline-block text-xs font-mono bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-500 px-3 py-1.5 rounded-lg">
                  {product.baseUrl}
                </code>
              </div>
              <div>
                <span className="text-xs font-semibold text-gray-500 dark:text-gray-400 mr-2">응답 타입</span>
                <code className="inline-block text-xs font-mono bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-500 px-3 py-1.5 rounded-lg">
                  {product.responseType ?? 'JSON'}
                </code>
              </div>
            </div>

            {/* 키 발급 영역 */}
            <div className="shrink-0 lg:min-w-72">
              {displayKey ? (
                <div className="bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-200 dark:border-indigo-800 rounded-2xl p-4">
                  <p className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 uppercase tracking-wide mb-2">내 Sandbox API Key</p>
                  <div className="flex items-center gap-2 bg-white dark:bg-gray-900 rounded-xl px-3 py-2 border border-indigo-100 dark:border-indigo-900">
                    <code className="text-xs font-mono text-gray-700 dark:text-gray-300 flex-1 truncate">{displayKey.slice(0, 30)}...</code>
                    <button
                      onClick={() => navigator.clipboard.writeText(displayKey)}
                      className="text-xs px-2 py-1 bg-indigo-100 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-400 rounded-lg hover:bg-indigo-200 dark:hover:bg-indigo-800 transition-colors shrink-0"
                    >
                      복사
                    </button>
                  </div>
                </div>
              ) : product.isPremium ? (
                <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-2xl p-4 text-center">
                  <p className="text-xs text-amber-700 dark:text-amber-400 font-medium mb-3">Pro 플랜 이상 필요</p>
                  <button className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white text-sm font-semibold rounded-xl transition-colors">
                    플랜 업그레이드
                  </button>
                </div>
              ) : (
                <button
                  onClick={handleIssueKey}
                  disabled={issuing}
                  className="w-full px-5 py-3 bg-indigo-600 dark:bg-indigo-500 hover:bg-indigo-700 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-2xl transition-colors disabled:opacity-50 shadow-sm"
                >
                  {issuing ? 'Sandbox 키 발급 중...' : '⚡ Sandbox 키 즉시 발급'}
                </button>
              )}
            </div>
          </div>
        </div>

        {/* 탭 */}
        <div className="flex gap-1 bg-gray-100 dark:bg-gray-800/60 p-1 rounded-xl w-fit mb-6 border border-gray-200 dark:border-gray-700">
          {[['snippets', '코드 스니펫'], ...(product.specJson ? [['swagger', 'Swagger UI']] : [])].map(([key, label]) => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={`px-5 py-2 text-sm font-medium rounded-lg transition-colors ${
                activeTab === key
                  ? 'bg-white dark:bg-gray-900 text-gray-900 dark:text-white shadow-sm border border-gray-200 dark:border-gray-700'
                  : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Swagger UI */}
        {activeTab === 'swagger' && product.specJson && (
          <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <SwaggerUI spec={product.specJson} tryItOutEnabled={false} defaultModelsExpandDepth={-1} />
          </div>
        )}

        {/* 코드 스니펫 */}
        {activeTab === 'snippets' && (
          <div className="bg-gray-900 dark:bg-gray-950 rounded-2xl border border-gray-800 overflow-hidden">
            <div className="flex items-center gap-1 px-4 py-3 border-b border-gray-800">
              {LANGS.map((lang) => (
                <button
                  key={lang}
                  onClick={() => setActiveLang(lang)}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                    activeLang === lang
                      ? 'bg-indigo-600 text-white'
                      : 'text-gray-500 hover:text-gray-300 hover:bg-gray-800'
                  }`}
                >
                  {LANG_LABELS[lang]}
                </button>
              ))}
              <div className="ml-auto">
                <CopyButton text={snippet} />
              </div>
            </div>
            <div className="p-6 overflow-x-auto">
              <pre className="text-sm text-emerald-300 font-mono leading-relaxed whitespace-pre">{snippet}</pre>
            </div>
            {!displayKey && (
              <div className="px-6 py-3 bg-amber-900/20 border-t border-amber-900/40 text-xs text-amber-400">
                💡 Sandbox 키를 발급받으면 코드에 실제 키가 자동으로 삽입됩니다.
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
