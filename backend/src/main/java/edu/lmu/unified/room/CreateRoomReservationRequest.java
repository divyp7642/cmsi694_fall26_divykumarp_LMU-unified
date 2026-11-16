package edu.lmu.unified.room;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateRoomReservationRequest(

        @NotNull(message = "Room ID is required")
        @Positive(message = "Room ID must be positive")
        Long roomId,

        @NotNull(message = "Event ID is required")
        @Positive(message = "Event ID must be positive")
        Long eventId,

        @NotNull(message = "Reservation date is required")
        @FutureOrPresent(
                message = "Reservation date cannot be in the past"
        )
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime

) {
}