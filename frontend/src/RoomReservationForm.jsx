
import { useEffect, useRef, useState } from "react";

const API_BASE_URL = "http://localhost:8080";

function getMinimumDate() {
  const today = new Date();

  return (
    `${today.getFullYear()}-` +
    `${String(today.getMonth() + 1).padStart(2, "0")}-` +
    `${String(today.getDate()).padStart(2, "0")}`
  );
}

function RoomReservationForm({
  token,
  events = [],
  onReservationSuccess,
}) {
  const [rooms, setRooms] = useState([]);

  const [eventId, setEventId] = useState("");
  const [roomId, setRoomId] = useState("");
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");

  const [loadingRooms, setLoadingRooms] = useState(false);
  const [availabilityChecked, setAvailabilityChecked] =
    useState(false);

  const [refreshKey, setRefreshKey] = useState(0);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [reservation, setReservation] = useState(null);

  // Prevent outdated availability responses from
  // overwriting the latest selection.
  const availabilityRequestRef = useRef(0);

  // Track whether a successful reservation needs
  // an availability refresh after the user changes inputs.
  const reservationRef = useRef(null);

  const approvedEvents = events.filter(
    (event) => event.status === "APPROVED"
  );

  const minimumDate = getMinimumDate();

  const selectedRoom = rooms.find(
    (room) => String(room.id) === roomId
  );

  const validTimeRange =
    Boolean(date && startTime && endTime) &&
    date >= minimumDate &&
    startTime < endTime;

  function clearMessages() {
    setError("");
    setSuccess("");
    setReservation(null);
    reservationRef.current = null;
  }

  function handleEventChange(event) {
    const selectedId = event.target.value;

    setEventId(selectedId);
    clearMessages();

    const selectedEvent = approvedEvents.find(
      (item) => String(item.id) === selectedId
    );

    setDate(selectedEvent?.date || "");
  }

  // Automatically retrieve available rooms whenever
  // the selected date/time changes or a refresh is requested.
  useEffect(() => {
    const controller = new AbortController();

    const requestId =
      ++availabilityRequestRef.current;

    async function loadAvailableRooms() {
      setLoadingRooms(true);
      setAvailabilityChecked(false);
      setRooms([]);
      setRoomId("");

      try {
        const params = new URLSearchParams({
          date,
          startTime,
          endTime,
        });

        const response = await fetch(
          `${API_BASE_URL}/api/organizer/rooms/available?${params}`,
          {
            method: "GET",
            headers: {
              Authorization: `Bearer ${token}`,
            },
            signal: controller.signal,
          }
        );

        const data = await response.json();

        if (!response.ok) {
          throw new Error(
            data.message ||
              "Unable to check room availability."
          );
        }

        if (
          !controller.signal.aborted &&
          requestId === availabilityRequestRef.current
        ) {
          setRooms(data.rooms || []);
          setAvailabilityChecked(true);
        }
      } catch (err) {
        if (
          !controller.signal.aborted &&
          requestId === availabilityRequestRef.current
        ) {
          setRooms([]);
          setAvailabilityChecked(false);

          setError(
            err.message ||
              "Unable to connect to the server."
          );
        }
      } finally {
        if (
          !controller.signal.aborted &&
          requestId === availabilityRequestRef.current
        ) {
          setLoadingRooms(false);
        }
      }
    }

    if (!token || !validTimeRange) {
      setRooms([]);
      setRoomId("");
      setAvailabilityChecked(false);
      setLoadingRooms(false);

      return () => controller.abort();
    }

    // Keep successful reservation confirmation visible
    // until the organizer changes the form.
    if (reservationRef.current) {
      setLoadingRooms(false);
      return () => controller.abort();
    }

    loadAvailableRooms();

    return () => controller.abort();
  }, [
    token,
    date,
    startTime,
    endTime,
    validTimeRange,
    refreshKey,
  ]);

  // Submit a room reservation.
  async function handleSubmit(event) {
    event.preventDefault();

    setError("");
    setSuccess("");
    setReservation(null);

    if (!token) {
      setError("Please log in first.");
      return;
    }

    if (
      !eventId ||
      !roomId ||
      !date ||
      !startTime ||
      !endTime
    ) {
      setError("Please complete all reservation fields.");
      return;
    }

    if (
      !approvedEvents.some(
        (item) => String(item.id) === eventId
      )
    ) {
      setError("Please select an approved event.");
      return;
    }

    if (date < getMinimumDate()) {
      setError("Reservation date cannot be in the past.");
      return;
    }

    if (startTime >= endTime) {
      setError("End time must be after start time.");
      return;
    }

    if (
      !availabilityChecked ||
      !selectedRoom ||
      loadingRooms
    ) {
      setError(
        "Please select an available campus room."
      );
      return;
    }

    setSubmitting(true);

    const requestBody = {
      roomId: Number(roomId),
      eventId: Number(eventId),
      date,
      startTime,
      endTime,
    };

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/organizer/room-reservations`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(requestBody),
        }
      );

      const data = await response.json();

      if (response.status === 409) {
        setError(
          data.message ||
            "The selected room is unavailable during this time. " +
              "Please choose another room or time."
        );

        // Clear the outdated room selection.
        setRooms([]);
        setRoomId("");
        setAvailabilityChecked(false);

        // Trigger another availability API request
        // without requiring the organizer to change
        // the date or time.
        setRefreshKey((previous) => previous + 1);

        return;
      }

      if (!response.ok) {
        throw new Error(
          data.message || "Unable to complete reservation."
        );
      }

      reservationRef.current = data.reservation;

      setSuccess(
        data.message || "Room reserved successfully."
      );

      setReservation(data.reservation);

      if (onReservationSuccess) {
        onReservationSuccess(data.reservation);
      }
    } catch (err) {
      setError(
        err.message || "Unable to connect to the server."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="portal-section">
      <div className="portal-section-heading">
        <div>
          <h2>Reserve a Campus Room</h2>

          <p>
            Select an approved event, choose a date and
            time, and view available campus rooms.
          </p>
        </div>
      </div>

      <div className="portal-panel">
        {approvedEvents.length === 0 ? (
          <div className="portal-empty">
            <div className="portal-empty-icon">▦</div>

            <h3>No approved events</h3>

            <p>
              You need an approved event before
              reserving a campus room.
            </p>
          </div>
        ) : (
          <form
            className="room-reservation-form"
            onSubmit={handleSubmit}
          >
            <div className="room-form-field">
              <label htmlFor="reservation-event">
                Approved Event
              </label>

              <select
                id="reservation-event"
                value={eventId}
                onChange={handleEventChange}
                disabled={submitting}
                required
              >
                <option value="">
                  Select an approved event
                </option>

                {approvedEvents.map((event) => (
                  <option
                    key={event.id}
                    value={event.id}
                  >
                    {event.title}
                  </option>
                ))}
              </select>
            </div>

            <div className="room-form-field">
              <label htmlFor="reservation-date">
                Reservation Date
              </label>

              <input
                id="reservation-date"
                type="date"
                value={date}
                min={minimumDate}
                onChange={(event) => {
                  setDate(event.target.value);
                  clearMessages();
                }}
                disabled={submitting}
                required
              />
            </div>

            <div className="room-time-grid">
              <div className="room-form-field">
                <label htmlFor="reservation-start">
                  Start Time
                </label>

                <input
                  id="reservation-start"
                  type="time"
                  value={startTime}
                  onChange={(event) => {
                    setStartTime(event.target.value);
                    clearMessages();
                  }}
                  disabled={submitting}
                  required
                />
              </div>

              <div className="room-form-field">
                <label htmlFor="reservation-end">
                  End Time
                </label>

                <input
                  id="reservation-end"
                  type="time"
                  value={endTime}
                  onChange={(event) => {
                    setEndTime(event.target.value);
                    clearMessages();
                  }}
                  disabled={submitting}
                  required
                />
              </div>
            </div>

            <div className="room-form-field">
              <label htmlFor="reservation-room">
                Available Campus Rooms
              </label>

              <select
                id="reservation-room"
                value={roomId}
                onChange={(event) => {
                  setRoomId(event.target.value);
                  clearMessages();
                }}
                disabled={
                  !validTimeRange ||
                  loadingRooms ||
                  !availabilityChecked ||
                  rooms.length === 0 ||
                  submitting ||
                  Boolean(reservation)
                }
                required
              >
                <option value="">
                  {loadingRooms
                    ? "Checking room availability..."
                    : !validTimeRange
                      ? "Select a valid date and time first"
                      : !availabilityChecked
                        ? "Availability not yet checked"
                        : rooms.length === 0
                          ? "No rooms available"
                          : "Select an available room"}
                </option>

                {rooms.map((room) => (
                  <option
                    key={room.id}
                    value={room.id}
                  >
                    {room.name} — {room.location}
                    {" "}({room.capacity} seats)
                  </option>
                ))}
              </select>
            </div>

            {validTimeRange &&
              availabilityChecked &&
              !loadingRooms && (
                <div
                  className="room-selection-details"
                  role="status"
                >
                  <strong>
                    {rooms.length === 0
                      ? "No rooms available"
                      : `${rooms.length} available room${
                          rooms.length === 1 ? "" : "s"
                        } found`}
                  </strong>

                  <span>
                    {rooms.length === 0
                      ? "Try another date or time range."
                      : "Choose a room from the list above."}
                  </span>
                </div>
              )}

            {selectedRoom && (
              <div className="room-selection-details">
                <strong>{selectedRoom.name}</strong>

                <span>
                  Location: {selectedRoom.location}
                </span>

                <span>
                  Seating capacity: {selectedRoom.capacity}
                </span>
              </div>
            )}

            {date && date < minimumDate && (
              <div
                className="portal-alert portal-error"
                role="alert"
              >
                Reservation date cannot be in the past.
              </div>
            )}

            {startTime &&
              endTime &&
              startTime >= endTime && (
                <div
                  className="portal-alert portal-error"
                  role="alert"
                >
                  End time must be after start time.
                </div>
              )}

            {error && (
              <div
                className="portal-alert portal-error"
                role="alert"
              >
                {error}
              </div>
            )}

            {success && (
              <div
                className="portal-alert portal-success"
                role="status"
              >
                {success}
              </div>
            )}

            {reservation && (
              <div className="room-selection-details">
                <strong>Reservation Confirmed</strong>

                <span>
                  Reservation ID: {reservation.id}
                </span>

                <span>
                  Date: {reservation.date}
                </span>

                <span>
                  Time: {reservation.startTime}
                  {" — "}
                  {reservation.endTime}
                </span>

                <span>
                  Status: {reservation.status}
                </span>
              </div>
            )}

            <button
              type="submit"
              className="portal-btn portal-btn-wide"
              disabled={
                submitting ||
                loadingRooms ||
                !availabilityChecked ||
                !roomId ||
                rooms.length === 0 ||
                Boolean(reservation)
              }
            >
              {submitting
                ? "Reserving Room..."
                : reservation
                  ? "Room Reserved"
                  : "Reserve Room"}
            </button>
          </form>
        )}
      </div>
    </section>
  );
}

export default RoomReservationForm;
