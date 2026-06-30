import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useTheme } from '../context/ThemeContext.jsx'
import { generateSandboxKey } from '../data/mockData.js'

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

const PERKS = [
  { icon: '⚡', text: '가입 즉시 Sandbox 키 자동 발급' },
  { icon: '🔑', text: '신용카드 · 승인 대기 없음' },
  { icon: '📋', text: '브라우저에서 바로 API 테스트' },
  { icon: '💻', text: 'Curl · Java · Python 코드 자동 생성' },
]

export default function SignupPage() {
  const [form, setForm] = useState({ email: '', password: '', companyName: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.email || !form.password) { setError('이메일과 비밀번호는 필수입니다.'); return }
    if (form.password.length < 8) { setError('비밀번호는 8자 이상이어야 합니다.'); return }
    setLoading(true)
    await new Promise((r) => setTimeout(r, 800))
    login({ id: Date.now(), email: form.email, companyName: form.companyName || '개인', tier: 'FREE', token: generateSandboxKey() })
    navigate('/dashboard')
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex flex-col lg:flex-row">
      {/* 왼쪽 브랜딩 패널 */}
      <div className="hidden lg:flex flex-col justify-between w-[420px] shrink-0 bg-gradient-to-b from-indigo-600 to-violet-700 dark:from-indigo-900 dark:to-violet-950 p-10">
        <span className="text-xl font-bold text-white">apiverse</span>
        <div>
          <h2 className="text-3xl font-bold text-white mb-3 leading-snug">
            지루한 B2B 미팅 없이<br />가입 즉시 시작하세요.
          </h2>
          <p className="text-indigo-200 dark:text-indigo-300 text-sm mb-10">
            Developer-First API 중개 플랫폼
          </p>
          <div className="space-y-4">
            {PERKS.map((p) => (
              <div key={p.text} className="flex items-center gap-3">
                <span className="text-xl">{p.icon}</span>
                <span className="text-sm text-indigo-100">{p.text}</span>
              </div>
            ))}
          </div>
        </div>
        <p className="text-indigo-300 text-xs">© 2026 Hypepia Inc.</p>
      </div>

      {/* 오른쪽 폼 */}
      <div className="flex-1 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 lg:hidden">
          <span className="text-lg font-bold bg-gradient-to-r from-indigo-500 to-violet-500 bg-clip-text text-transparent">apiverse</span>
          <button onClick={toggleTheme} className="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
            {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
          </button>
        </div>

        <div className="flex-1 flex items-center justify-center px-6 py-12">
          <div className="w-full max-w-sm">
            <div className="flex items-center justify-between mb-8">
              <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">계정 만들기</h1>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">무료로 시작하세요.</p>
              </div>
              <button onClick={toggleTheme} className="hidden lg:flex p-2 rounded-lg text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
              </button>
            </div>

            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm p-6">
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">이메일 *</label>
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    placeholder="dev@example.com"
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">비밀번호 *</label>
                  <input
                    type="password"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    placeholder="8자 이상"
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">회사명 (선택)</label>
                  <input
                    type="text"
                    value={form.companyName}
                    onChange={(e) => setForm({ ...form, companyName: e.target.value })}
                    placeholder="개인 / 스타트업 / 기업명"
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent"
                  />
                </div>

                {error && (
                  <p className="text-xs text-red-500 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 px-3 py-2 rounded-lg">
                    {error}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 shadow-sm"
                >
                  {loading ? '계정 생성 중...' : '가입 후 Sandbox 키 발급받기 →'}
                </button>
              </form>
            </div>

            <p className="text-center text-sm text-gray-500 dark:text-gray-400 mt-5">
              이미 계정이 있으신가요?{' '}
              <Link to="/login" className="text-indigo-600 dark:text-indigo-400 font-semibold hover:underline">로그인</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
