
package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomControllerTest {

    private AuthorizationService authorizationService;

    private RoomRepository roomRepository;

    private RoomController roomController;

    private static final String AUTHORIZATION =
            "Bearer test-token";

    private static final String ORGANIZER_EMAIL =
            "organizer@lion.lmu.edu";

    @BeforeEach
    void setUp() {

        authorizationService =
                mock(AuthorizationService.class);

        roomRepository =
                new RoomRepository();

        roomController =
                new RoomController(
                        roomRepository,
                        authorizationService
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

    @Test
    void shouldReturnAllRoomsForOrganizer() {

        authenticateOrganizer();

        ResponseEntity<?> response =
                roomController.getRooms(AUTHORIZATION);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertInstanceOf(
                Map.class,
                response.getBody()
        );

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertEquals(
                "Campus rooms retrieved successfully.",
                body.get("message")
        );

        assertEquals(
                5,
                body.get("totalRooms")
        );

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

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        List<?> rooms =
                (List<?>) body.get("rooms");

        assertEquals(5, rooms.size());

        Room expectedRoom = new Room(
                101L,
                "Conference Room A",
                "University Hall",
                50
        );

        assertEquals(
                expectedRoom,
                rooms.get(0)
        );
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

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

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

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

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

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        assertEquals(
                "Access denied. Organizer role required.",
                body.get("message")
        );
    }
}
