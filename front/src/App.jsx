import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import { ThemeProvider } from './context/ThemeContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import LandingPage from './pages/LandingPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import MarketplacePage from './pages/MarketplacePage.jsx'
import ApiDetailPage from './pages/ApiDetailPage.jsx'
import RegisterApiPage from './pages/RegisterApiPage.jsx'
import InquiryPage from './pages/InquiryPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

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
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />
              <Route path="/" element={<RedirectIfAuth><LandingPage /></RedirectIfAuth>} />
              <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
              <Route path="/marketplace" element={<ProtectedRoute><MarketplacePage /></ProtectedRoute>} />
              <Route path="/marketplace/register" element={<ProtectedRoute><RegisterApiPage /></ProtectedRoute>} />
              <Route path="/marketplace/:id" element={<ApiDetailPage />} />
              <Route path="/inquiry" element={<ProtectedRoute><InquiryPage /></ProtectedRoute>} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
        </ErrorBoundary>
      </AuthProvider>
    </ThemeProvider>
  )
}
