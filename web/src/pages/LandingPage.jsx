import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useTheme } from '../context/ThemeContext.jsx'
import client from '../api/client.js'

const CATEGORY_STYLES = {
  Weather:  'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-900/20 dark:text-sky-400 dark:border-sky-800',
  Location: 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/20 dark:text-emerald-400 dark:border-emerald-800',
  Tourism:  'bg-violet-50 text-violet-700 border-violet-200 dark:bg-violet-900/20 dark:text-violet-400 dark:border-violet-800',
  Finance:  'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/20 dark:text-amber-400 dark:border-amber-800',
}

const CATEGORY_KO = {
  Weather: '날씨', Location: '위치', Tourism: '관광', Finance: '금융',
}

function SunIcon() {
  return (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364-6.364l-.707.707M6.343 17.657l-.707.707M17.657 17.657l-.707-.707M6.343 6.343l-.707-.707M12 7a5 5 0 100 10A5 5 0 0012 7z" />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
    </svg>
  )
}

function LandingNavbar() {
  const { user } = useAuth()
  const { theme, toggleTheme } = useTheme()

  return (
    <nav className="bg-slate-50/90 dark:bg-gray-900/80 backdrop-blur-md border-b border-slate-200 dark:border-gray-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-14">
          <div className="flex items-center gap-6">
            <Link to="/" className="text-lg font-bold bg-gradient-to-r from-indigo-500 to-violet-500 bg-clip-text text-transparent">
              APIverse
            </Link>
            <a href="#apis" className="hidden md:block text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 transition-colors">
              API 목록
            </a>
            <a href="#how" className="hidden md:block text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 transition-colors">
              사용 방법
            </a>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            >
              {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
            </button>
            {user ? (
              <Link to="/dashboard" className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium transition-colors">
                대시보드
              </Link>
            ) : (
              <>
                <Link to="/login" className="hidden sm:block px-3 py-1.5 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                  로그인
                </Link>
                <Link to="/signup" className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium transition-colors">
                  회원가입
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}

function Hero() {
  return (
    <section className="relative overflow-hidden bg-slate-50 dark:bg-gray-900 pt-20 pb-24">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-indigo-100 dark:bg-indigo-900/20 rounded-full blur-3xl opacity-70" />
        <div className="absolute -bottom-20 -left-20 w-72 h-72 bg-violet-100 dark:bg-violet-900/20 rounded-full blur-3xl opacity-60" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-to-r from-indigo-50 to-violet-50 dark:from-indigo-900/10 dark:to-violet-900/10 rounded-full blur-3xl opacity-40" />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 dark:bg-indigo-900/30 border border-indigo-100 dark:border-indigo-800 mb-8">
          <span className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-pulse" />
          <span className="text-xs font-medium text-indigo-600 dark:text-indigo-400">API 게이트웨이 플랫폼</span>
        </div>

        <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-slate-800 dark:text-white mb-6 leading-tight">
          API 통합의{' '}
          <span className="bg-gradient-to-r from-indigo-500 to-violet-500 bg-clip-text text-transparent">
            새로운 기준
          </span>
        </h1>

        <p className="text-lg sm:text-xl text-gray-500 dark:text-gray-400 max-w-2xl mx-auto mb-10 leading-relaxed">
          외부 API를 안전하게 관리하고 실시간으로 모니터링하는<br className="hidden sm:block" />
          개발자를 위한 API 게이트웨이 플랫폼입니다.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link
            to="/signup"
            className="w-full sm:w-auto px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition-colors shadow-lg shadow-indigo-500/25"
          >
            무료로 시작하기
          </Link>
        </div>
      </div>
    </section>
  )
}

function ProductPreview() {
  return (
    <section className="bg-slate-50 dark:bg-gray-900 py-20">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold text-slate-800 dark:text-white mb-3">
            모든 현황을 한눈에
          </h2>
          <p className="text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
            API 호출량, 오류율, 발급 키 목록까지 하나의 대시보드에서 실시간으로 확인하세요.
          </p>
        </div>
        {/* 브라우저 프레임 */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 shadow-2xl shadow-gray-200/60 dark:shadow-black/40 overflow-hidden">
          {/* 브라우저 크롬 */}
          <div className="bg-gray-100 dark:bg-gray-800 px-4 py-3 flex items-center gap-3 border-b border-gray-200 dark:border-gray-700">
            <div className="flex gap-1.5">
              <span className="w-3 h-3 rounded-full bg-red-400/80" />
              <span className="w-3 h-3 rounded-full bg-yellow-400/80" />
              <span className="w-3 h-3 rounded-full bg-green-400/80" />
            </div>
            <div className="flex-1 bg-white dark:bg-gray-700 rounded-md px-3 py-1 text-xs text-gray-400 dark:text-gray-500 font-mono">
              localhost:5173/dashboard
            </div>
          </div>
          {/* 스크린샷 */}
          <img
            src="/imgs/dash_board.png"
            alt="APIverse 대시보드 미리보기"
            className="w-full block"
          />
        </div>
      </div>
    </section>
  )
}

function Stats() {
  const items = [
    { value: '4개', label: '연동 API' },
    { value: '실시간', label: '프록시 라우팅' },
    { value: 'Token Bucket', label: 'Rate Limiting' },
    { value: '무제한', label: 'Sandbox 체험' },
  ]
  return (
    <div className="border-y border-slate-200 dark:border-gray-800 bg-white dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
          {items.map((item) => (
            <div key={item.label}>
              <p className="text-xl font-bold text-slate-800 dark:text-white">{item.value}</p>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{item.label}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function ApiCard({ product }) {
  const catStyle = CATEGORY_STYLES[product.category] || 'bg-gray-50 text-gray-600 border-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-700'
  const catKo = CATEGORY_KO[product.category] || product.category

  return (
    <div className="bg-white dark:bg-gray-900 rounded-2xl border border-slate-200 dark:border-gray-800 p-5 flex flex-col hover:border-indigo-200 dark:hover:border-indigo-800 hover:shadow-lg hover:shadow-indigo-500/5 transition-all duration-200 group">
      <div className="flex items-start justify-between mb-3">
        <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${catStyle}`}>
          {catKo}
        </span>
        {product.isPremium && (
          <span className="text-xs px-2 py-0.5 rounded-full bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 border border-amber-200 dark:border-amber-800 font-medium">
            PRO
          </span>
        )}
      </div>

      <h3 className="text-sm font-semibold text-slate-700 dark:text-white mb-2 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
        {product.name}
      </h3>
      <p className="text-xs text-gray-500 dark:text-gray-400 leading-relaxed flex-1 line-clamp-2">
        {product.description}
      </p>

      <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between">
        <span className="text-xs text-gray-400 dark:text-gray-500 font-mono">
          {product.callsPerSec}req/s
        </span>
        <Link
          to={`/marketplace/${product.code}`}
          className="text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors"
        >
          자세히 보기 →
        </Link>
      </div>
    </div>
  )
}

function ApiSection({ products }) {
  return (
    <section id="apis" className="py-20 bg-slate-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold text-slate-800 dark:text-white mb-4">연동 가능한 API</h2>
          <p className="text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
            Sandbox 키를 즉시 발급받아 테스트해보세요. 모든 API는 무료로 체험 가능합니다.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {products.length === 0
            ? [...Array(4)].map((_, i) => (
                <div key={i} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 h-44 animate-pulse" />
              ))
            : products.map((p) => <ApiCard key={p.id} product={p} />)
          }
        </div>

        <div className="text-center mt-8">
          <Link
            to="/marketplace"
            className="inline-flex items-center gap-2 text-sm font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 transition-colors"
          >
            전체 마켓플레이스 보기
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 8l4 4m0 0l-4 4m4-4H3" />
            </svg>
          </Link>
        </div>
      </div>
    </section>
  )
}

function HowItWorks() {
  const steps = [
    {
      number: '01',
      title: 'API 키 발급',
      description: '원하는 API를 마켓플레이스에서 선택하고 Sandbox 키를 즉시 발급받으세요. 별도의 심사 없이 바로 시작할 수 있습니다.',
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
        </svg>
      ),
    },
    {
      number: '02',
      title: '게이트웨이 호출',
      description: 'X-API-KEY 헤더에 발급받은 키를 담아 APIverse 게이트웨이를 통해 API를 호출합니다. Rate Limiting이 자동으로 적용됩니다.',
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      ),
    },
    {
      number: '03',
      title: '실시간 모니터링',
      description: '대시보드에서 API 호출 현황, 오류율, 일별 통계를 실시간으로 확인하고 관리하세요.',
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      ),
    },
  ]

  return (
    <section id="how" className="py-20 bg-white/70 dark:bg-gray-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-14">
          <h2 className="text-3xl font-bold text-slate-800 dark:text-white mb-4">어떻게 작동하나요?</h2>
          <p className="text-gray-500 dark:text-gray-400">3단계로 API 통합을 시작하세요</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
          {/* 연결선 */}
          <div className="hidden md:block absolute top-9 left-1/3 right-1/3 h-px bg-gradient-to-r from-indigo-200 via-violet-200 to-indigo-200 dark:from-indigo-800 dark:via-violet-800 dark:to-indigo-800" />

          {steps.map((step) => (
            <div key={step.number} className="relative bg-gray-50 dark:bg-gray-800/50 rounded-2xl p-6 border border-gray-100 dark:border-gray-800">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-indigo-50 dark:bg-indigo-900/40 border border-indigo-100 dark:border-indigo-800 flex items-center justify-center text-indigo-600 dark:text-indigo-400 flex-shrink-0">
                  {step.icon}
                </div>
                <span className="text-2xl font-bold text-gray-200 dark:text-gray-700 leading-none">{step.number}</span>
              </div>
              <h3 className="text-base font-semibold text-slate-800 dark:text-white mb-2">{step.title}</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{step.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function CodeExample() {
  return (
    <section className="py-20 bg-slate-100/60 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold text-slate-800 dark:text-white mb-4">간단한 호출, 강력한 기능</h2>
          <p className="text-gray-500 dark:text-gray-400">
            단 한 줄의 헤더 추가로 게이트웨이의 모든 기능을 사용할 수 있습니다
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 max-w-5xl mx-auto">
          {/* Request */}
          <div className="bg-gray-900 dark:bg-black rounded-2xl border border-gray-700 dark:border-gray-800 overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-700 dark:border-gray-800 flex items-center gap-2">
              <div className="flex gap-1.5">
                <span className="w-3 h-3 rounded-full bg-red-500/60" />
                <span className="w-3 h-3 rounded-full bg-yellow-500/60" />
                <span className="w-3 h-3 rounded-full bg-green-500/60" />
              </div>
              <span className="text-xs text-gray-500 ml-2 font-mono">요청</span>
            </div>
            <pre className="p-5 text-sm leading-relaxed overflow-x-auto">
              <code>
                <span className="text-gray-500"># 기상청 날씨 API 호출{'\n'}</span>
                <span className="text-green-400">curl</span>
                <span className="text-gray-300"> "http://localhost:8080</span>
                <span className="text-indigo-400">/gateway/1/current</span>
                <span className="text-amber-400">?region=서울</span>
                <span className="text-gray-300">" \{'\n'}</span>
                <span className="text-gray-300">  </span>
                <span className="text-blue-400">-H</span>
                <span className="text-gray-300"> </span>
                <span className="text-orange-300">"X-API-KEY: apiverse_sandbox_a1b2..."</span>
              </code>
            </pre>
          </div>

          {/* Response */}
          <div className="bg-gray-900 dark:bg-black rounded-2xl border border-gray-700 dark:border-gray-800 overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-700 dark:border-gray-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="flex gap-1.5">
                  <span className="w-3 h-3 rounded-full bg-red-500/60" />
                  <span className="w-3 h-3 rounded-full bg-yellow-500/60" />
                  <span className="w-3 h-3 rounded-full bg-green-500/60" />
                </div>
                <span className="text-xs text-gray-500 ml-2 font-mono">응답</span>
              </div>
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-900/40 text-emerald-400 border border-emerald-800 font-mono">
                200 OK
              </span>
            </div>
            <pre className="p-5 text-sm leading-relaxed overflow-x-auto">
              <code>
                <span className="text-gray-400">{'{'}{'\n'}</span>
                <span className="text-gray-400">  </span><span className="text-blue-400">"region"</span><span className="text-gray-400">:    </span><span className="text-orange-300">"서울"</span><span className="text-gray-400">,{'\n'}</span>
                <span className="text-gray-400">  </span><span className="text-blue-400">"temp"</span><span className="text-gray-400">:      </span><span className="text-purple-400">22.4</span><span className="text-gray-400">,{'\n'}</span>
                <span className="text-gray-400">  </span><span className="text-blue-400">"humidity"</span><span className="text-gray-400">:  </span><span className="text-purple-400">65</span><span className="text-gray-400">,{'\n'}</span>
                <span className="text-gray-400">  </span><span className="text-blue-400">"wind"</span><span className="text-gray-400">:      </span><span className="text-purple-400">3.2</span><span className="text-gray-400">,{'\n'}</span>
                <span className="text-gray-400">  </span><span className="text-blue-400">"condition"</span><span className="text-gray-400">: </span><span className="text-orange-300">"맑음"</span><span className="text-gray-400">{'\n'}</span>
                <span className="text-gray-400">{'}'}</span>
              </code>
            </pre>
          </div>
        </div>

        {/* Features list */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-8 max-w-5xl mx-auto">
          {[
            { icon: '🔑', title: 'API 키 인증', desc: 'X-API-KEY 헤더로 간편하게 인증' },
            { icon: '⚡', title: 'Rate Limiting', desc: 'Token Bucket 알고리즘으로 초당 요청 제한' },
            { icon: '📊', title: '자동 로그', desc: '모든 호출이 자동으로 billing_logs에 기록' },
          ].map((f) => (
            <div key={f.title} className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-4 flex items-start gap-3">
              <span className="text-xl">{f.icon}</span>
              <div>
                <p className="text-sm font-semibold text-slate-700 dark:text-white">{f.title}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{f.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function Footer() {
  return (
    <footer className="border-t border-slate-200 dark:border-gray-800 py-8 bg-slate-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
        <span className="text-base font-bold bg-gradient-to-r from-indigo-500 to-violet-500 bg-clip-text text-transparent">
          APIverse
        </span>
        <div className="flex items-center gap-6 text-xs text-gray-400 dark:text-gray-500">
          <Link to="/marketplace" className="hover:text-gray-600 dark:hover:text-gray-300 transition-colors">API 마켓플레이스</Link>
          <Link to="/inquiry" className="hover:text-gray-600 dark:hover:text-gray-300 transition-colors">1:1 문의</Link>
          <span>© 2026 Hypepia Inc.</span>
        </div>
      </div>
    </footer>
  )
}

export default function LandingPage() {
  const [products, setProducts] = useState([])

  useEffect(() => {
    client.get('/products').then(({ data }) => setProducts(data)).catch(() => {})
  }, [])

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-gray-900">
      <LandingNavbar />
      <Hero />
      <Stats />
      <ApiSection products={products} />
      <CodeExample />
      {/*<CtaBanner />*/}
      <ProductPreview />
      <HowItWorks />
      <Footer />
    </div>
  )
}
