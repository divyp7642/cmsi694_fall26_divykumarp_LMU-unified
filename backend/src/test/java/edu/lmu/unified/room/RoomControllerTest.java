
package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoomControllerTest {

    private AuthorizationService authorizationService;
    private RoomRepository roomRepository;
    private RoomConflictService roomConflictService;
    private RoomController roomController;

    private static final String AUTHORIZATION =
            "Bearer test-token";

    private static final String ORGANIZER_EMAIL =
            "organizer@lion.lmu.edu";

    private static final String DATE =
            LocalDate.now().plusDays(7).toString();

    @BeforeEach
    void setUp() {
        authorizationService =
                mock(AuthorizationService.class);

        roomRepository =
                new RoomRepository();

        roomConflictService =
                mock(RoomConflictService.class);

        roomController =
                new RoomController(
                        roomRepository,
                        authorizationService,
                        roomConflictService
                );
    }

    private void authenticateOrganizer() {
        when(authorizationService.getAuthenticatedEmail(
                AUTHORIZATION
        )).thenReturn(Optional.of(ORGANIZER_EMAIL));

        when(authorizationService.hasRole(
                AUTHORIZATION,
                UserRole.ORGANIZER
        )).thenReturn(true);
    }

    private ResponseEntity<?> availableRooms(
            String date,
            String startTime,
            String endTime
    ) {
        return roomController.getAvailableRooms(
                AUTHORIZATION,
                date,
                startTime,
                endTime
        );
    }

    private Map<?, ?> responseBody(
            ResponseEntity<?> response
    ) {
        assertInstanceOf(Map.class, response.getBody());
        return (Map<?, ?>) response.getBody();
    }

    // Existing room catalog tests

    @Test
    void shouldReturnAllRoomsForOrganizer() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                roomController.getRooms(AUTHORIZATION);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        Map<?, ?> body = responseBody(response);

        assertEquals(
                "Campus rooms retrieved successfully.",
                body.get("message")
        );

        assertEquals(5, body.get("totalRooms"));

        assertEquals(
                roomRepository.findAll(),
                body.get("rooms")
        );
    }

    @Test
    void shouldReturnCorrectRoomDetails() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                roomController.getRooms(AUTHORIZATION);

        Map<?, ?> body = responseBody(response);

        List<?> rooms =
                (List<?>) body.get("rooms");

        assertNotNull(rooms);
        assertEquals(5, rooms.size());

        Room expectedRoom = new Room(
                101L,
                "Conference Room A",
                "University Hall",
                50
        );

        assertEquals(expectedRoom, rooms.get(0));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        when(authorizationService.getAuthenticatedEmail(
                AUTHORIZATION
        )).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                roomController.getRooms(AUTHORIZATION);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        Map<?, ?> body = responseBody(response);

        assertEquals(
                "Please log in first.",
                body.get("message")
        );

        verify(
                authorizationService,
                never()
        ).hasRole(
                anyString(),
                any(UserRole.class)
        );
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {
        when(authorizationService.getAuthenticatedEmail(
                null
        )).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                roomController.getRooms(null);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        Map<?, ?> body = responseBody(response);

        assertEquals(
                "Please log in first.",
                body.get("message")
        );
    }

    @Test
    void shouldRejectNonOrganizerUser() {
        when(authorizationService.getAuthenticatedEmail(
                AUTHORIZATION
        )).thenReturn(
                Optional.of("student@lion.lmu.edu")
        );

        when(authorizationService.hasRole(
                AUTHORIZATION,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                roomController.getRooms(AUTHORIZATION);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        Map<?, ?> body = responseBody(response);

        assertEquals(
                "Access denied. Organizer role required.",
                body.get("message")
        );
    }

    // SCRUM-25 availability tests

    @Test
    void shouldReturnAllRoomsWhenAvailable() {
        authenticateOrganizer();

        when(roomConflictService.isRoomAvailable(
                anyLong(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)
        )).thenReturn(true);

        ResponseEntity<?> response =
                availableRooms(DATE, "11:00", "13:00");

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        Map<?, ?> body = responseBody(response);

        assertEquals(5, body.get("totalRooms"));
        assertEquals(
                roomRepository.findAll(),
                body.get("rooms")
        );

        assertEquals(DATE, body.get("date"));
        assertEquals("11:00", body.get("startTime"));
        assertEquals("13:00", body.get("endTime"));

        verify(roomConflictService, times(5))
                .isRoomAvailable(
                        anyLong(),
                        eq(LocalDate.parse(DATE)),
                        eq(LocalTime.of(11, 0)),
                        eq(LocalTime.of(13, 0))
                );
    }

    @Test
    void shouldExcludeConflictingRooms() {
        authenticateOrganizer();

        when(roomConflictService.isRoomAvailable(
                anyLong(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)
        )).thenReturn(true);

        when(roomConflictService.isRoomAvailable(
                eq(101L),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)
        )).thenReturn(false);

        ResponseEntity<?> response =
                availableRooms(DATE, "12:00", "14:00");

        Map<?, ?> body = responseBody(response);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(4, body.get("totalRooms"));

        List<?> rooms =
                (List<?>) body.get("rooms");

        assertNotNull(rooms);

        assertFalse(
                rooms.stream()
                        .map(Room.class::cast)
                        .anyMatch(room -> room.id().equals(101L))
        );
    }

    @Test
    void shouldReturnEmptyMessageWhenNoRoomsAvailable() {
        authenticateOrganizer();

        when(roomConflictService.isRoomAvailable(
                anyLong(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class)
        )).thenReturn(false);

        ResponseEntity<?> response =
                availableRooms(DATE, "11:00", "13:00");

        Map<?, ?> body = responseBody(response);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(0, body.get("totalRooms"));
        assertEquals(List.of(), body.get("rooms"));

        assertEquals(
                "No rooms are available for the selected date and time.",
                body.get("message")
        );
    }

    @Test
    void shouldRejectMissingAvailabilityParameters() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                availableRooms(DATE, null, "13:00");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectInvalidDateFormat() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                availableRooms("invalid-date", "11:00", "13:00");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectInvalidTimeFormat() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                availableRooms(DATE, "invalid", "13:00");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectPastDates() {
        authenticateOrganizer();

        String yesterday =
                LocalDate.now().minusDays(1).toString();

        ResponseEntity<?> response =
                availableRooms(yesterday, "11:00", "13:00");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectEndTimeBeforeStartTime() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                availableRooms(DATE, "14:00", "12:00");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectEqualStartAndEndTimes() {
        authenticateOrganizer();

        ResponseEntity<?> response =
                availableRooms(DATE, "12:00", "12:00");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectUnauthenticatedAvailabilityRequest() {
        when(authorizationService.getAuthenticatedEmail(
                AUTHORIZATION
        )).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                availableRooms(DATE, "11:00", "13:00");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }

    @Test
    void shouldRejectNonOrganizerAvailabilityRequest() {
        when(authorizationService.getAuthenticatedEmail(
                AUTHORIZATION
        )).thenReturn(
                Optional.of("student@lion.lmu.edu")
        );

        when(authorizationService.hasRole(
                AUTHORIZATION,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                availableRooms(DATE, "11:00", "13:00");

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(roomConflictService);
    }
}
