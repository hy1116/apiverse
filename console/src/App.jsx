import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import { ThemeProvider } from './context/ThemeContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import ProductsPage from './pages/ProductsPage.jsx'
import ProductDetailPage from './pages/ProductDetailPage.jsx'
import InquiriesPage from './pages/InquiriesPage.jsx'
import UsersPage from './pages/UsersPage.jsx'
import UserDetailPage from './pages/UserDetailPage.jsx'
import ApiKeysPage from './pages/ApiKeysPage.jsx'
import ApiKeyDetailPage from './pages/ApiKeyDetailPage.jsx'
import BillingLogsPage from './pages/BillingLogsPage.jsx'
import BlockedIpsPage from './pages/BlockedIpsPage.jsx'
import StatsPage from './pages/StatsPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

function RedirectIfAuth({ children }) {
  const { admin } = useAuth()
  return admin ? <Navigate to="/dashboard" replace /> : children
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <ErrorBoundary>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<RedirectIfAuth><LoginPage /></RedirectIfAuth>} />
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
              <Route path="/products" element={<ProtectedRoute><ProductsPage /></ProtectedRoute>} />
              <Route path="/products/:id" element={<ProtectedRoute><ProductDetailPage /></ProtectedRoute>} />
              <Route path="/inquiries" element={<ProtectedRoute><InquiriesPage /></ProtectedRoute>} />
              <Route path="/users" element={<ProtectedRoute><UsersPage /></ProtectedRoute>} />
              <Route path="/users/:id" element={<ProtectedRoute><UserDetailPage /></ProtectedRoute>} />
              <Route path="/keys" element={<ProtectedRoute><ApiKeysPage /></ProtectedRoute>} />
              <Route path="/keys/:id" element={<ProtectedRoute><ApiKeyDetailPage /></ProtectedRoute>} />
              <Route path="/logs" element={<ProtectedRoute><BillingLogsPage /></ProtectedRoute>} />
              <Route path="/blocked-ips" element={<ProtectedRoute><BlockedIpsPage /></ProtectedRoute>} />
              <Route path="/stats" element={<ProtectedRoute><StatsPage /></ProtectedRoute>} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
        </ErrorBoundary>
      </AuthProvider>
    </ThemeProvider>
  )
}
