import { useEffect, useState } from 'react'
import Navbar from '../components/Navbar.jsx'
import client from '../api/client.js'

export default function InquiriesPage() {
  const [inquiries, setInquiries] = useState([])
  const [loading, setLoading] = useState(true)
  const [answerDrafts, setAnswerDrafts] = useState({})
  const [submittingId, setSubmittingId] = useState(null)

  const load = () => {
    setLoading(true)
    client.get('/admin/inquiries')
      .then((res) => setInquiries(res.data))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const submitAnswer = async (id) => {
    const answer = (answerDrafts[id] ?? '').trim()
    if (!answer) return
    setSubmittingId(id)
    try {
      await client.post(`/admin/inquiries/${id}/answer`, { answer })
      load()
    } finally {
      setSubmittingId(null)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar />
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-6">문의 관리</h1>

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">불러오는 중...</p>
        ) : inquiries.length === 0 ? (
          <p className="text-sm text-gray-400 dark:text-gray-500">문의가 없습니다.</p>
        ) : (
          <div className="space-y-4">
            {inquiries.map((inq) => (
              <div key={inq.id} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-5">
                <div className="flex items-center justify-between mb-2">
                  <h2 className="font-semibold text-gray-900 dark:text-white">{inq.title}</h2>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                    inq.status === 'ANSWERED'
                      ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
                      : 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
                  }`}>
                    {inq.status === 'ANSWERED' ? '답변완료' : '대기중'}
                  </span>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-300 whitespace-pre-wrap mb-3">{inq.content}</p>

                {inq.status === 'ANSWERED' ? (
                  <div className="bg-gray-50 dark:bg-gray-800 rounded-xl p-3 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                    {inq.answer}
                  </div>
                ) : (
                  <div className="space-y-2">
                    <textarea
                      value={answerDrafts[inq.id] ?? ''}
                      onChange={(e) => setAnswerDrafts({ ...answerDrafts, [inq.id]: e.target.value })}
                      placeholder="답변을 입력하세요"
                      rows={3}
                      className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent"
                    />
                    <button
                      onClick={() => submitAnswer(inq.id)}
                      disabled={submittingId === inq.id}
                      className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-50"
                    >
                      {submittingId === inq.id ? '전송 중...' : '답변 등록'}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
