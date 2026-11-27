import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import { ThemeProvider } from './context/ThemeContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import { lazy, Suspense } from 'react'
import LoginPage from './pages/LoginPage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import LandingPage from './pages/LandingPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

// 진입 페이지(랜딩/로그인/회원가입) 외에는 라우트 진입 시점에 로드한다.
// swagger-ui-react(ApiDetail/RegisterApi), recharts(Dashboard)가 초기 번들에 끌려오지 않게 하기 위함.
const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx'))
const MarketplacePage = lazy(() => import('./pages/MarketplacePage.jsx'))
const ApiDetailPage = lazy(() => import('./pages/ApiDetailPage.jsx'))
const RegisterApiPage = lazy(() => import('./pages/RegisterApiPage.jsx'))
const MyProductsPage = lazy(() => import('./pages/MyProductsPage.jsx'))
const ProfilePage = lazy(() => import('./pages/ProfilePage.jsx'))
const LogsPage = lazy(() => import('./pages/LogsPage.jsx'))
const InquiryPage = lazy(() => import('./pages/InquiryPage.jsx'))

function PageFallback() {
  return <div className="min-h-screen" />
}

function RedirectIfAuth({ children }) {
  const { user } = useAuth()
  return user ? <Navigate to="/dashboard" replace /> : children
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <ErrorBoundary>
          <BrowserRouter>
            <Suspense fallback={<PageFallback />}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />
              <Route path="/" element={<RedirectIfAuth><LandingPage /></RedirectIfAuth>} />
              <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
              <Route path="/marketplace" element={<ProtectedRoute><MarketplacePage /></ProtectedRoute>} />
              <Route path="/marketplace/register" element={<ProtectedRoute><RegisterApiPage /></ProtectedRoute>} />
              <Route path="/marketplace/my" element={<ProtectedRoute><MyProductsPage /></ProtectedRoute>} />
              <Route path="/marketplace/:code" element={<ApiDetailPage />} />
              <Route path="/inquiry" element={<ProtectedRoute><InquiryPage /></ProtectedRoute>} />
              <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
              <Route path="/logs" element={<ProtectedRoute><LogsPage /></ProtectedRoute>} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
            </Suspense>
          </BrowserRouter>
        </ErrorBoundary>
      </AuthProvider>
    </ThemeProvider>
  )
}
