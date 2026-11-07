
import { useEffect, useState } from 'react'
import './App.css'
const API_URL = 'http://localhost:8080'
const navigation = [
  { id: 'dashboard', icon: '◫', label: 'Dashboard' },
  { id: 'explore', icon: '▦', label: 'Explore Events' },
  { id: 'rsvps', icon: '✓', label: 'My RSVPs' },
  { id: 'profile', icon: '◎', label: 'Profile' },
]
const emptyEventForm = {
  title: '',
  description: '',
  date: '',
  time: '',
  location: '',
  category: '',
}
const eventCategories = [
  'Academic',
  'Career',
  'Technology',
  'Entertainment',
  'Sports',
  'Social',
  'Workshop',
  'Other',
]
function localDateString() {
  const date = new Date()
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}
function formatEventTime(time) {
  const [hours, minutes] = time.split(':').map(Number)
  const period = hours >= 12 ? 'PM' : 'AM'
  const displayHour = hours % 12 || 12
  return `${displayHour}:${String(minutes).padStart(2, '0')} ${period}`
}
function App() {
  // Authentication
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
  // Account roles
  const [userRole, setUserRole] = useState(null)
  const [roleLoading, setRoleLoading] = useState(false)
  const [roleError, setRoleError] = useState('')
  const isAdmin = userRole === 'ADMIN'
  const isStudent = userRole === 'STUDENT'
  const isOrganizer = userRole === 'ORGANIZER'
  // Dashboards
  const [adminDashboard, setAdminDashboard] = useState(null)
  const [adminDashboardError, setAdminDashboardError] = useState('')
  const [isAdminDashboardLoading, setIsAdminDashboardLoading] =
    useState(false)
  const [studentDashboard, setStudentDashboard] = useState(null)
  const [studentDashboardError, setStudentDashboardError] = useState('')
  const [isStudentDashboardLoading, setIsStudentDashboardLoading] =
    useState(false)
  // SCRUM-23 - Admin event review
  const [pendingEvents, setPendingEvents] = useState([])
  const [pendingLoading, setPendingLoading] = useState(false)
  const [pendingError, setPendingError] = useState('')
  const [reviewEvent, setReviewEvent] = useState(null)
  const [reviewBusyId, setReviewBusyId] = useState(null)
  const [reviewMessage, setReviewMessage] = useState('')
  const [reviewMessageError, setReviewMessageError] = useState(false)
  const [reviewVersion, setReviewVersion] = useState(0)

  // SCRUM-24 - Organizer notifications and submitted events
  const [organizerEvents, setOrganizerEvents] = useState([])
  const [organizerNotifications, setOrganizerNotifications] = useState([])
  const [organizerDataLoading, setOrganizerDataLoading] = useState(false)
  const [organizerDataError, setOrganizerDataError] = useState('')
  const [organizerRefreshVersion, setOrganizerRefreshVersion] = useState(0)
  const [notificationBusyId, setNotificationBusyId] = useState(null)
  const [notificationReadError, setNotificationReadError] = useState('')

  // Navigation
  const [activePage, setActivePage] = useState('dashboard')
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const currentNavigation = isStudent
    ? navigation
    : navigation.filter((item) => item.id !== 'rsvps')
  // Events
  const [events, setEvents] = useState([])
  const [eventsError, setEventsError] = useState('')
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [isDetailsLoading, setIsDetailsLoading] = useState(false)
  // Search and filters
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [selectedDate, setSelectedDate] = useState('')
  // RSVP
  const [registeredEvents, setRegisteredEvents] = useState([])
  const [registeredEventsError, setRegisteredEventsError] = useState('')
  const [isRegisteredEventsLoading, setIsRegisteredEventsLoading] =
    useState(false)
  const [registrationVersion, setRegistrationVersion] = useState(0)
  const [hasRsvp, setHasRsvp] = useState(false)
  const [rsvpCount, setRsvpCount] = useState(0)
  const [isRsvpLoading, setIsRsvpLoading] = useState(false)
  const [isRsvpStatusLoading, setIsRsvpStatusLoading] = useState(false)
  const [rsvpMessage, setRsvpMessage] = useState('')
  const [rsvpError, setRsvpError] = useState(false)
  // SCRUM-22 - Organizer event submission
  const [eventForm, setEventForm] = useState({ ...emptyEventForm })
  const [eventFormErrors, setEventFormErrors] = useState({})
  const [isSubmittingEvent, setIsSubmittingEvent] = useState(false)
  const [eventSubmissionMessage, setEventSubmissionMessage] = useState('')
  const [eventSubmissionError, setEventSubmissionError] = useState(false)
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
    setRoleError('')
    setOrganizerEvents([])
    setOrganizerNotifications([])
    setOrganizerDataError('')
    setOrganizerDataLoading(false)
    setOrganizerRefreshVersion(0)
    setNotificationBusyId(null)
    setNotificationReadError('')
    setPendingEvents([])
    setPendingError('')
    setReviewEvent(null)
    setReviewMessage('')
    setReviewBusyId(null)
    setAdminDashboard(null)
    setAdminDashboardError('')
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
    setEventForm({ ...emptyEventForm })
    setEventFormErrors({})
    setIsSubmittingEvent(false)
    setEventSubmissionMessage('')
    setEventSubmissionError(false)
    setActivePage('dashboard')
    setIsSidebarOpen(false)
  }
  function handleSessionExpired() {
    clearLocalSession()
    setMessage('Your session has expired. Please log in again.')
    setIsError(true)
  }
  // SCRUM-22 - Detect role from backend
  useEffect(() => {
    if (!isAuthenticated) return
    let cancelled = false
    async function detectRole() {
      setRoleLoading(true)
      setRoleError('')
      setUserRole(null)
      try {
        const response = await fetch(`${API_URL}/api/auth/me`, {
          headers: getAuthHeaders(),
        })
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (!response.ok) {
          throw new Error('Unable to determine account role.')
        }
        const data = await response.json()
        if (!['STUDENT', 'ORGANIZER', 'ADMIN'].includes(data.role)) {
          throw new Error('Unknown account role.')
        }
        if (!cancelled) {
          setUserRole(data.role)
          if (data.email) setEmail(data.email)
        }
      } catch (error) {
        if (!cancelled) {
          setRoleError(error.message || 'Unable to load account role.')
        }
      } finally {
        if (!cancelled) setRoleLoading(false)
      }
    }
    detectRole()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated])
  // SCRUM-24 - Load data for the authenticated organizer only.
  useEffect(() => {
    if (!isAuthenticated || userRole !== 'ORGANIZER') return
    let cancelled = false

    async function loadOrganizerData() {
      setOrganizerDataLoading(true)
      setOrganizerDataError('')
      try {
        const [eventsResponse, notificationsResponse] = await Promise.all([
          fetch(`${API_URL}/api/organizer/events`, { headers: getAuthHeaders() }),
          fetch(`${API_URL}/api/organizer/notifications`, { headers: getAuthHeaders() }),
        ])
        if (cancelled) return
        if (eventsResponse.status === 401 || notificationsResponse.status === 401) {
          handleSessionExpired()
          return
        }
        const [eventsData, notificationsData] = await Promise.all([
          eventsResponse.json(),
          notificationsResponse.json(),
        ])
        if (!eventsResponse.ok || !notificationsResponse.ok) {
          throw new Error(
            eventsData.message && !eventsResponse.ok
              ? eventsData.message
              : notificationsData.message || 'Unable to load organizer updates.'
          )
        }
        if (!cancelled) {
          setOrganizerEvents(Array.isArray(eventsData.events) ? eventsData.events : [])
          setOrganizerNotifications(
            Array.isArray(notificationsData.notifications) ? notificationsData.notifications : []
          )
        }
      } catch (error) {
        if (!cancelled) setOrganizerDataError(error.message || 'Unable to load organizer updates.')
      } finally {
        if (!cancelled) setOrganizerDataLoading(false)
      }
    }

    loadOrganizerData()
    return () => { cancelled = true }
  }, [isAuthenticated, userRole, organizerRefreshVersion])

  // Refresh decisions when the organizer returns to the browser tab.
  useEffect(() => {
    if (!isAuthenticated || userRole !== 'ORGANIZER') return
    const refreshOnFocus = () => setOrganizerRefreshVersion((value) => value + 1)
    window.addEventListener('focus', refreshOnFocus)
    return () => window.removeEventListener('focus', refreshOnFocus)
  }, [isAuthenticated, userRole])

  // Admin Dashboard
  useEffect(() => {
    if (!isAuthenticated || userRole !== 'ADMIN') return
    let cancelled = false
    async function loadAdminDashboard() {
      setIsAdminDashboardLoading(true)
      setAdminDashboardError('')
      try {
        const response = await fetch(
          `${API_URL}/api/admin/dashboard`,
          { headers: getAuthHeaders() }
        )
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (response.status === 403) {
          throw new Error('Administrator access required.')
        }
        if (!response.ok) {
          throw new Error('Unable to load admin dashboard.')
        }
        const data = await response.json()
        if (!cancelled) setAdminDashboard(data)
      } catch (error) {
        if (!cancelled) setAdminDashboardError(error.message)
      } finally {
        if (!cancelled) setIsAdminDashboardLoading(false)
      }
    }
    loadAdminDashboard()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, userRole, reviewVersion])
  // SCRUM-23 - Fetch pending organizer submissions for admins
  useEffect(() => {
    if (!isAuthenticated || userRole !== 'ADMIN') return
    let cancelled = false
    async function loadPendingEvents() {
      setPendingLoading(true)
      setPendingError('')
      try {
        const response = await fetch(`${API_URL}/api/admin/events/pending`, {
          headers: getAuthHeaders(),
        })
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        const data = await response.json()
        if (!response.ok) {
          throw new Error(data.message || 'Unable to load pending events.')
        }
        setPendingEvents(Array.isArray(data.events) ? data.events : [])
      } catch (error) {
        if (!cancelled) setPendingError(error.message || 'Unable to load pending events.')
      } finally {
        if (!cancelled) setPendingLoading(false)
      }
    }
    loadPendingEvents()
    return () => { cancelled = true }
  }, [isAuthenticated, userRole, reviewVersion])

  // SCRUM-23 - Approve or reject a pending event
  async function handleReviewDecision(eventId, decision) {
    if (!isAdmin || reviewBusyId !== null) return
    setReviewBusyId(eventId)
    setReviewMessage('')
    setReviewMessageError(false)
    try {
      const response = await fetch(
        `${API_URL}/api/admin/events/${eventId}/${decision}`,
        { method: 'PUT', headers: getAuthHeaders() }
      )
      if (response.status === 401) {
        handleSessionExpired()
        return
      }
      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.message || `Unable to ${decision} event.`)
      }
      setReviewMessage(data.message || `Event ${decision}d successfully.`)
      setReviewEvent(null)
      setReviewVersion((version) => version + 1)
    } catch (error) {
      setReviewMessage(error.message || 'Unable to process event.')
      setReviewMessageError(true)
    } finally {
      setReviewBusyId(null)
    }
  }

  // Student Dashboard
  useEffect(() => {
    if (!isAuthenticated || userRole !== 'STUDENT') return
    let cancelled = false
    async function loadStudentDashboard() {
      setIsStudentDashboardLoading(true)
      setStudentDashboardError('')
      try {
        const response = await fetch(
          `${API_URL}/api/student/dashboard`,
          { headers: getAuthHeaders() }
        )
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (response.status === 403) {
          throw new Error('Student access required.')
        }
        if (!response.ok) {
          throw new Error('Unable to load student dashboard.')
        }
        const data = await response.json()
        if (!cancelled) setStudentDashboard(data)
      } catch (error) {
        if (!cancelled) setStudentDashboardError(error.message)
      } finally {
        if (!cancelled) setIsStudentDashboardLoading(false)
      }
    }
    loadStudentDashboard()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, userRole])
  // Registered events
  useEffect(() => {
    if (!isAuthenticated || userRole !== 'STUDENT') return
    let cancelled = false
    async function loadRegisteredEvents() {
      setIsRegisteredEventsLoading(true)
      setRegisteredEventsError('')
      try {
        const response = await fetch(
          `${API_URL}/api/student/registered-events`,
          { headers: getAuthHeaders() }
        )
        if (cancelled) return
        if (response.status === 401) {
          handleSessionExpired()
          return
        }
        if (response.status === 403) {
          throw new Error('Student access required.')
        }
        if (!response.ok) {
          throw new Error('Unable to load registered events.')
        }
        const data = await response.json()
        if (!cancelled) {
          setRegisteredEvents(Array.isArray(data) ? data : [])
        }
      } catch (error) {
        if (!cancelled) setRegisteredEventsError(error.message)
      } finally {
        if (!cancelled) setIsRegisteredEventsLoading(false)
      }
    }
    loadRegisteredEvents()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, userRole, registrationVersion])
  // Public upcoming events
  useEffect(() => {
    if (!isAuthenticated) return
    let cancelled = false
    async function loadEvents() {
      setEventsError('')
      try {
        const response = await fetch(`${API_URL}/api/events`)
        if (!response.ok) {
          throw new Error('Unable to load upcoming events.')
        }
        const data = await response.json()
        if (!cancelled) {
          setEvents(Array.isArray(data) ? data : [])
        }
      } catch (error) {
        if (!cancelled) setEventsError(error.message)
      }
    }
    loadEvents()
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, reviewVersion])
  // Request OTP
  async function handleSubmit(event) {
    event.preventDefault()
    const lmuEmailPattern = /^[A-Za-z0-9._%+-]+@(lion\.)?lmu\.edu$/i
    if (!email.trim()) {
      setMessage('Email is required.')
      setIsError(true)
      return
    }
    if (!lmuEmailPattern.test(email.trim())) {
      setMessage('Use your @lmu.edu or @lion.lmu.edu email.')
      setIsError(true)
      return
    }
    setIsLoading(true)
    setMessage('')
    try {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      })
      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.message || 'Login request failed.')
      }
      setMessage(data.message || 'OTP sent successfully.')
      setIsError(false)
      setOtpSent(true)
    } catch (error) {
      setMessage(error.message || 'Unable to contact login service.')
      setIsError(true)
    } finally {
      setIsLoading(false)
    }
  }
  // Verify OTP
  async function handleVerify(event) {
    event.preventDefault()
    if (!/^\d{6}$/.test(otp.trim())) {
      setMessage('Enter the 6-digit OTP.')
      setIsError(true)
      return
    }
    setIsLoading(true)
    setMessage('')
    try {
      const response = await fetch(`${API_URL}/api/auth/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email.trim(),
          otp: otp.trim(),
        }),
      })
      const data = await response.json()
      if (!response.ok) {
        setMessage(data.message || 'OTP verification failed.')
        setIsError(true)
        return
      }
      if (!data.token) {
        throw new Error('Session token missing.')
      }
      sessionStorage.setItem('lmuToken', data.token)
      sessionStorage.setItem('lmuAuthenticated', 'true')
      sessionStorage.setItem('lmuEmail', data.email)
      setEmail(data.email)
      setIsAuthenticated(true)
      setMessage('')
      setIsError(false)
      setOtp('')
    } catch (error) {
      setMessage(error.message || 'Unable to complete authentication.')
      setIsError(true)
    } finally {
      setIsLoading(false)
    }
  }
  // Logout
  async function handleLogout() {
    const token = sessionStorage.getItem('lmuToken')
    clearLocalSession()
    setMessage('You have been logged out.')
    setIsError(false)
    if (token) {
      try {
        await fetch(`${API_URL}/api/auth/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        })
      } catch {
        // Local session was already cleared.
      }
    }
  }
  // Navigation
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
  // RSVP status
  async function loadRsvpStatus(eventId) {
    setIsRsvpStatusLoading(true)
    setRsvpMessage('')
    setRsvpError(false)
    try {
      const response = await fetch(
        `${API_URL}/api/events/${eventId}/rsvp`,
        { headers: getAuthHeaders() }
      )
      if (response.status === 401) {
        handleSessionExpired()
        return
      }
      if (!response.ok) {
        throw new Error('Unable to load RSVP status.')
      }
      const data = await response.json()
      setHasRsvp(Boolean(data.hasRsvp))
      setRsvpCount(data.rsvpCount ?? 0)
    } catch {
      setRsvpMessage('Unable to load RSVP information.')
      setRsvpError(true)
    } finally {
      setIsRsvpStatusLoading(false)
    }
  }
  // Event details
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
        throw new Error('Unable to load event details.')
      }
      const data = await response.json()
      setSelectedEvent(data)
      if (isStudent) {
        await loadRsvpStatus(data.id)
      }
      window.scrollTo(0, 0)
    } catch {
      setMessage('Unable to load event details. Please try again.')
      setIsError(true)
    } finally {
      setIsDetailsLoading(false)
    }
  }
  // RSVP
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
        setRsvpMessage(data.message || 'Unable to RSVP.')
        setRsvpError(true)
        if (response.status === 409) {
          await loadRsvpStatus(selectedEvent.id)
          setRsvpMessage("You have already RSVP'd to this event.")
          setRsvpError(true)
        }
        return
      }
      setHasRsvp(true)
      setRsvpCount(data.rsvpCount ?? 0)
      setRsvpMessage(data.message || 'RSVP confirmed.')
      setRsvpError(false)
      setRegistrationVersion((version) => version + 1)
    } catch {
      setRsvpMessage('Unable to contact the RSVP service.')
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
        setRsvpMessage(data.message || 'Unable to cancel RSVP.')
        setRsvpError(true)
        return
      }
      setHasRsvp(false)
      setRsvpCount(data.rsvpCount ?? 0)
      setRsvpMessage(data.message || 'RSVP cancelled.')
      setRsvpError(false)
      setRegistrationVersion((version) => version + 1)
    } catch {
      setRsvpMessage('Unable to contact the RSVP service.')
      setRsvpError(true)
    } finally {
      setIsRsvpLoading(false)
    }
  }
  // SCRUM-22 - Update organizer form fields
  function updateEventForm(field, value) {
    setEventForm((previous) => ({
      ...previous,
      [field]: value,
    }))
    setEventFormErrors((previous) => ({
      ...previous,
      [field]: '',
    }))
    setEventSubmissionMessage('')
  }
  // SCRUM-22 - Validate organizer event form
  function validateEventForm() {
    const errors = {}
    if (!eventForm.title.trim()) {
      errors.title = 'Event title is required.'
    }
    if (!eventForm.description.trim()) {
      errors.description = 'Event description is required.'
    }
    if (!eventForm.date) {
      errors.date = 'Event date is required.'
    } else if (eventForm.date < localDateString()) {
      errors.date = 'Event date cannot be in the past.'
    }
    if (!eventForm.time) {
      errors.time = 'Event time is required.'
    }
    if (!eventForm.location.trim()) {
      errors.location = 'Event location is required.'
    }
    if (!eventForm.category) {
      errors.category = 'Please select an event category.'
    }
    setEventFormErrors(errors)
    return Object.keys(errors).length === 0
  }
  // SCRUM-22 - Submit event for admin approval
  async function handleEventSubmission(event) {
    event.preventDefault()
    if (!isOrganizer || isSubmittingEvent) return
    setEventSubmissionMessage('')
    setEventSubmissionError(false)
    if (!validateEventForm()) {
      setEventSubmissionMessage(
        'Please correct the highlighted fields before submitting.'
      )
      setEventSubmissionError(true)
      return
    }
    setIsSubmittingEvent(true)
    try {
      const response = await fetch(
        `${API_URL}/api/organizer/events`,
        {
          method: 'POST',
          headers: {
            ...getAuthHeaders(),
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            title: eventForm.title.trim(),
            description: eventForm.description.trim(),
            date: eventForm.date,
            time: formatEventTime(eventForm.time),
            location: eventForm.location.trim(),
            category: eventForm.category,
          }),
        }
      )
      if (response.status === 401) {
        handleSessionExpired()
        return
      }
      const data = await response.json()
      if (!response.ok) {
        setEventSubmissionMessage(
          data.message || 'Unable to submit event.'
        )
        setEventSubmissionError(true)
        return
      }
      setEventSubmissionMessage(
        data.message ||
          'Event submitted successfully. Pending administrator approval.'
      )
      setEventSubmissionError(false)
      setEventForm({ ...emptyEventForm })
      setOrganizerRefreshVersion((version) => version + 1)
      setEventFormErrors({})
    } catch {
      setEventSubmissionMessage(
        'Unable to connect to the event submission service.'
      )
      setEventSubmissionError(true)
    } finally {
      setIsSubmittingEvent(false)
    }
  }
  // Search and filter
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
      !selectedDate || event.date === selectedDate
    return matchesSearch && matchesCategory && matchesDate
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
        !['ACTIVE', 'APPROVED'].includes(selectedEvent.status) ||
        selectedEvent.date < localDateString()
      )
  )
  // Event card
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
          {isDetailsLoading ? 'Loading...' : 'View Details →'}
        </button>
      </article>
    )
  }
  // Registered events section
  function renderRegisteredSection(compact = false) {
    const displayedEvents = compact
      ? registeredEvents.slice(0, 3)
      : registeredEvents
    return (
      <section className="portal-section">
        <div className="portal-section-heading">
          <div>
            <h2>
              {compact ? 'My Registered Events' : 'My RSVPs'}
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
          <p className="portal-alert portal-error" role="alert">
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
  // Explore events section
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
            <p>Find something great happening at LMU.</p>
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
          <p className="portal-alert portal-error" role="alert">
            {eventsError}
          </p>
        )}
        {message && isError && (
          <p className="portal-alert portal-error" role="alert">
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
  // SCRUM-22 - Organizer event submission form
  function renderOrganizerEventForm() {
    const fieldStyle = {
      width: '100%',
      boxSizing: 'border-box',
    }
    const errorFor = (field) =>
      eventFormErrors[field] ? (
        <p className="portal-alert portal-error" role="alert">
          {eventFormErrors[field]}
        </p>
      ) : null
    return (
      <section className="portal-panel">
        <h2>Submit a New Event</h2>
        <p className="portal-muted">
          Share your upcoming campus event with the LMU
          community. Your submission will be reviewed by
          an administrator before it appears publicly.
        </p>
        <form
          onSubmit={handleEventSubmission}
          noValidate
          style={{
            display: 'grid',
            gap: '20px',
            marginTop: '24px',
          }}
        >
          <div>
            <label htmlFor="event-title">
              Event Title *
            </label>
            <input
              id="event-title"
              type="text"
              value={eventForm.title}
              onChange={(event) =>
                updateEventForm('title', event.target.value)
              }
              placeholder="e.g. LMU AI and Cybersecurity Workshop"
              maxLength={150}
              aria-invalid={Boolean(eventFormErrors.title)}
              style={fieldStyle}
            />
            {errorFor('title')}
          </div>
          <div>
            <label htmlFor="event-description">
              Event Description *
            </label>
            <textarea
              id="event-description"
              value={eventForm.description}
              onChange={(event) =>
                updateEventForm(
                  'description',
                  event.target.value
                )
              }
              placeholder="Describe the event and what attendees can expect..."
              rows={5}
              maxLength={2000}
              aria-invalid={Boolean(eventFormErrors.description)}
              style={{
                ...fieldStyle,
                padding: '12px',
                borderRadius: '8px',
              }}
            />
            {errorFor('description')}
          </div>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns:
                'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '18px',
            }}
          >
            <div>
              <label htmlFor="event-date">
                Event Date *
              </label>
              <input
                id="event-date"
                type="date"
                min={localDateString()}
                value={eventForm.date}
                onChange={(event) =>
                  updateEventForm('date', event.target.value)
                }
                aria-invalid={Boolean(eventFormErrors.date)}
                style={fieldStyle}
              />
              {errorFor('date')}
            </div>
            <div>
              <label htmlFor="event-time">
                Event Time *
              </label>
              <input
                id="event-time"
                type="time"
                value={eventForm.time}
                onChange={(event) =>
                  updateEventForm('time', event.target.value)
                }
                aria-invalid={Boolean(eventFormErrors.time)}
                style={fieldStyle}
              />
              {errorFor('time')}
            </div>
          </div>
          <div>
            <label htmlFor="event-location">
              Event Location *
            </label>
            <input
              id="event-location"
              type="text"
              value={eventForm.location}
              onChange={(event) =>
                updateEventForm(
                  'location',
                  event.target.value
                )
              }
              placeholder="e.g. University Hall, LMU"
              maxLength={200}
              aria-invalid={Boolean(eventFormErrors.location)}
              style={fieldStyle}
            />
            {errorFor('location')}
          </div>
          <div>
            <label htmlFor="event-category">
              Event Category *
            </label>
            <select
              id="event-category"
              value={eventForm.category}
              onChange={(event) =>
                updateEventForm(
                  'category',
                  event.target.value
                )
              }
              aria-invalid={Boolean(eventFormErrors.category)}
              style={fieldStyle}
            >
              <option value="">Select a category</option>
              {eventCategories.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
            {errorFor('category')}
          </div>
          <div
            className="portal-panel"
            style={{
              background: 'rgba(116, 20, 38, 0.05)',
              padding: '16px',
            }}
          >
            <strong>Approval Required</strong>
            <p className="portal-muted">
              Your event will receive Pending Approval status.
              It will not appear in public event discovery until
              an administrator approves it.
            </p>
          </div>
          <button
            type="submit"
            className="portal-btn portal-btn-wide"
            disabled={isSubmittingEvent}
          >
            {isSubmittingEvent
              ? 'Submitting Event...'
              : 'Submit Event for Approval →'}
          </button>
          {eventSubmissionMessage && (
            <p
              className={`portal-alert ${
                eventSubmissionError
                  ? 'portal-error'
                  : 'portal-success'
              }`}
              role="status"
            >
              {eventSubmissionMessage}
            </p>
          )}
        </form>
      </section>
    )
  }
  // SCRUM-24 - Persist organizer notification read status in Redis.
  async function handleMarkNotificationRead(notificationId = null) {
    if (!isOrganizer || notificationBusyId !== null || organizerDataLoading) return

    setNotificationBusyId(notificationId || 'all')
    setNotificationReadError('')

    try {
      const path = notificationId
        ? `/api/organizer/notifications/${encodeURIComponent(notificationId)}/read`
        : '/api/organizer/notifications/read-all'

      const response = await fetch(`${API_URL}${path}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      })

      if (response.status === 401) {
        handleSessionExpired()
        return
      }

      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.message || 'Unable to mark notifications as read.')
      }

      setOrganizerNotifications((previous) => previous.map((item) =>
        (!notificationId || item.id === notificationId)
          ? { ...item, read: true }
          : item
      ))
    } catch (error) {
      setNotificationReadError(error.message || 'Unable to update notification.')
    } finally {
      setNotificationBusyId(null)
    }
  }

  // SCRUM-24 - Organizer approval statuses and in-app notifications.
  function renderOrganizerUpdates() {
    const unreadCount = organizerNotifications.filter((item) => !item.read).length
    const statusLabel = (status) => ({
      PENDING_APPROVAL: 'Pending Approval',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
    }[status] || status)
    const statusColor = (status) => ({
      PENDING_APPROVAL: '#a66a09',
      APPROVED: '#18794e',
      REJECTED: '#b42318',
    }[status] || '#555')

    return (
      <>
        <section className="portal-section">
          <div className="portal-section-heading">
            <div>
              <h2>Notifications ({unreadCount} unread)</h2>
              <p>Updates about administrator decisions on your events.</p>
            </div>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <button
                type="button"
                className="portal-btn portal-btn-outline"
                onClick={() => handleMarkNotificationRead()}
                disabled={unreadCount === 0 || notificationBusyId !== null || organizerDataLoading}
              >
                {notificationBusyId === 'all' ? 'Marking...' : 'Mark All as Read'}
              </button>
              <button type="button" className="portal-btn portal-btn-outline"
                onClick={() => setOrganizerRefreshVersion((value) => value + 1)}
                disabled={organizerDataLoading || notificationBusyId !== null}>
                {organizerDataLoading ? 'Refreshing...' : 'Refresh Updates'}
              </button>
            </div>
          </div>
          {notificationReadError && (
            <p className="portal-alert portal-error" role="alert">{notificationReadError}</p>
          )}
          {organizerDataError && (
            <p className="portal-alert portal-error" role="alert">{organizerDataError}</p>
          )}
          {organizerDataLoading && <p className="portal-muted">Loading your updates...</p>}
          {!organizerDataLoading && !organizerDataError && organizerNotifications.length === 0 && (
            <div className="portal-empty">
              <h3>No notifications yet</h3>
              <p>You'll see a message here when an administrator reviews your event.</p>
            </div>
          )}
          {!organizerDataError && organizerNotifications.map((notification) => (
            <article className="portal-panel" key={notification.id} style={{ marginBottom: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
                <strong>{notification.eventTitle}</strong>
                <span style={{ color: statusColor(notification.decision), fontWeight: 700 }}>
                  {statusLabel(notification.decision)}
                </span>
              </div>
              <p className="portal-muted">{notification.message}</p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                <small style={{ fontWeight: 700 }}>
                  {notification.read ? 'Read' : 'Unread'}
                </small>
                {!notification.read && (
                  <button
                    type="button"
                    className="portal-btn portal-btn-outline"
                    onClick={() => handleMarkNotificationRead(notification.id)}
                    disabled={notificationBusyId !== null || organizerDataLoading}
                  >
                    {notificationBusyId === notification.id ? 'Marking...' : 'Mark as Read'}
                  </button>
                )}
              </div>
            </article>
          ))}
        </section>
        <section className="portal-section">
          <div className="portal-section-heading">
            <div>
              <h2>My Submitted Events ({organizerEvents.length})</h2>
              <p>Track each event's current approval status.</p>
            </div>
          </div>
          {!organizerDataLoading && !organizerDataError && organizerEvents.length === 0 && (
            <div className="portal-empty">
              <h3>No submitted events yet</h3>
              <p>Submit an event using the form above to see it here.</p>
            </div>
          )}
          {!organizerDataError && organizerEvents.length > 0 && (
            <div className="portal-event-grid">
              {organizerEvents.map((event) => (
                <article className="portal-event-card" key={event.id}>
                  <div className="portal-event-card-top">
                    <span className="portal-category">{event.category}</span>
                    <span style={{ color: statusColor(event.status), fontWeight: 700 }}>
                      {statusLabel(event.status)}
                    </span>
                  </div>
                  <h3>{event.title}</h3>
                  <p className="portal-event-description">{event.description}</p>
                  <div className="portal-event-meta">
                    <span>▦ &nbsp;{event.date}</span>
                    <span>◷ &nbsp;{event.time}</span>
                    <span>⌖ &nbsp;{event.location}</span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </>
    )
  }

  // Login screen
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
              : 'Sign in with your LMU email to discover events and manage your campus activities.'}
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
                {isLoading ? 'Verifying...' : 'Verify OTP →'}
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
                isError ? 'portal-error' : 'portal-success'
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
  // Authenticated application
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
      {/* Sidebar */}
      <aside
        className={`portal-sidebar ${
          isSidebarOpen ? 'portal-sidebar-open' : ''
        }`}
      >
        <div className="portal-sidebar-brand">
          <span className="portal-logo">L</span>
          <div>
            <strong>LMU Unified</strong>
            <small>
              {isAdmin
                ? 'ADMIN PORTAL'
                : isOrganizer
                  ? 'ORGANIZER PORTAL'
                  : 'STUDENT PORTAL'}
            </small>
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
          aria-label="Portal navigation"
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
              <strong>
                {isAdmin
                  ? 'Administrator Account'
                  : isOrganizer
                    ? 'Organizer Account'
                    : 'Student Account'}
              </strong>
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
      {/* Main application */}
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
          {userRole === null ? (
            <section className="portal-panel">
              <h2>Checking account access</h2>
              <p className="portal-muted">
                {roleLoading
                  ? 'Loading your account role...'
                  : roleError || 'Please wait...'}
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
                  {[
                    ['Date', selectedEvent.date],
                    ['Time', selectedEvent.time],
                    ['Location', selectedEvent.location],
                    ['Category', selectedEvent.category],
                    [
                      'Organizer',
                      selectedEvent.organizer || 'LMU',
                    ],
                  ].map(([label, value]) => (
                    <div
                      className="portal-info-row"
                      key={label}
                    >
                      <span>{label}</span>
                      <strong>{value}</strong>
                    </div>
                  ))}
                </section>
                {isAdmin ? (
                  <section className="portal-panel">
                    <h2>Event Registrations</h2>
                    <div className="portal-info-row">
                      <span>Registered attendees</span>
                      <strong>
                        {adminDashboard?.events?.find(
                          (event) =>
                            String(event.id) ===
                            String(selectedEvent.id)
                        )?.rsvpCount ?? 0}
                      </strong>
                    </div>
                    <p className="portal-muted">
                      Total students registered for this event.
                    </p>
                  </section>
                ) : isStudent ? (
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
                ) : (
                  <section className="portal-panel">
                    <h2>Organizer View</h2>
                    <p className="portal-muted">
                      You can view approved campus events here.
                      Submit new events from your Organizer Dashboard.
                    </p>
                  </section>
                )}
              </div>
            </>
          ) : isAdmin && activePage === 'dashboard' ? (
            <>
              <div className="portal-welcome">
                <div>
                  <p className="portal-eyebrow">
                    ADMINISTRATOR WORKSPACE
                  </p>
                  <h1>
                    Admin Dashboard
                    <span className="portal-welcome-dot">
                      .
                    </span>
                  </h1>
                  <p>
                    Manage your overview of campus events
                    and registrations.
                  </p>
                </div>
                <div
                  className="portal-welcome-decoration"
                  aria-hidden="true"
                >
                  ✦
                </div>
              </div>
              {isAdminDashboardLoading && (
                <p className="portal-muted">
                  Loading administrator dashboard...
                </p>
              )}
              {adminDashboardError && (
                <p
                  className="portal-alert portal-error"
                  role="alert"
                >
                  {adminDashboardError}
                </p>
              )}
              <div className="portal-stats">
                <div className="portal-stat">
                  <span className="portal-stat-icon">
                    ▦
                  </span>
                  <div>
                    <strong>
                      {adminDashboard?.totalEvents ??
                        events.length}
                    </strong>
                    <span>Upcoming Events</span>
                  </div>
                </div>
                <div className="portal-stat">
                  <span className="portal-stat-icon">
                    ✓
                  </span>
                  <div>
                    <strong>
                      {adminDashboard?.totalRsvps ?? 0}
                    </strong>
                    <span>Total RSVPs</span>
                  </div>
                </div>
                <div className="portal-stat">
                  <span className="portal-stat-icon">
                    ◎
                  </span>
                  <div>
                    <strong>ADMIN</strong>
                    <span>Account Role</span>
                  </div>
                </div>
              </div>
              {/* SCRUM-23 - Administrator approval queue */}
              <section className="portal-section">
                <div className="portal-section-heading">
                  <div>
                    <h2>Events Awaiting Approval ({pendingEvents.length})</h2>
                    <p>Review organizer submissions before publishing them to students.</p>
                  </div>
                </div>
                {reviewMessage && (
                  <p className={`portal-alert ${reviewMessageError ? 'portal-error' : 'portal-success'}`} role="status">
                    {reviewMessage}
                  </p>
                )}
                {pendingError && <p className="portal-alert portal-error" role="alert">{pendingError}</p>}
                {pendingLoading ? (
                  <p className="portal-muted">Loading pending events...</p>
                ) : pendingEvents.length === 0 && !pendingError ? (
                  <div className="portal-empty"><h3>No pending events</h3><p>New organizer submissions will appear here.</p></div>
                ) : (
                  <div className="portal-event-grid">
                    {pendingEvents.map((event) => (
                      <article className="portal-event-card" key={event.id}>
                        <div className="portal-event-card-top">
                          <span className="portal-category">{event.category}</span>
                          <span className="portal-confirmed">Pending Approval</span>
                        </div>
                        <h3>{event.title}</h3>
                        <p className="portal-event-description">{event.description}</p>
                        <div className="portal-event-meta">
                          <span>▦ &nbsp;{event.date}</span>
                          <span>◷ &nbsp;{event.time}</span>
                          <span>⌖ &nbsp;{event.location}</span>
                          <span>Organizer: {event.organizer}</span>
                        </div>
                        <button type="button" className="portal-btn portal-btn-outline"
                          onClick={() => setReviewEvent(event)}>
                          Review Event →
                        </button>
                      </article>
                    ))}
                  </div>
                )}
                {reviewEvent && (
                  <section className="portal-panel" style={{ marginTop: '20px' }} aria-label="Pending event review">
                    <h2>Review: {reviewEvent.title}</h2>
                    <p>{reviewEvent.description}</p>
                    {[
                      ['Date', reviewEvent.date],
                      ['Time', reviewEvent.time],
                      ['Location', reviewEvent.location],
                      ['Category', reviewEvent.category],
                      ['Organizer', reviewEvent.organizer],
                      ['Status', reviewEvent.status],
                    ].map(([label, value]) => (
                      <div className="portal-info-row" key={label}>
                        <span>{label}</span><strong>{value}</strong>
                      </div>
                    ))}
                    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginTop: '20px' }}>
                      <button type="button" className="portal-btn"
                        disabled={reviewBusyId !== null}
                        onClick={() => handleReviewDecision(reviewEvent.id, 'approve')}>
                        {reviewBusyId === reviewEvent.id ? 'Processing...' : 'Approve Event'}
                      </button>
                      <button type="button" className="portal-btn portal-btn-danger"
                        disabled={reviewBusyId !== null}
                        onClick={() => handleReviewDecision(reviewEvent.id, 'reject')}>
                        Reject Event
                      </button>
                      <button type="button" className="portal-btn portal-btn-outline"
                        disabled={reviewBusyId !== null}
                        onClick={() => setReviewEvent(null)}>Cancel</button>
                    </div>
                  </section>
                )}
              </section>

              <section className="portal-section">
                <div className="portal-section-heading">
                  <div>
                    <h2>Campus Events</h2>
                    <p>
                      Events currently available on LMU Unified.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="portal-text-btn"
                    onClick={() => navigate('explore')}
                  >
                    View all →
                  </button>
                </div>
                <div className="portal-event-grid">
                  {(Array.isArray(adminDashboard?.events) &&
                  adminDashboard.events.length
                    ? adminDashboard.events.map(
                        (entry) => entry.event || entry
                      )
                    : events
                  )
                    .slice(0, 3)
                    .map((event) =>
                      renderEventCard(event)
                    )}
                </div>
              </section>
            </>
          ) : isOrganizer && activePage === 'dashboard' ? (
            <>
              <div className="portal-welcome">
                <div>
                  <p className="portal-eyebrow">
                    ORGANIZER WORKSPACE
                  </p>
                  <h1>
                    Organizer Dashboard
                    <span className="portal-welcome-dot">
                      .
                    </span>
                  </h1>
                  <p>
                    Create campus events and submit them
                    for administrator approval.
                  </p>
                </div>
                <div
                  className="portal-welcome-decoration"
                  aria-hidden="true"
                >
                  ✦
                </div>
              </div>
              {renderOrganizerEventForm()}
              {renderOrganizerUpdates()}
              {renderExploreSection(true)}
            </>
          ) : activePage === 'dashboard' ? (
            <>
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
                      {studentDashboard?.role || 'STUDENT'}
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
          ) : isStudent && activePage === 'rsvps' ? (
            <>
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
              <div className="portal-page-heading">
                <p className="portal-eyebrow">
                  ACCOUNT
                </p>
                <h1>My Profile</h1>
                <p>Your LMU Unified account.</p>
              </div>
              <section className="portal-panel portal-profile">
                <span className="portal-avatar portal-profile-avatar">
                  {(email || 'S')[0].toUpperCase()}
                </span>
                <h2>
                  {isAdmin
                    ? 'Administrator Account'
                    : isOrganizer
                      ? 'Organizer Account'
                      : 'Student Account'}
                </h2>
                <div className="portal-info-row">
                  <span>Email</span>
                  <strong>
                    {(isAdmin
                      ? adminDashboard?.email
                      : isStudent
                        ? studentDashboard?.email
                        : email) || email}
                  </strong>
                </div>
                <div className="portal-info-row">
                  <span>Role</span>
                  <strong>{userRole}</strong>
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
