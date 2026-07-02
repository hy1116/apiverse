import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import { ThemeProvider } from './context/ThemeContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import ProductsPage from './pages/ProductsPage.jsx'
import InquiriesPage from './pages/InquiriesPage.jsx'
import UsersPage from './pages/UsersPage.jsx'
import ApiKeysPage from './pages/ApiKeysPage.jsx'
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
              <Route path="/inquiries" element={<ProtectedRoute><InquiriesPage /></ProtectedRoute>} />
              <Route path="/users" element={<ProtectedRoute><UsersPage /></ProtectedRoute>} />
              <Route path="/keys" element={<ProtectedRoute><ApiKeysPage /></ProtectedRoute>} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
        </ErrorBoundary>
      </AuthProvider>
    </ThemeProvider>
  )
}
