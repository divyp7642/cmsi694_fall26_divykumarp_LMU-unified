import { useState } from 'react'
import './App.css'

function App() {
const [email, setEmail] = useState('')
const [otp, setOtp] = useState('')
const [otpSent, setOtpSent] = useState(false)
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
      setOtpSent(true)
    } catch {
      setMessage('Unable to contact the login service')
      setIsError(true)
    } finally {
      setIsLoading(false)
    }
  }
async function handleVerify(event) {
  event.preventDefault()

  if (!/^\d{6}$/.test(otp.trim())) {
    setMessage('Enter the 6-digit OTP')
    setIsError(true)
    return
  }

  setIsLoading(true)
  setMessage('')

  try {
    const response = await fetch('http://localhost:8080/api/auth/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: email.trim(),
        otp: otp.trim(),
      }),
    })

    const data = await response.json()
    setMessage(data.message)
    setIsError(!response.ok)
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
        <h1>{otpSent ? 'Verify your code' : 'Begin secure login'}</h1>

<p className="subtitle">
  {otpSent
    ? `Enter the verification code sent to ${email}.`
    : 'Enter your LMU email address to continue.'}
</p>

        {!otpSent ? (
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
      {isLoading ? 'Sending...' : 'Continue'}
    </button>
  </form>
) : (
  <form onSubmit={handleVerify} noValidate>
    <label htmlFor="otp">Verification code</label>
    <input
      id="otp"
      type="text"
      value={otp}
      onChange={(event) => setOtp(event.target.value)}
      placeholder="Enter 6-digit OTP"
      inputMode="numeric"
      maxLength={6}
      autoComplete="one-time-code"
    />

    <button type="submit" disabled={isLoading}>
      {isLoading ? 'Verifying...' : 'Verify OTP'}
    </button>
  </form>
)}

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