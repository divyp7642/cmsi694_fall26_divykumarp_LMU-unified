package edu.lmu.unified.room;

import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class RoomReservationService {

    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final RoomConflictService conflictService;
    private final EventRepository eventRepository;

    public RoomReservationService(
            RoomRepository roomRepository,
            RoomReservationRepository reservationRepository,
            RoomConflictService conflictService,
            EventRepository eventRepository
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.conflictService = conflictService;
        this.eventRepository = eventRepository;
    }

    // Create a confirmed room reservation.
    public RoomReservation reserveRoom(
            CreateRoomReservationRequest request,
            String organizerEmail
    ) {

        // 1. Validate organizer identity.
        if (organizerEmail == null
                || organizerEmail.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first."
            );
        }

        // 2. Verify that the event exists.
        Event event = eventRepository
                .findById(request.eventId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Event not found."
                        )
                );

        // 3. Verify event ownership.
        if (!organizerEmail.equalsIgnoreCase(
                event.organizer()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only reserve rooms for your own events."
            );
        }

        // 4. Verify that the event is approved.
        if (!"APPROVED".equalsIgnoreCase(
                event.status()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only approved events can reserve rooms."
            );
        }

        // 5. Verify that the selected room exists.
        if (!roomRepository.existsById(
                request.roomId()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Selected room not found."
            );
        }

        // 6. Validate the reservation date.
        LocalDate today = LocalDate.now(
                ZoneId.of("America/Los_Angeles")
        );

        if (request.date() == null
                || request.date().isBefore(today)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation date cannot be in the past."
            );
        }

        // 7. Validate time and check availability.
        boolean available;

        try {

            available = conflictService.isRoomAvailable(
                    request.roomId(),
                    request.date(),
                    request.startTime(),
                    request.endTime()
            );

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }

        if (!available) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The selected room is unavailable during "
                            + "this time. Please select another "
                            + "room or time."
            );
        }

        // 8. Generate a unique reservation ID.
        Long reservationId =
                reservationRepository.generateReservationId();

        // 9. Create the confirmed reservation.
        RoomReservation reservation =
                new RoomReservation(
                        reservationId,
                        request.roomId(),
                        request.eventId(),
                        organizerEmail.trim().toLowerCase(
                                java.util.Locale.ROOT
                        ),
                        request.date(),
                        request.startTime(),
                        request.endTime(),
                        ReservationStatus.CONFIRMED
                );

        // 10. Atomically check and save in Redis.
        boolean saved =
                reservationRepository.saveIfAvailable(
                        reservation
                );

        if (!saved) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The selected room is no longer available. "
                            + "Please select another room or time."
            );
        }

        return reservation;
    }
}