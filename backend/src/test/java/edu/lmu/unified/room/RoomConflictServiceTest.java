package edu.lmu.unified.room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomConflictServiceTest {

    private RoomReservationRepository reservationRepository;
    private RoomConflictService conflictService;

    private final LocalDate reservationDate =
            LocalDate.of(2026, 11, 20);

    @BeforeEach
    void setUp() {

        reservationRepository =
                mock(RoomReservationRepository.class);

        conflictService =
                new RoomConflictService(reservationRepository);
    }

    private RoomReservation createConfirmedReservation() {

        return new RoomReservation(
                1L,
                101L,
                5L,
                "organizer@lion.lmu.edu",
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                ReservationStatus.CONFIRMED
        );
    }

    private void mockExistingReservation() {

        when(reservationRepository
                .findConfirmedByRoomIdAndDate(
                        101L,
                        reservationDate
                ))
                .thenReturn(
                        List.of(createConfirmedReservation())
                );
    }

    @Test
    void shouldDetectOverlappingReservation() {

        mockExistingReservation();

        boolean conflict = conflictService.hasConflict(
                101L,
                reservationDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0)
        );

        assertTrue(conflict);
    }

    @Test
    void shouldDetectIdenticalTimeReservation() {

        mockExistingReservation();

        boolean conflict = conflictService.hasConflict(
                101L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        assertTrue(conflict);
    }

    @Test
    void shouldDetectReservationInsideExistingReservation() {

        mockExistingReservation();

        boolean conflict = conflictService.hasConflict(
                101L,
                reservationDate,
                LocalTime.of(10, 30),
                LocalTime.of(11, 0)
        );

        assertTrue(conflict);
    }

    @Test
    void shouldDetectReservationContainingExistingReservation() {

        mockExistingReservation();

        boolean conflict = conflictService.hasConflict(
                101L,
                reservationDate,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        assertTrue(conflict);
    }

    @Test
    void shouldAllowReservationAfterExistingReservation() {

        mockExistingReservation();

        boolean available = conflictService.isRoomAvailable(
                101L,
                reservationDate,
                LocalTime.of(12, 0),
                LocalTime.of(14, 0)
        );

        assertTrue(available);
    }

    @Test
    void shouldAllowReservationBeforeExistingReservation() {

        mockExistingReservation();

        boolean available = conflictService.isRoomAvailable(
                101L,
                reservationDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        assertTrue(available);
    }

    @Test
    void shouldAllowReservationInDifferentRoom() {

        mockExistingReservation();

        when(reservationRepository
                .findConfirmedByRoomIdAndDate(
                        102L,
                        reservationDate
                ))
                .thenReturn(List.of());

        boolean available = conflictService.isRoomAvailable(
                102L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        assertTrue(available);
    }

    @Test
    void shouldAllowReservationOnDifferentDate() {

        mockExistingReservation();

        LocalDate differentDate =
                reservationDate.plusDays(1);

        when(reservationRepository
                .findConfirmedByRoomIdAndDate(
                        101L,
                        differentDate
                ))
                .thenReturn(List.of());

        boolean available = conflictService.isRoomAvailable(
                101L,
                differentDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        assertTrue(available);
    }

    @Test
    void shouldAllowReservationWhenNoConfirmedReservationsExist() {

        when(reservationRepository
                .findConfirmedByRoomIdAndDate(
                        101L,
                        reservationDate
                ))
                .thenReturn(List.of());

        boolean available = conflictService.isRoomAvailable(
                101L,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        assertTrue(available);
    }

    @Test
    void shouldRejectEndTimeBeforeStartTime() {

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.hasConflict(
                        101L,
                        reservationDate,
                        LocalTime.of(12, 0),
                        LocalTime.of(10, 0)
                )
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectIdenticalStartAndEndTime() {

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.hasConflict(
                        101L,
                        reservationDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 0)
                )
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectInvalidRoomId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.hasConflict(
                        -1L,
                        reservationDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                )
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectMissingDate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.hasConflict(
                        101L,
                        null,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                )
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectMissingStartTime() {

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.hasConflict(
                        101L,
                        reservationDate,
                        null,
                        LocalTime.of(12, 0)
                )
        );

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectMissingEndTime() {

        assertThrows(
                IllegalArgumentException.class,
                () -> conflictService.hasConflict(
                        101L,
                        reservationDate,
                        LocalTime.of(10, 0),
                        null
                )
        );

        verifyNoInteractions(reservationRepository);
    }
}