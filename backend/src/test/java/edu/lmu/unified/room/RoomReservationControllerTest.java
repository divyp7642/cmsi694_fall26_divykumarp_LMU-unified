package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomReservationControllerTest {

    private AuthorizationService authorizationService;
    private RoomReservationService reservationService;
    private RoomReservationController controller;

    private final String token = "Bearer test-token";

    private final String organizerEmail =
            "organizer@lion.lmu.edu";

    private LocalDate reservationDate;

    @BeforeEach
    void setUp() {

        authorizationService =
                mock(AuthorizationService.class);

        reservationService =
                mock(RoomReservationService.class);

        controller =
                new RoomReservationController(
                        authorizationService,
                        reservationService
                );

        reservationDate =
                LocalDate.now().plusDays(5);
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

    private RoomReservation createReservation() {

        return new RoomReservation(
                1L,
                101L,
                5L,
                organizerEmail,
                reservationDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                ReservationStatus.CONFIRMED
        );
    }

    private void mockAuthenticatedOrganizer() {

        when(authorizationService
                .getAuthenticatedEmail(token))
                .thenReturn(
                        Optional.of(organizerEmail)
                );

        when(authorizationService.hasRole(
                token,
                UserRole.ORGANIZER
        )).thenReturn(true);
    }

    @Test
    void shouldReturnCreatedForSuccessfulReservation() {

        mockAuthenticatedOrganizer();

        CreateRoomReservationRequest request =
                createRequest();

        RoomReservation reservation =
                createReservation();

        when(reservationService.reserveRoom(
                request,
                organizerEmail
        )).thenReturn(reservation);

        ResponseEntity<?> response =
                controller.reserveRoom(
                        token,
                        request
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        assertEquals(
                "Room reserved successfully.",
                body.get("message")
        );

        assertEquals(
                reservation,
                body.get("reservation")
        );

        verify(reservationService)
                .reserveRoom(
                        request,
                        organizerEmail
                );
    }

    @Test
    void shouldReturnUnauthorizedWhenNotLoggedIn() {

        when(authorizationService
                .getAuthenticatedEmail(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.reserveRoom(
                        null,
                        createRequest()
                );

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

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() {

        String invalidToken =
                "Bearer invalid-token";

        when(authorizationService
                .getAuthenticatedEmail(invalidToken))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.reserveRoom(
                        invalidToken,
                        createRequest()
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnForbiddenForNonOrganizer() {

        when(authorizationService
                .getAuthenticatedEmail(token))
                .thenReturn(
                        Optional.of(
                                "student@lion.lmu.edu"
                        )
                );

        when(authorizationService.hasRole(
                token,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                controller.reserveRoom(
                        token,
                        createRequest()
                );

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

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnConflictForOverlappingReservation() {

        mockAuthenticatedOrganizer();

        CreateRoomReservationRequest request =
                createRequest();

        when(reservationService.reserveRoom(
                request,
                organizerEmail
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The selected room is unavailable during "
                                + "this time. Please select another "
                                + "room or time."
                )
        );

        ResponseEntity<?> response =
                controller.reserveRoom(
                        token,
                        request
                );

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        assertEquals(
                "The selected room is unavailable during "
                        + "this time. Please select another "
                        + "room or time.",
                body.get("message")
        );
    }

    @Test
    void shouldReturnBadRequestForUnapprovedEvent() {

        mockAuthenticatedOrganizer();

        CreateRoomReservationRequest request =
                createRequest();

        when(reservationService.reserveRoom(
                request,
                organizerEmail
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only approved events can reserve rooms."
                )
        );

        ResponseEntity<?> response =
                controller.reserveRoom(
                        token,
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        assertEquals(
                "Only approved events can reserve rooms.",
                body.get("message")
        );
    }

    @Test
    void shouldReturnNotFoundForMissingEvent() {

        mockAuthenticatedOrganizer();

        CreateRoomReservationRequest request =
                createRequest();

        when(reservationService.reserveRoom(
                request,
                organizerEmail
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found."
                )
        );

        ResponseEntity<?> response =
                controller.reserveRoom(
                        token,
                        request
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        assertEquals(
                "Event not found.",
                body.get("message")
        );
    }

    @Test
    void shouldReturnForbiddenForAnotherOrganizersEvent() {

        mockAuthenticatedOrganizer();

        CreateRoomReservationRequest request =
                createRequest();

        when(reservationService.reserveRoom(
                request,
                organizerEmail
        )).thenThrow(
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only reserve rooms for your own events."
                )
        );

        ResponseEntity<?> response =
                controller.reserveRoom(
                        token,
                        request
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        Map<?, ?> body =
                (Map<?, ?>) response.getBody();

        assertNotNull(body);

        assertEquals(
                "You can only reserve rooms for your own events.",
                body.get("message")
        );
    }
}