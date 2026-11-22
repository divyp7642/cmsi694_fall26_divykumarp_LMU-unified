
package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoomReservationControllerTest {

    private AuthorizationService authorizationService;
    private RoomReservationService reservationService;
    private RoomReservationController controller;
    private MockMvc mockMvc;

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

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

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

    private MethodArgumentNotValidException
            createValidationException(
                    String field,
                    String message
            ) {

        CreateRoomReservationRequest request =
                createRequest();

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        request,
                        "request"
                );

        bindingResult.addError(
                new FieldError(
                        "request",
                        field,
                        message
                )
        );

        return new MethodArgumentNotValidException(
                null,
                bindingResult
        );
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

    // SCRUM-26: Direct validation-handler tests.

    @Test
    void shouldReturnBadRequestForMissingRoomId() {

        MethodArgumentNotValidException exception =
                createValidationException(
                        "roomId",
                        "Room ID is required"
                );

        ResponseEntity<Map<String, String>> response =
                controller.handleValidationException(
                        exception
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Room ID is required",
                response.getBody().get("message")
        );
    }

    @Test
    void shouldReturnBadRequestForMissingEventId() {

        MethodArgumentNotValidException exception =
                createValidationException(
                        "eventId",
                        "Event ID is required"
                );

        ResponseEntity<Map<String, String>> response =
                controller.handleValidationException(
                        exception
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Event ID is required",
                response.getBody().get("message")
        );
    }

    @Test
    void shouldReturnBadRequestForInvalidRoomId() {

        MethodArgumentNotValidException exception =
                createValidationException(
                        "roomId",
                        "Room ID must be positive"
                );

        ResponseEntity<Map<String, String>> response =
                controller.handleValidationException(
                        exception
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Room ID must be positive",
                response.getBody().get("message")
        );
    }

    @Test
    void shouldReturnBadRequestForMissingStartTime() {

        MethodArgumentNotValidException exception =
                createValidationException(
                        "startTime",
                        "Start time is required"
                );

        ResponseEntity<Map<String, String>> response =
                controller.handleValidationException(
                        exception
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Start time is required",
                response.getBody().get("message")
        );
    }

    @Test
    void shouldReturnFallbackMessageForValidationError() {

        MethodArgumentNotValidException exception =
                createValidationException(
                        "roomId",
                        ""
                );

        ResponseEntity<Map<String, String>> response =
                controller.handleValidationException(
                        exception
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Please provide valid reservation information.",
                response.getBody().get("message")
        );
    }

    // SCRUM-26: HTTP-level validation tests.
    // MockMvc sends requests through Spring MVC,
    // including JSON binding and @Valid processing.

    @Test
    void shouldRejectMissingRoomIdThroughHttp()
            throws Exception {

        String json = """
                {
                  "eventId": 5,
                  "date": "%s",
                  "startTime": "10:00",
                  "endTime": "12:00"
                }
                """.formatted(reservationDate);

        mockMvc.perform(
                post("/api/organizer/room-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(json)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.message")
                        .value("Room ID is required")
        );

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldRejectInvalidRoomIdThroughHttp()
            throws Exception {

        String json = """
                {
                  "roomId": -1,
                  "eventId": 5,
                  "date": "%s",
                  "startTime": "10:00",
                  "endTime": "12:00"
                }
                """.formatted(reservationDate);

        mockMvc.perform(
                post("/api/organizer/room-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(json)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.message")
                        .value("Room ID must be positive")
        );

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldRejectMissingStartTimeThroughHttp()
            throws Exception {

        String json = """
                {
                  "roomId": 101,
                  "eventId": 5,
                  "date": "%s",
                  "endTime": "12:00"
                }
                """.formatted(reservationDate);

        mockMvc.perform(
                post("/api/organizer/room-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(json)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.message")
                        .value("Start time is required")
        );

        verifyNoInteractions(reservationService);
    }
}
