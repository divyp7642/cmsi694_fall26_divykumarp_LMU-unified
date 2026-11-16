package edu.lmu.unified.room;

import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomReservationServiceTest {

    private RoomRepository roomRepository;
    private RoomReservationRepository reservationRepository;
    private RoomConflictService conflictService;
    private EventRepository eventRepository;

    private RoomReservationService reservationService;

    private final String organizerEmail =
            "organizer@lion.lmu.edu";

    private LocalDate reservationDate;

    @BeforeEach
    void setUp() {

        roomRepository =
                mock(RoomRepository.class);

        reservationRepository =
                mock(RoomReservationRepository.class);

        conflictService =
                mock(RoomConflictService.class);

        eventRepository =
                mock(EventRepository.class);

        reservationService =
                new RoomReservationService(
                        roomRepository,
                        reservationRepository,
                        conflictService,
                        eventRepository
                );

        reservationDate = LocalDate.now(
                ZoneId.of("America/Los_Angeles")
        ).plusDays(5);
    }

    private CreateRoomReservationRequest createRequest() {

        return new CreateRoomReservationRequest(
                101L,
                5L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
    }

    private Event createEvent(
            String organizer,
            String status
    ) {

        return new Event(
                5L,
                "Technology Workshop",
                "A workshop for LMU students",
                reservationDate.toString(),
                "10:00 AM",
                "University Hall",
                "Technology",
                organizer,
                status
        );
    }

    private void mockApprovedEvent() {

        when(eventRepository.findById(5L))
                .thenReturn(
                        Optional.of(
                                createEvent(
                                        organizerEmail,
                                        "APPROVED"
                                )
                        )
                );
    }

    private void mockAvailableRoom() {

        when(roomRepository.existsById(101L))
                .thenReturn(true);

        when(conflictService.isRoomAvailable(
                101L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        )).thenReturn(true);
    }

    @Test
    void shouldReserveRoomSuccessfully() {

        mockApprovedEvent();
        mockAvailableRoom();

        when(reservationRepository
                .generateReservationId())
                .thenReturn(1L);

        when(reservationRepository
                .saveIfAvailable(
                        any(RoomReservation.class)
                ))
                .thenReturn(true);

        RoomReservation reservation =
                reservationService.reserveRoom(
                        createRequest(),
                        organizerEmail
                );

        assertNotNull(reservation);
        assertEquals(1L, reservation.id());
        assertEquals(101L, reservation.roomId());
        assertEquals(5L, reservation.eventId());
        assertEquals(
                organizerEmail,
                reservation.organizerEmail()
        );
        assertEquals(
                reservationDate,
                reservation.date()
        );
        assertEquals(
                LocalTime.of(10, 0),
                reservation.startTime()
        );
        assertEquals(
                LocalTime.of(12, 0),
                reservation.endTime()
        );
        assertEquals(
                ReservationStatus.CONFIRMED,
                reservation.status()
        );

        verify(reservationRepository)
                .saveIfAvailable(reservation);
    }

    @Test
    void shouldRejectMissingOrganizerEmail() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                null
                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectEventThatDoesNotExist() {

        when(eventRepository.findById(5L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectEventOwnedByAnotherOrganizer() {

        when(eventRepository.findById(5L))
                .thenReturn(
                        Optional.of(
                                createEvent(
                                        "another@lion.lmu.edu",
                                        "APPROVED"
                                )
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectPendingEvent() {

        when(eventRepository.findById(5L))
                .thenReturn(
                        Optional.of(
                                createEvent(
                                        organizerEmail,
                                        "PENDING_APPROVAL"
                                )
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectRejectedEvent() {

        when(eventRepository.findById(5L))
                .thenReturn(
                        Optional.of(
                                createEvent(
                                        organizerEmail,
                                        "REJECTED"
                                )
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectRoomThatDoesNotExist() {

        mockApprovedEvent();

        when(roomRepository.existsById(101L))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectReservationInThePast() {

        mockApprovedEvent();

        when(roomRepository.existsById(101L))
                .thenReturn(true);

        CreateRoomReservationRequest request =
                new CreateRoomReservationRequest(
                        101L,
                        5L,
                        LocalDate.now(
                                ZoneId.of("America/Los_Angeles")
                        ).minusDays(1),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                request,
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectInvalidTimeRange() {

        mockApprovedEvent();

        when(roomRepository.existsById(101L))
                .thenReturn(true);

        CreateRoomReservationRequest request =
                new CreateRoomReservationRequest(
                        101L,
                        5L,
                        reservationDate,
                        LocalTime.of(12, 0),
                        LocalTime.of(10, 0)
                );

        when(conflictService.isRoomAvailable(
                101L,
                reservationDate,
                LocalTime.of(12, 0),
                LocalTime.of(10, 0)
        )).thenThrow(
                new IllegalArgumentException(
                        "End time must be after start time"
                )
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                request,
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectConflictingReservation() {

        mockApprovedEvent();

        when(roomRepository.existsById(101L))
                .thenReturn(true);

        when(conflictService.isRoomAvailable(
                101L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        )).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertTrue(
                exception.getReason()
                        .contains("unavailable")
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectConcurrentBookingConflict() {

        mockApprovedEvent();
        mockAvailableRoom();

        when(reservationRepository
                .generateReservationId())
                .thenReturn(1L);

        when(reservationRepository
                .saveIfAvailable(
                        any(RoomReservation.class)
                ))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reservationService.reserveRoom(
                                createRequest(),
                                organizerEmail
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertTrue(
                exception.getReason()
                        .contains("no longer available")
        );

        verify(reservationRepository)
                .saveIfAvailable(
                        any(RoomReservation.class)
                );
    }
}