import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('Uncaught error:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 dark:bg-gray-900 px-4">
          <p className="text-8xl font-bold text-red-400 mb-4">500</p>
          <h1 className="text-2xl font-semibold text-slate-800 dark:text-white mb-2">
            예상치 못한 오류가 발생했어요
          </h1>
          <p className="text-slate-500 dark:text-slate-400 mb-8 text-center">
            {this.state.error?.message ?? '잠시 후 다시 시도해주세요.'}
          </p>
          <button
            onClick={() => window.location.replace('/')}
            className="px-6 py-3 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium transition-colors"
          >
            홈으로 이동
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
