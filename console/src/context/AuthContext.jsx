import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [admin, setAdmin] = useState(() => {
    const saved = localStorage.getItem('av_admin_user')
    return saved ? JSON.parse(saved) : null
  })

  const login = (adminData) => {
    localStorage.setItem('av_admin_user', JSON.stringify(adminData))
    setAdmin(adminData)
  }

  const logout = () => {
    localStorage.removeItem('av_admin_user')
    setAdmin(null)
  }

  return (
    <AuthContext.Provider value={{ admin, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
