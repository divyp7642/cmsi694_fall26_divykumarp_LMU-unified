
import { useEffect, useState } from "react";

const API_BASE_URL = "http://localhost:8080";

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

  const [loadingRooms, setLoadingRooms] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [reservation, setReservation] = useState(null);

  // Only approved events can reserve rooms.
  const approvedEvents = events.filter(
    (event) => event.status === "APPROVED"
  );

  // Get today's date using the local timezone.
  const today = new Date();
  const minimumDate =
    `${today.getFullYear()}-` +
    `${String(today.getMonth() + 1).padStart(2, "0")}-` +
    `${String(today.getDate()).padStart(2, "0")}`;

  // Retrieve the campus room catalog.
  useEffect(() => {
    const controller = new AbortController();

    async function loadRooms() {
      setLoadingRooms(true);
      setError("");
      setRooms([]);

      try {
        const response = await fetch(
          `${API_BASE_URL}/api/organizer/rooms`,
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
            data.message || "Unable to retrieve campus rooms."
          );
        }

        setRooms(data.rooms || []);
      } catch (err) {
        if (err.name !== "AbortError") {
          setError(
            err.message || "Unable to connect to the server."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoadingRooms(false);
        }
      }
    }

    if (token) {
      loadRooms();
    } else {
      setLoadingRooms(false);
      setRooms([]);
      setError("Please log in first.");
    }

    return () => controller.abort();
  }, [token]);

  const selectedRoom = rooms.find(
    (room) => String(room.id) === roomId
  );

  // Clear old messages when the organizer changes the form.
  function clearMessages() {
    setError("");
    setSuccess("");
    setReservation(null);
  }

  function handleEventChange(event) {
    const selectedId = event.target.value;

    setEventId(selectedId);
    clearMessages();

    const selectedEvent = approvedEvents.find(
      (item) => String(item.id) === selectedId
    );

    if (selectedEvent?.date) {
      setDate(selectedEvent.date);
    } else {
      setDate("");
    }
  }

  // Submit the reservation request.
  async function handleSubmit(event) {
    event.preventDefault();

    clearMessages();

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

    if (!selectedRoom) {
      setError("Please select a valid campus room.");
      return;
    }

    if (date < minimumDate) {
      setError("Reservation date cannot be in the past.");
      return;
    }

    if (startTime >= endTime) {
      setError("End time must be after start time.");
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

        return;
      }

      if (!response.ok) {
        throw new Error(
          data.message || "Unable to complete reservation."
        );
      }

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
            Select an approved event, campus room,
            and reservation time.
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
              <label htmlFor="reservation-room">
                Campus Room
              </label>

              <select
                id="reservation-room"
                value={roomId}
                onChange={(event) => {
                  setRoomId(event.target.value);
                  clearMessages();
                }}
                disabled={loadingRooms || submitting}
                required
              >
                <option value="">
                  {loadingRooms
                    ? "Loading campus rooms..."
                    : "Select a campus room"}
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
                rooms.length === 0 ||
                Boolean(reservation)
              }
            >
              {submitting
                ? "Checking Availability..."
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
