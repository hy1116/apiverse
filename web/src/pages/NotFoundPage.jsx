import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function NotFoundPage() {
  const { user } = useAuth()

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 dark:bg-gray-900 px-4">
      <p className="text-8xl font-bold text-indigo-500 mb-4">404</p>
      <h1 className="text-2xl font-semibold text-slate-800 dark:text-white mb-2">
        페이지를 찾을 수 없어요
      </h1>
      <p className="text-slate-500 dark:text-slate-400 mb-8 text-center">
        요청하신 주소가 존재하지 않거나 이동되었습니다.
      </p>
      <Link
        to={user ? '/dashboard' : '/'}
        className="px-6 py-3 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition-colors"
      >
        {user ? '대시보드로 이동' : '홈으로 이동'}
      </Link>
    </div>
  )
}
