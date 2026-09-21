import { useState } from 'react'
import './App.css'

function App() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [isError, setIsError] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()

    const lmuEmailPattern =
      /^[A-Za-z0-9._%+-]+@(lion\.)?lmu\.edu$/i

    if (!email.trim()) {
      setMessage('Email is required')
      setIsError(true)
      return
    }

    if (!lmuEmailPattern.test(email.trim())) {
      setMessage('Use your @lmu.edu or @lion.lmu.edu email')
      setIsError(true)
      return
    }

    setIsLoading(true)
    setMessage('')

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: email.trim() }),
      })

      if (!response.ok) {
        throw new Error('Login request failed')
      }

      const data = await response.json()
      setMessage(data.message)
      setIsError(false)
    } catch {
      setMessage('Unable to contact the login service')
      setIsError(true)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <p className="brand">LMU Unified</p>
        <h1>Begin secure login</h1>
        <p className="subtitle">
          Enter your LMU email address to continue.
        </p>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="email">LMU email address</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="name@lmu.edu"
            autoComplete="email"
          />

          <button type="submit" disabled={isLoading}>
            {isLoading ? 'Checking...' : 'Continue'}
          </button>
        </form>

        {message && (
          <p className={isError ? 'message error' : 'message success'}>
            {message}
          </p>
        )}
      </section>
    </main>
  )
}

export default App