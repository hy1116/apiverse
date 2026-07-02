import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { useTheme } from '../context/ThemeContext.jsx'
import client from '../api/client.js'

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
  { icon: '📋', text: '브라우저에서 바로 API 테스트' },
  { icon: '💻', text: 'Curl · Java · Python 코드 자동 생성' },
]

const INPUT_BASE = 'w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:border-transparent'

function formatPhone(raw) {
  const digits = raw.replace(/\D/g, '').slice(0, 11)
  if (digits.startsWith('02')) {
    const d = digits.slice(0, 10)
    if (d.length <= 2) return d
    if (d.length <= 6) return `${d.slice(0, 2)}-${d.slice(2)}`
    return `${d.slice(0, 2)}-${d.slice(2, 6)}-${d.slice(6)}`
  }
  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

function validatePhone(phone) {
  const d = phone.replace(/\D/g, '')
  return /^(010|011|016|017|018|019)\d{7,8}$/.test(d) ||
         /^02\d{7,8}$/.test(d) ||
         /^0[3-9]\d{8,9}$/.test(d)
}

export default function SignupPage() {
  const [form, setForm] = useState({ email: '', password: '', confirmPassword: '', companyName: '', phone: '' })
  const [emailCheck, setEmailCheck] = useState({ status: 'idle', message: '' })
  // status: 'idle' | 'checking' | 'available' | 'taken' | 'error'
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const passwordMatch = form.confirmPassword.length > 0 && form.password === form.confirmPassword
  const passwordMismatch = form.confirmPassword.length > 0 && form.password !== form.confirmPassword
  const phoneInvalid = form.phone.length > 0 && !validatePhone(form.phone)

  const handleEmailChange = (e) => {
    setForm({ ...form, email: e.target.value })
    setEmailCheck({ status: 'idle', message: '' })
  }

  const handlePhoneChange = (e) => {
    setForm({ ...form, phone: formatPhone(e.target.value) })
  }

  const handleEmailCheck = async () => {
    if (!form.email) { setEmailCheck({ status: 'idle', message: '이메일을 입력해주세요.' }); return }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(form.email)) { setEmailCheck({ status: 'idle', message: '올바른 이메일 형식이 아닙니다.' }); return }

    setEmailCheck({ status: 'checking', message: '' })
    try {
      const { data } = await client.get(`/auth/check-email?email=${encodeURIComponent(form.email)}`)
      if (data.available) {
        setEmailCheck({ status: 'available', message: '사용 가능한 이메일입니다.' })
      } else {
        setEmailCheck({ status: 'taken', message: '이미 사용 중인 이메일입니다.' })
      }
    } catch {
      setEmailCheck({ status: 'error', message: '확인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' })
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!form.email || !form.password) { setError('이메일과 비밀번호는 필수입니다.'); return }
    if (emailCheck.status !== 'available') { setError('이메일 중복 확인을 완료해주세요.'); return }
    if (form.password.length < 8) { setError('비밀번호는 8자 이상이어야 합니다.'); return }
    if (form.password !== form.confirmPassword) { setError('비밀번호가 일치하지 않습니다.'); return }
    if (phoneInvalid) { setError('올바른 전화번호 형식이 아닙니다.'); return }

    setLoading(true)
    try {
      const { data } = await client.post('/auth/signup', {
        email: form.email,
        password: form.password,
        companyName: form.companyName,
        phone: form.phone,
      })
      login(data)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.detail || err.response?.data?.message || '회원가입 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const emailBorderColor = {
    idle:      'border-gray-200 dark:border-gray-700 focus:ring-indigo-500 dark:focus:ring-indigo-400',
    checking:  'border-gray-200 dark:border-gray-700 focus:ring-indigo-500 dark:focus:ring-indigo-400',
    available: 'border-emerald-400 dark:border-emerald-500 focus:ring-emerald-400',
    taken:     'border-red-400 dark:border-red-500 focus:ring-red-400',
    error:     'border-amber-400 dark:border-amber-500 focus:ring-amber-400',
  }[emailCheck.status]

  const phoneBorder = phoneInvalid
    ? 'border-red-400 dark:border-red-500 focus:ring-red-400'
    : form.phone && !phoneInvalid
      ? 'border-emerald-400 dark:border-emerald-500 focus:ring-emerald-400'
      : 'border-gray-200 dark:border-gray-700 focus:ring-indigo-500 dark:focus:ring-indigo-400'

  const pwConfirmBorder = passwordMismatch
    ? 'border-red-400 dark:border-red-600 focus:ring-red-400'
    : passwordMatch
      ? 'border-emerald-400 dark:border-emerald-600 focus:ring-emerald-400'
      : 'border-gray-200 dark:border-gray-700 focus:ring-indigo-500 dark:focus:ring-indigo-400'

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex flex-col lg:flex-row">
      {/* 왼쪽 브랜딩 패널 */}
      <div className="hidden lg:flex flex-col justify-between w-[420px] shrink-0 bg-gradient-to-b from-indigo-600 to-violet-700 dark:from-indigo-900 dark:to-violet-950 p-10">
        <span className="text-xl font-bold text-white">APIverse</span>
        <div>
          <h2 className="text-3xl font-bold text-white mb-3 leading-snug">
            지루한 B2B 미팅 없이<br />가입 즉시 시작하세요.
          </h2>
          <p className="text-indigo-200 dark:text-indigo-300 text-sm mb-10">Developer-First API 중개 플랫폼</p>
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
          <span className="text-lg font-bold bg-gradient-to-r from-indigo-500 to-violet-500 bg-clip-text text-transparent">APIverse</span>
          <button onClick={toggleTheme} className="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
            {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
          </button>
        </div>

        <div className="flex-1 flex items-center justify-center px-6 py-10">
          <div className="w-full max-w-sm">
            <div className="flex items-center justify-between mb-6">
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

                {/* 이메일 + 중복 확인 */}
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                    이메일 *
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="email"
                      value={form.email}
                      onChange={handleEmailChange}
                      placeholder="dev@example.com"
                      className={`flex-1 min-w-0 ${INPUT_BASE} ${emailBorderColor}`}
                    />
                    <button
                      type="button"
                      onClick={handleEmailCheck}
                      disabled={emailCheck.status === 'checking' || !form.email}
                      className="shrink-0 px-3 py-2 text-xs font-semibold border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 rounded-xl transition-colors disabled:opacity-40 whitespace-nowrap"
                    >
                      {emailCheck.status === 'checking' ? '확인 중...' : '중복 확인'}
                    </button>
                  </div>
                  {emailCheck.message && (
                    <p className={`text-xs mt-1.5 flex items-center gap-1 ${
                      emailCheck.status === 'available' ? 'text-emerald-600 dark:text-emerald-400' :
                      emailCheck.status === 'error'     ? 'text-amber-600 dark:text-amber-400' :
                                                         'text-red-500 dark:text-red-400'
                    }`}>
                      {emailCheck.status === 'available' ? '✓' : emailCheck.status === 'error' ? '⚠' : '✕'} {emailCheck.message}
                    </p>
                  )}
                </div>

                {/* 비밀번호 */}
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                    비밀번호 *
                  </label>
                  <input
                    type="password"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    placeholder="8자 이상"
                    className={`${INPUT_BASE} border-gray-200 dark:border-gray-700 focus:ring-indigo-500 dark:focus:ring-indigo-400`}
                  />
                  {form.password.length > 0 && form.password.length < 8 && (
                    <p className="text-xs text-amber-500 dark:text-amber-400 mt-1.5">8자 이상 입력해주세요. ({form.password.length}/8)</p>
                  )}
                </div>

                {/* 비밀번호 확인 */}
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                    비밀번호 확인 *
                  </label>
                  <input
                    type="password"
                    value={form.confirmPassword}
                    onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                    placeholder="비밀번호를 다시 입력하세요"
                    className={`${INPUT_BASE} ${pwConfirmBorder}`}
                  />
                  {passwordMatch && (
                    <p className="text-xs text-emerald-600 dark:text-emerald-400 mt-1.5 flex items-center gap-1">✓ 비밀번호가 일치합니다.</p>
                  )}
                  {passwordMismatch && (
                    <p className="text-xs text-red-500 dark:text-red-400 mt-1.5 flex items-center gap-1">✕ 비밀번호가 일치하지 않습니다.</p>
                  )}
                </div>

                {/* 회사명 */}
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                    회사명 <span className="text-gray-400 dark:text-gray-600 normal-case font-normal">(선택)</span>
                  </label>
                  <input
                    type="text"
                    value={form.companyName}
                    onChange={(e) => setForm({ ...form, companyName: e.target.value })}
                    placeholder="개인 / 스타트업 / 기업명"
                    className={`${INPUT_BASE} border-gray-200 dark:border-gray-700 focus:ring-indigo-500 dark:focus:ring-indigo-400`}
                  />
                </div>

                {/* 연락처 */}
                <div>
                  <label className="block text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide mb-1.5">
                    연락처 <span className="text-gray-400 dark:text-gray-600 normal-case font-normal">(선택 · 유선/휴대폰)</span>
                  </label>
                  <input
                    type="tel"
                    value={form.phone}
                    onChange={handlePhoneChange}
                    placeholder="010-0000-0000 또는 02-0000-0000"
                    className={`${INPUT_BASE} ${phoneBorder}`}
                  />
                  {phoneInvalid && (
                    <p className="text-xs text-red-500 dark:text-red-400 mt-1.5">✕ 올바른 전화번호 형식이 아닙니다.</p>
                  )}
                  {form.phone && !phoneInvalid && (
                    <p className="text-xs text-emerald-600 dark:text-emerald-400 mt-1.5">✓ 올바른 형식입니다.</p>
                  )}
                </div>

                {error && (
                  <p className="text-xs text-red-500 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 px-3 py-2 rounded-lg">
                    ✕ {error}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={loading || emailCheck.status !== 'available' || passwordMismatch || !form.confirmPassword || phoneInvalid}
                  className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-40 disabled:cursor-not-allowed shadow-sm"
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
