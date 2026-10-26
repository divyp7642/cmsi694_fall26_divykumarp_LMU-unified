import { useEffect, useState } from 'react'
import './App.css'
const API_URL = 'http://localhost:8080'
const navigation = [
  { id: 'dashboard', icon: '◫', label: 'Dashboard' },
  { id: 'explore', icon: '▦', label: 'Explore Events' },
  { id: 'rsvps', icon: '✓', label: 'My RSVPs' },
  { id: 'profile', icon: '◎', label: 'Profile' },
]
function App() {
  const [email, setEmail] = useState(
    () => sessionStorage.getItem('lmuEmail') || ''
  )
  const [otp, setOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [message, setMessage] = useState('')
  const [isError, setIsError] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isAuthenticated, setIsAuthenticated] = useState(
    () => Boolean(sessionStorage.getItem('lmuToken'))
  )

  // SCRUM-20 - Administrator dashboard and role detection
  const [userRole, setUserRole] = useState(null)
  const [adminDashboard, setAdminDashboard] = useState(null)
  const [adminDashboardError, setAdminDashboardError] = useState('')
  const [isAdminDashboardLoading, setIsAdminDashboardLoading] = useState(false)
  const isAdmin = userRole === 'ADMIN'
  const currentNavigation = isAdmin
    ? navigation.filter((item) => item.id !== 'rsvps')
    : navigation

  // SCRUM-19 - Student Dashboard
  const [studentDashboard, setStudentDashboard] = useState(null)
  const [isStudentDashboardLoading, setIsStudentDashboardLoading] =
    useState(false)
  const [studentDashboardError, setStudentDashboardError] = useState('')

  // SCRUM-19 - Registered Events
  const [registeredEvents, setRegisteredEvents] = useState([])
  const [isRegisteredEventsLoading, setIsRegisteredEventsLoading] =
    useState(false)
  const [registeredEventsError, setRegisteredEventsError] = useState('')
  const [registrationVersion, setRegistrationVersion] = useState(0)

  // Modern Portal Navigation
  const [activePage, setActivePage] = useState('dashboard')
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  // SCRUM-15 - Event Discovery
  const [events, setEvents] = useState([])
  const [eventsError, setEventsError] = useState('')

  // SCRUM-17 - Event Details
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [isDetailsLoading, setIsDetailsLoading] = useState(false)

  // SCRUM-16 - Search and Filter
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [selectedDate, setSelectedDate] = useState('')

  // SCRUM-18 - RSVP
  const [hasRsvp, setHasRsvp] = useState(false)
  const [rsvpCount, setRsvpCount] = useState(0)
  const [isRsvpLoading, setIsRsvpLoading] = useState(false)
  const [isRsvpStatusLoading, setIsRsvpStatusLoading] = useState(false)
  const [rsvpMessage, setRsvpMessage] = useState('')
  const [rsvpError, setRsvpError] = useState(false)

  function getAuthHeaders() {
    const token = sessionStorage.getItem('lmuToken')
    return {
      Authorization: `Bearer ${token || ''}`,
    }
  }

  function clearLocalSession() {
    sessionStorage.removeItem('lmuToken')
    sessionStorage.removeItem('lmuAuthenticated')
    sessionStorage.removeItem('lmuEmail')
    setIsAuthenticated(false)
    setEmail('')
    setOtp('')
    setOtpSent(false)
    setUserRole(null)
    setAdminDashboard(null)
    setAdminDashboardError('')
    setIsAdminDashboardLoading(false)
    setStudentDashboard(null)
    setStudentDashboardError('')
    setRegisteredEvents([])
    setRegisteredEventsError('')
    setRegistrationVersion(0)
    setEvents([])
    setEventsError('')
    setSelectedEvent(null)
    setSearchTerm('')
    setSelectedCategory('All')
    setSelectedDate('')
    setHasRsvp(false)
    setRsvpCount(0)
    setRsvpMessage('')
    setRsvpError(false)
    setActivePage('dashboard')
    setIsSidebarOpen(false)
  }

  function handleSessionExpired() {
    clearLocalSession()
    setMessage('Your session has expired. Please log in again.')
    setIsError(true)
  }

  // SCRUM-20 - Determine role using the protected backend endpoints.

  // Admin is checked first so a valid administrator never hits student-only APIs.

  useEffect(() => {
    if (!isAuthenticated) return
    let cancelled = false
    async function detectRole() {
      setUserRole(null)
      setAdminDashboard(null)
      setAdminDashboardError('')
      setIsAdminDashboardLoading(true)
      try {
        const response = await fetch(`${API_URL}/api/admin/dashboard`, {
          headers: getAuthHeaders(),
        })
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (response.ok) {
          const data = await response.json()
          if (!cancelled) {
            setAdminDashboard(data)
            setUserRole('ADMIN')
          }
          return
        }
        if (response.status === 403) {

          // A student is expected to receive 403 on the admin endpoint.
          const studentResponse = await fetch(`${API_URL}/api/student/dashboard`, {
            headers: getAuthHeaders(),
          })
          if (cancelled) return
          if (studentResponse.status === 401) {
            handleSessionExpired()
            return
          }
          if (studentResponse.ok) {
            setUserRole('STUDENT')
          } else {
            setAdminDashboardError('Your account does not have dashboard access.')
          }
          return
        }
        setAdminDashboardError('Unable to determine your account role.')
      } catch {
        if (!cancelled) setAdminDashboardError('Unable to contact the authentication service.')
      } finally {
        if (!cancelled) setIsAdminDashboardLoading(false)
      }
    }
    detectRole()
    return () => { cancelled = true }
  }, [isAuthenticated])

  // Load Student Dashboard

  useEffect(() => {
    if (!isAuthenticated || userRole !== 'STUDENT') return
    let cancelled = false
    async function loadStudentDashboard() {
      setIsStudentDashboardLoading(true)
      setStudentDashboardError('')
      try {
        const response = await fetch(
          `${API_URL}/api/student/dashboard`,
          {
            method: 'GET',
            headers: getAuthHeaders(),
          }
        )
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (response.status === 403) {
          setStudentDashboardError(
            'Access denied. Student role required.'
          )
          return
        }
        if (!response.ok) {
          throw new Error('Unable to load dashboard')
        }
        const data = await response.json()
        if (!cancelled) {
          setStudentDashboard(data)
        }
      } catch {
        if (!cancelled) {
          setStudentDashboardError(
            'Unable to load student dashboard.'
          )
        }
      } finally {
        if (!cancelled) {
          setIsStudentDashboardLoading(false)
        }
      }
    }
    loadStudentDashboard()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, userRole])

  // Load Registered Events

  useEffect(() => {
    if (!isAuthenticated || userRole !== 'STUDENT') return
    let cancelled = false
    async function loadRegisteredEvents() {
      setIsRegisteredEventsLoading(true)
      setRegisteredEventsError('')
      try {
        const response = await fetch(
          `${API_URL}/api/student/registered-events`,
          {
            method: 'GET',
            headers: getAuthHeaders(),
          }
        )
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (response.status === 403) {
          setRegisteredEventsError(
            'Access denied. Student role required.'
          )
          return
        }
        if (!response.ok) {
          throw new Error('Unable to load registered events')
        }
        const data = await response.json()
        if (!cancelled) {
          setRegisteredEvents(
            Array.isArray(data) ? data : []
          )
        }
      } catch {
        if (!cancelled) {
          setRegisteredEventsError(
            'Unable to load your registered events.'
          )
        }
      } finally {
        if (!cancelled) {
          setIsRegisteredEventsLoading(false)
        }
      }
    }
    loadRegisteredEvents()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, userRole, registrationVersion])

  // Load Upcoming Events

  useEffect(() => {
    if (!isAuthenticated) return
    let cancelled = false
    async function loadEvents() {
      setEventsError('')
      try {
        const response = await fetch(`${API_URL}/api/events`)
        if (!response.ok) {
          throw new Error('Unable to load events')
        }
        const data = await response.json()
        if (!cancelled) {
          setEvents(Array.isArray(data) ? data : [])
        }
      } catch {
        if (!cancelled) {
          setEventsError('Unable to load upcoming events.')
        }
      }
    }
    loadEvents()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated])

  // Request OTP

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
      setMessage(
        'Use your @lmu.edu or @lion.lmu.edu email'
      )
      setIsError(true)
      return
    }
    setIsLoading(true)
    setMessage('')
    try {
      const response = await fetch(
        `${API_URL}/api/auth/login`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: email.trim(),
          }),
        }
      )
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

  // Verify OTP

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
      const response = await fetch(
        `${API_URL}/api/auth/verify`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: email.trim(),
            otp: otp.trim(),
          }),
        }
      )
      const data = await response.json()
      if (!response.ok) {
        setMessage(
          data.message || 'OTP verification failed'
        )
        setIsError(true)
        return
      }
      if (!data.token) {
        throw new Error('Session token missing')
      }
      sessionStorage.setItem('lmuToken', data.token)
      sessionStorage.setItem(
        'lmuAuthenticated',
        'true'
      )
      sessionStorage.setItem('lmuEmail', data.email)
      setEmail(data.email)
      setIsAuthenticated(true)
      setMessage('')
      setIsError(false)
      setOtp('')
    } catch {
      setMessage('Unable to complete authentication')
      setIsError(true)
    } finally {
      setIsLoading(false)
    }
  }

  // Logout

  async function handleLogout() {
    const token = sessionStorage.getItem('lmuToken')
    clearLocalSession()
    setMessage('You have been logged out')
    setIsError(false)
    if (token) {
      try {
        await fetch(`${API_URL}/api/auth/logout`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
      } catch {

        // Local session has already been cleared.
      }
    }
  }

  // Sidebar Navigation

  function navigate(page) {
    setActivePage(page)
    setSelectedEvent(null)
    setMessage('')
    setIsSidebarOpen(false)
    window.scrollTo(0, 0)
  }

  function clearFilters() {
    setSearchTerm('')
    setSelectedCategory('All')
    setSelectedDate('')
  }

  // Load RSVP Status

  async function loadRsvpStatus(eventId) {
    setIsRsvpStatusLoading(true)
    setRsvpMessage('')
    setRsvpError(false)
    try {
      const response = await fetch(
        `${API_URL}/api/events/${eventId}/rsvp`,
        {
          method: 'GET',
          headers: getAuthHeaders(),
        }
      )
      if (response.status === 401) {
        handleSessionExpired()
        return
      }
      if (!response.ok) {
        throw new Error('Unable to load RSVP status')
      }
      const data = await response.json()
      setHasRsvp(Boolean(data.hasRsvp))
      setRsvpCount(data.rsvpCount)
    } catch {
      setRsvpMessage('Unable to load RSVP information.')
      setRsvpError(true)
    } finally {
      setIsRsvpStatusLoading(false)
    }
  }

  // View Event Details

  async function handleViewDetails(event) {
    setMessage('')
    setIsError(false)
    setIsDetailsLoading(true)
    setSelectedEvent(null)
    setHasRsvp(false)
    setRsvpCount(0)
    setRsvpMessage('')
    try {
      const response = await fetch(
        `${API_URL}/api/events/${event.id}`
      )
      if (response.status === 404) {
        setMessage('This event is no longer available.')
        setIsError(true)
        return
      }
      if (!response.ok) {
        throw new Error('Unable to load event details')
      }
      const data = await response.json()
      setSelectedEvent(data)
      if (userRole === 'STUDENT') await loadRsvpStatus(data.id)
      window.scrollTo(0, 0)
    } catch {
      setSelectedEvent(null)
      setMessage(
        'Unable to load event details. Please try again.'
      )
      setIsError(true)
    } finally {
      setIsDetailsLoading(false)
    }
  }

  // Register RSVP

  async function handleRsvp() {
    if (!selectedEvent || isRsvpLoading) return
    setIsRsvpLoading(true)
    setRsvpMessage('')
    setRsvpError(false)
    try {
      const response = await fetch(
        `${API_URL}/api/events/${selectedEvent.id}/rsvp`,
        {
          method: 'POST',
          headers: getAuthHeaders(),
        }
      )
      if (response.status === 401) {
        handleSessionExpired()
        return
      }
      const data = await response.json()
      if (!response.ok) {
        setRsvpMessage(
          data.message || 'Unable to RSVP to this event.'
        )
        setRsvpError(true)
        if (response.status === 409) {
          await loadRsvpStatus(selectedEvent.id)
          setRsvpMessage(
            "You have already RSVP'd to this event."
          )
          setRsvpError(true)
        }
        return
      }
      setHasRsvp(true)
      setRsvpCount(data.rsvpCount)
      setRsvpMessage(data.message)
      setRsvpError(false)
      setRegistrationVersion(
        (version) => version + 1
      )
    } catch {
      setRsvpMessage(
        'Unable to contact the RSVP service.'
      )
      setRsvpError(true)
    } finally {
      setIsRsvpLoading(false)
    }
  }

  // Cancel RSVP

  async function handleCancelRsvp() {
    if (!selectedEvent || isRsvpLoading) return
    setIsRsvpLoading(true)
    setRsvpMessage('')
    setRsvpError(false)
    try {
      const response = await fetch(
        `${API_URL}/api/events/${selectedEvent.id}/rsvp`,
        {
          method: 'DELETE',
          headers: getAuthHeaders(),
        }
      )
      if (response.status === 401) {
        handleSessionExpired()
        return
      }
      const data = await response.json()
      if (!response.ok) {
        setRsvpMessage(
          data.message || 'Unable to cancel RSVP.'
        )
        setRsvpError(true)
        return
      }
      setHasRsvp(false)
      setRsvpCount(data.rsvpCount)
      setRsvpMessage(data.message)
      setRsvpError(false)
      setRegistrationVersion(
        (version) => version + 1
      )
    } catch {
      setRsvpMessage(
        'Unable to contact the RSVP service.'
      )
      setRsvpError(true)
    } finally {
      setIsRsvpLoading(false)
    }
  }

  // Search and Filter
  const filteredEvents = events.filter((event) => {
    const search = searchTerm.trim().toLowerCase()
    const matchesSearch = [
      event.title,
      event.description,
      event.location,
      event.category,
    ].some((value) =>
      String(value || '').toLowerCase().includes(search)
    )
    const matchesCategory =
      selectedCategory === 'All' ||
      event.category === selectedCategory
    const matchesDate =
      selectedDate === '' ||
      event.date === selectedDate
    return (
      matchesSearch &&
      matchesCategory &&
      matchesDate
    )
  })
  const categories = [
    'All',
    ...new Set(
      events.map((event) => event.category).filter(Boolean)
    ),
  ]
  const eventUnavailable = Boolean(
    selectedEvent &&
      (
        selectedEvent.status !== 'ACTIVE' ||
        selectedEvent.date <
          new Date().toLocaleDateString('en-CA')
      )
  )

  // Event Card

  function renderEventCard(event, registered = false) {
    return (
      <article className="portal-event-card" key={event.id}>
        <div className="portal-event-card-top">
          <span className="portal-category">
            {event.category}
          </span>
          {registered && (
            <span className="portal-confirmed">
              ✓ Registered
            </span>
          )}
        </div>
        <h3>{event.title}</h3>
        {event.description && (
          <p className="portal-event-description">
            {event.description}
          </p>
        )}
        <div className="portal-event-meta">
          <span>▦ &nbsp;{event.date}</span>
          <span>◷ &nbsp;{event.time}</span>
          <span>⌖ &nbsp;{event.location}</span>
        </div>
        <button
          type="button"
          className="portal-btn portal-btn-outline"
          onClick={() => handleViewDetails(event)}
          disabled={isDetailsLoading}
        >
          {isDetailsLoading
            ? 'Loading...'
            : 'View Details →'}
        </button>
      </article>
    )
  }

  // My Registered Events

  function renderRegisteredSection(compact = false) {
    const displayedEvents = compact
      ? registeredEvents.slice(0, 3)
      : registeredEvents
    return (
      <section className="portal-section">
        <div className="portal-section-heading">
          <div>
            <h2>
              {compact
                ? 'My Registered Events'
                : 'My RSVPs'}
            </h2>
            <p>
              Keep track of the events you've signed up for.
            </p>
          </div>
          {compact && (
            <button
              type="button"
              className="portal-text-btn"
              onClick={() => navigate('rsvps')}
            >
              View all →
            </button>
          )}
        </div>
        {isRegisteredEventsLoading ? (
          <p className="portal-muted">
            Loading your registered events...
          </p>
        ) : registeredEventsError ? (
          <p
            className="portal-alert portal-error"
            role="alert"
          >
            {registeredEventsError}
          </p>
        ) : registeredEvents.length === 0 ? (
          <div className="portal-empty">
            <span className="portal-empty-icon">◇</span>
            <h3>No registrations yet</h3>
            <p>
              Explore campus events and RSVP to see them here.
            </p>
            <button
              type="button"
              className="portal-btn"
              onClick={() => navigate('explore')}
            >
              Explore Events
            </button>
          </div>
        ) : (
          <div className="portal-event-grid">
            {displayedEvents.map((event) =>
              renderEventCard(event, true)
            )}
          </div>
        )}
      </section>
    )
  }

  // Explore Events

  function renderExploreSection(compact = false) {
    const displayedEvents = compact
      ? events.slice(0, 3)
      : filteredEvents
    return (
      <section className="portal-section">
        <div className="portal-section-heading">
          <div>
            <h2>
              {compact
                ? 'Upcoming Campus Events'
                : 'Explore Events'}
            </h2>
            <p>
              Find something great happening at LMU.
            </p>
          </div>
          {compact && (
            <button
              type="button"
              className="portal-text-btn"
              onClick={() => navigate('explore')}
            >
              Explore all →
            </button>
          )}
        </div>
        {!compact && (
          <div className="portal-filters">
            <input
              type="search"
              aria-label="Search events"
              placeholder="Search events..."
              value={searchTerm}
              onChange={(event) =>
                setSearchTerm(event.target.value)
              }
            />
            <select
              aria-label="Event category"
              value={selectedCategory}
              onChange={(event) =>
                setSelectedCategory(event.target.value)
              }
            >
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category === 'All'
                    ? 'All Categories'
                    : category}
                </option>
              ))}
            </select>
            <input
              type="date"
              aria-label="Event date"
              value={selectedDate}
              onChange={(event) =>
                setSelectedDate(event.target.value)
              }
            />
            <button
              type="button"
              className="portal-btn portal-btn-outline"
              onClick={clearFilters}
            >
              Clear Filters
            </button>
          </div>
        )}
        {eventsError && (
          <p
            className="portal-alert portal-error"
            role="alert"
          >
            {eventsError}
          </p>
        )}
        {message && isError && (
          <p
            className="portal-alert portal-error"
            role="alert"
          >
            {message}
          </p>
        )}
        <div className="portal-event-grid">
          {displayedEvents.map((event) =>
            renderEventCard(event)
          )}
        </div>
        {!eventsError && displayedEvents.length === 0 && (
          <div className="portal-empty">
            <h3>No events found</h3>
            <p>
              {compact
                ? 'Check back soon for new campus events.'
                : 'Try a different search or clear your filters.'}
            </p>
          </div>
        )}
      </section>
    )
  }

  // Login Screen
  if (!isAuthenticated) {
    return (
      <main className="portal-login">
        <div className="portal-login-brand">
          <span className="portal-logo">L</span>
          <span>
            LMU <strong>Unified</strong>
          </span>
        </div>
        <section className="portal-login-card">
          <div className="portal-login-emblem">✦</div>
          <p className="portal-eyebrow">
            WELCOME TO LMU UNIFIED
          </p>
          <h1>
            {otpSent
              ? 'Verify your code'
              : 'Your campus, connected.'}
          </h1>
          <p className="portal-muted">
            {otpSent
              ? `Enter the verification code sent to ${email}.`
              : 'Sign in with your LMU email to discover events and manage your RSVPs.'}
          </p>
          {!otpSent ? (
            <form onSubmit={handleSubmit} noValidate>
              <label htmlFor="email">
                LMU email address
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) =>
                  setEmail(event.target.value)
                }
                placeholder="name@lmu.edu"
                autoComplete="email"
              />
              <button
                className="portal-btn portal-btn-wide"
                type="submit"
                disabled={isLoading}
              >
                {isLoading ? 'Sending...' : 'Continue →'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerify} noValidate>
              <label htmlFor="otp">
                Verification code
              </label>
              <input
                id="otp"
                type="text"
                value={otp}
                onChange={(event) =>
                  setOtp(event.target.value)
                }
                placeholder="6-digit OTP"
                inputMode="numeric"
                maxLength={6}
                autoComplete="one-time-code"
              />
              <button
                className="portal-btn portal-btn-wide"
                type="submit"
                disabled={isLoading}
              >
                {isLoading
                  ? 'Verifying...'
                  : 'Verify OTP →'}
              </button>
              <button
                className="portal-text-btn"
                type="button"
                onClick={() => {
                  setOtpSent(false)
                  setOtp('')
                  setMessage('')
                }}
              >
                Use another email
              </button>
            </form>
          )}
          {message && (
            <p
              className={`portal-alert ${
                isError
                  ? 'portal-error'
                  : 'portal-success'
              }`}
              role="status"
            >
              {message}
            </p>
          )}
        </section>

        <p className="portal-login-footer">
          Loyola Marymount University · Student Events Portal
        </p>
      </main>
    )
  }

  // Authenticated Student Portal
  return (
    <div className="portal-app">
      {isSidebarOpen && (
        <button
          type="button"
          className="portal-overlay"
          aria-label="Close menu"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}
      {/* Left Sidebar */}
      <aside
        className={`portal-sidebar ${
          isSidebarOpen ? 'portal-sidebar-open' : ''
        }`}
      >
        <div className="portal-sidebar-brand">
          <span className="portal-logo">L</span>
          <div>
            <strong>LMU Unified</strong>
            <small>{isAdmin ? 'ADMIN PORTAL' : 'STUDENT PORTAL'}</small>
          </div>
          <button
            type="button"
            className="portal-sidebar-close"
            aria-label="Close navigation"
            onClick={() => setIsSidebarOpen(false)}
          >
            ×
          </button>
        </div>
        <p className="portal-nav-label">
          WORKSPACE
        </p>
        <nav
          className="portal-nav"
          aria-label={isAdmin ? "Administrator navigation" : "Student navigation"}
        >
          {currentNavigation.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`portal-nav-item ${
                activePage === item.id
                  ? 'portal-nav-active'
                  : ''
              }`}
              onClick={() => navigate(item.id)}
            >
              <span
                className="portal-nav-icon"
                aria-hidden="true"
              >
                {item.icon}
              </span>
              <span>{item.label}</span>
              {item.id === 'rsvps' &&
                registeredEvents.length > 0 && (
                  <span className="portal-nav-count">
                    {registeredEvents.length}
                  </span>
                )}
            </button>
          ))}
        </nav>

        <div className="portal-sidebar-bottom">
          <div className="portal-sidebar-user">
            <span className="portal-avatar">
              {(email || 'S')[0].toUpperCase()}
            </span>
            <div>
              <strong>{isAdmin ? 'Administrator Account' : 'Student Account'}</strong>
              <small title={email}>{email}</small>
            </div>
          </div>
          <button
            type="button"
            className="portal-nav-item portal-logout"
            onClick={handleLogout}
          >
            <span
              className="portal-nav-icon"
              aria-hidden="true"
            >
              ↪
            </span>
            Log out
          </button>
        </div>
      </aside>

      {/* Main Application */}
      <div className="portal-main">
        <header className="portal-topbar">
          <button
            type="button"
            className="portal-menu-btn"
            aria-label="Open navigation"
            onClick={() => setIsSidebarOpen(true)}
          >
            ☰
          </button>
          <div className="portal-breadcrumb">
            LMU Unified <span>/</span>{' '}
            {selectedEvent
              ? 'Event Details'
              : currentNavigation.find(
                  (item) => item.id === activePage
                )?.label}
          </div>
          <div className="portal-topbar-right">
            <span className="portal-role-pill">
              {userRole || 'Checking...'}
            </span>
            <span className="portal-avatar portal-avatar-small">
              {(email || 'S')[0].toUpperCase()}
            </span>
          </div>
        </header>

        <main className="portal-content">
          {/* Event Details */}
          {userRole === null ? (
            <section className="portal-panel">
              <h2>Checking account access</h2>
              <p className="portal-muted">
                {isAdminDashboardLoading ? 'Loading your account role...' : (adminDashboardError || 'Please wait...')}
              </p>
            </section>
          ) : selectedEvent ? (
            <>
              <button
                type="button"
                className="portal-text-btn portal-back"
                onClick={() => {
                  setSelectedEvent(null)
                  setRsvpMessage('')
                  setRsvpError(false)
                  window.scrollTo(0, 0)
                }}
              >
                ← Back to{' '}
                {activePage === 'rsvps'
                  ? 'My RSVPs'
                  : activePage === 'dashboard'
                    ? 'Dashboard'
                    : 'Events'}
              </button>
              <div className="portal-page-heading">
                <span className="portal-category">
                  {selectedEvent.category}
                </span>
                <h1>{selectedEvent.title}</h1>
                <p>{selectedEvent.description}</p>
              </div>
              {selectedEvent.status === 'CANCELLED' && (
                <p className="portal-alert portal-error">
                  This event has been cancelled.
                  Please check other upcoming events.
                </p>
              )}
              <div className="portal-detail-grid">
                <section className="portal-panel">
                  <h2>Event Information</h2>
                  <div className="portal-info-row">
                    <span>Date</span>
                    <strong>{selectedEvent.date}</strong>
                  </div>
                  <div className="portal-info-row">
                    <span>Time</span>
                    <strong>{selectedEvent.time}</strong>
                  </div>
                  <div className="portal-info-row">
                    <span>Location</span>
                    <strong>
                      {selectedEvent.location}
                    </strong>
                  </div>
                  <div className="portal-info-row">
                    <span>Category</span>
                    <strong>
                      {selectedEvent.category}
                    </strong>
                  </div>
                  <div className="portal-info-row">
                    <span>Organizer</span>
                    <strong>
                      {selectedEvent.organizer || 'LMU'}
                    </strong>
                  </div>
                </section>

                {isAdmin ? (
                  <section className="portal-panel">
                  <h2>Event Registrations</h2>

                    <div className="portal-info-row">
                    <span>Registered attendees</span>
                    <strong>
                      {adminDashboard?.events?.find(
                        (event) => String(event.id) === String(selectedEvent.id)
                      )?.rsvpCount ?? 0}
                    </strong>
                    </div>

                    <p className="portal-muted">
                      Total students registered for this event.
                    </p>
                  </section>
                ) : (
                <section className="portal-panel">
                  <h2>Event RSVP</h2>
                  {isRsvpStatusLoading ? (
                    <p className="portal-muted">
                      Loading RSVP status...
                    </p>
                  ) : (
                    <>
                      <div className="portal-info-row">
                        <span>Registered attendees</span>
                        <strong>{rsvpCount}</strong>
                      </div>
                      <div className="portal-info-row">
                        <span>Your RSVP status</span>
                        <strong
                          className={
                            hasRsvp
                              ? 'portal-status-confirmed'
                              : ''
                          }
                        >
                          {hasRsvp
                            ? '✓ Confirmed'
                            : 'Not registered'}
                        </strong>
                      </div>
                      {eventUnavailable && !hasRsvp ? (
                        <p className="portal-alert portal-error">
                          RSVP is unavailable for this event.
                        </p>
                      ) : (
                        <button
                          type="button"
                          className={`portal-btn portal-btn-wide ${
                            hasRsvp
                              ? 'portal-btn-danger'
                              : ''
                          }`}
                          onClick={
                            hasRsvp
                              ? handleCancelRsvp
                              : handleRsvp
                          }
                          disabled={isRsvpLoading}
                        >
                          {isRsvpLoading
                            ? 'Processing...'
                            : hasRsvp
                              ? 'Cancel RSVP'
                              : 'RSVP to Event'}
                        </button>
                      )}
                    </>
                  )}
                  {rsvpMessage && (
                    <p
                      className={`portal-alert ${
                        rsvpError
                          ? 'portal-error'
                          : 'portal-success'
                      }`}
                      role="status"
                    >
                      {rsvpMessage}
                    </p>
                  )}
                </section>
                )}
              </div>
            </>
          ) : isAdmin && activePage === 'dashboard' ? (
            <>
              <div className="portal-welcome">
                <div>
                  <p className="portal-eyebrow">ADMINISTRATOR WORKSPACE</p>
                  <h1>Admin Dashboard<span className="portal-welcome-dot">.</span></h1>
                  <p>Manage your overview of campus events and registrations.</p>
                </div>
                <div className="portal-welcome-decoration" aria-hidden="true">✦</div>
              </div>
              {adminDashboardError && (
                <p className="portal-alert portal-error" role="alert">{adminDashboardError}</p>
              )}
              <div className="portal-stats">
                <div className="portal-stat">
                  <span className="portal-stat-icon">▦</span>
                  <div>
                    <strong>{adminDashboard?.totalEvents ?? events.length}</strong>
                    <span>Upcoming Events</span>
                  </div>
                </div>
                <div className="portal-stat">
                  <span className="portal-stat-icon">✓</span>
                  <div>
                    <strong>{adminDashboard?.totalRsvps ?? 0}</strong>
                    <span>Total RSVPs</span>
                  </div>
                </div>
                <div className="portal-stat">
                  <span className="portal-stat-icon">◎</span>
                  <div><strong>ADMIN</strong><span>Account Role</span></div>
                </div>
              </div>
              <section className="portal-section">
                <div className="portal-section-heading">
                  <div><h2>Campus Events</h2><p>Events currently available on LMU Unified.</p></div>
                  <button type="button" className="portal-text-btn" onClick={() => navigate('explore')}>
                    View all →
                  </button>
                </div>
                <div className="portal-event-grid">
                  {(Array.isArray(adminDashboard?.events) && adminDashboard.events.length
                    ? adminDashboard.events.map((entry) => entry.event || entry)
                    : events).slice(0, 3).map((event) => renderEventCard(event))}
                </div>
              </section>
            </>
          ) : activePage === 'dashboard' ? (
            <>
              {/* Dashboard Overview */}
              <div className="portal-welcome">
                <div>
                  <p className="portal-eyebrow">
                    YOUR CAMPUS HUB
                  </p>
                  <h1>
                    Welcome back
                    <span className="portal-welcome-dot">
                      .
                    </span>
                  </h1>
                  <p>
                    Explore what's happening on campus
                    and stay connected with LMU.
                  </p>
                </div>
                <div
                  className="portal-welcome-decoration"
                  aria-hidden="true"
                >
                  ✦
                </div>
              </div>
              {isStudentDashboardLoading && (
                <p className="portal-muted">
                  Loading your dashboard...
                </p>
              )}
              {studentDashboardError && (
                <p
                  className="portal-alert portal-error"
                  role="alert"
                >
                  {studentDashboardError}
                </p>
              )}
              <div className="portal-stats">
                <div className="portal-stat">
                  <span className="portal-stat-icon">
                    ✓
                  </span>
                  <div>
                    <strong>
                      {registeredEvents.length}
                    </strong>
                    <span>My RSVPs</span>
                  </div>
                </div>
                <div className="portal-stat">
                  <span className="portal-stat-icon">
                    ▦
                  </span>
                  <div>
                    <strong>{events.length}</strong>
                    <span>Upcoming Events</span>
                  </div>
                </div>
                <div className="portal-stat">
                  <span className="portal-stat-icon">
                    ◎
                  </span>
                  <div>
                    <strong>
                      {studentDashboard?.role || 'Student'}
                    </strong>
                    <span>Account Role</span>
                  </div>
                </div>
              </div>
              {renderRegisteredSection(true)}
              {renderExploreSection(true)}
            </>
          ) : activePage === 'explore' ? (
            <>
              {/* Explore Events */}
              <div className="portal-page-heading">
                <p className="portal-eyebrow">
                  DISCOVER LMU
                </p>
                <h1>Explore Events</h1>
                <p>
                  Find events, connect with peers,
                  and make the most of campus life.
                </p>
              </div>
              {renderExploreSection()}
            </>
          ) : !isAdmin && activePage === 'rsvps' ? (
            <>
              {/* My RSVPs */}
              <div className="portal-page-heading">
                <p className="portal-eyebrow">
                  YOUR CALENDAR
                </p>
                <h1>My RSVPs</h1>
                <p>
                  Everything you've registered for,
                  all in one place.
                </p>
              </div>
              {renderRegisteredSection()}
            </>
          ) : (
            <>
              {/* Student Profile */}
              <div className="portal-page-heading">
                <p className="portal-eyebrow">
                  ACCOUNT
                </p>
                <h1>My Profile</h1>
                <p>
                  Your LMU Unified account.
                </p>
              </div>
              <section className="portal-panel portal-profile">
                <span className="portal-avatar portal-profile-avatar">
                  {(email || 'S')[0].toUpperCase()}
                </span>
                <h2>{isAdmin ? 'Administrator Account' : 'Student Account'}</h2>
                <div className="portal-info-row">
                  <span>Email</span>
                  <strong>
                    {(isAdmin ? adminDashboard?.email : studentDashboard?.email) || email}
                  </strong>
                </div>
                <div className="portal-info-row">
                  <span>Role</span>
                  <strong>
                    {userRole || 'STUDENT'}
                  </strong>
                </div>
                <p className="portal-muted">
                  Profile details are read-only in this version.
                </p>
              </section>
            </>
          )}
          <footer className="portal-footer">
            © {new Date().getFullYear()} LMU Unified ·
            Loyola Marymount University
          </footer>
        </main>
      </div>
    </div>
  )
}
export default App
