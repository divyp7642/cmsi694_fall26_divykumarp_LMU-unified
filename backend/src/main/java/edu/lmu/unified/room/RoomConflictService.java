package edu.lmu.unified.room;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RoomConflictService {

    private final RoomReservationRepository reservationRepository;

    public RoomConflictService(
            RoomReservationRepository reservationRepository
    ) {
        this.reservationRepository = reservationRepository;
    }

    // Check whether a requested time overlaps
    // with an existing confirmed reservation.
    public boolean hasConflict(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        validateRequest(roomId, date, startTime, endTime);

        List<RoomReservation> confirmedReservations =
                reservationRepository
                        .findConfirmedByRoomIdAndDate(
                                roomId,
                                date
                        );

        return confirmedReservations.stream()
                .anyMatch(reservation ->
                        startTime.isBefore(
                                reservation.endTime()
                        )
                        &&
                        endTime.isAfter(
                                reservation.startTime()
                        )
                );
    }

    // Check whether a room is available.
    public boolean isRoomAvailable(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        return !hasConflict(
                roomId,
                date,
                startTime,
                endTime
        );
    }

    // Validate the requested reservation information.
    private void validateRequest(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException(
                    "A valid room ID is required"
            );
        }

        if (date == null) {
            throw new IllegalArgumentException(
                    "Reservation date is required"
            );
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }
}