package edu.lmu.unified.room;

import java.time.LocalDate;
import java.time.LocalTime;

public record RoomReservation(
        Long id,
        Long roomId,
        Long eventId,
        String organizerEmail,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status
) {
}