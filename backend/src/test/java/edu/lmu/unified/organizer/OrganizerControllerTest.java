
package edu.lmu.unified.organizer;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.CreateEventRequest;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventSubmissionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizerControllerTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private EventSubmissionService eventSubmissionService;

    @InjectMocks
    private OrganizerController organizerController;

    private CreateEventRequest validRequest;

    private static final String TOKEN = "Bearer test-token";

    private static final String ORGANIZER_EMAIL =
            "organizer@lion.lmu.edu";

    @BeforeEach
    void setUp() {
        validRequest = new CreateEventRequest(
                "LMU Cloud Computing Workshop",
                "A hands-on workshop introducing students to AWS.",
                LocalDate.of(2026, 11, 16),
                "4:00 PM",
                "University Hall",
                "Technology"
        );
    }

    // Test 1 - Unauthenticated users cannot submit events.
    @Test
    void shouldRejectUnauthenticatedUser() {

        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                organizerController.submitEvent(
                        TOKEN,
                        validRequest
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(eventSubmissionService);
    }

    // Test 2 - Student users cannot submit events.
    @Test
    void shouldRejectStudentUser() {

        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("student@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                organizerController.submitEvent(
                        TOKEN,
                        validRequest
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(eventSubmissionService);
    }

    // Test 3 - Admin users cannot submit organizer events.
    @Test
    void shouldRejectAdminUser() {

        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("admin@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                organizerController.submitEvent(
                        TOKEN,
                        validRequest
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(eventSubmissionService);
    }

    // Test 4 - Organizer can successfully submit an event.
    @Test
    void shouldAllowOrganizerToSubmitEvent() {

        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of(ORGANIZER_EMAIL));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(true);

        Event pendingEvent = new Event(
                4L,
                "LMU Cloud Computing Workshop",
                "A hands-on workshop introducing students to AWS.",
                "2026-11-16",
                "4:00 PM",
                "University Hall",
                "Technology",
                ORGANIZER_EMAIL,
                "PENDING_APPROVAL"
        );

        when(eventSubmissionService.submitEvent(
                validRequest,
                ORGANIZER_EMAIL
        )).thenReturn(pendingEvent);

        ResponseEntity<?> response =
                organizerController.submitEvent(
                        TOKEN,
                        validRequest
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertInstanceOf(Map.class, response.getBody());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(
                pendingEvent,
                body.get("event")
        );

        Event returnedEvent = (Event) body.get("event");

        assertEquals(
                "PENDING_APPROVAL",
                returnedEvent.status()
        );

        assertEquals(
                ORGANIZER_EMAIL,
                returnedEvent.organizer()
        );

        verify(eventSubmissionService, times(1))
                .submitEvent(validRequest, ORGANIZER_EMAIL);
    }

    // Test 5 - A successful response contains confirmation.
    @Test
    void shouldReturnSubmissionConfirmation() {

        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of(ORGANIZER_EMAIL));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(true);

        Event pendingEvent = new Event(
                5L,
                "LMU Cloud Computing Workshop",
                "A hands-on workshop introducing students to AWS.",
                "2026-11-16",
                "4:00 PM",
                "University Hall",
                "Technology",
                ORGANIZER_EMAIL,
                "PENDING_APPROVAL"
        );

        when(eventSubmissionService.submitEvent(
                validRequest,
                ORGANIZER_EMAIL
        )).thenReturn(pendingEvent);

        ResponseEntity<?> response =
                organizerController.submitEvent(
                        TOKEN,
                        validRequest
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(
                "Event submitted successfully. Pending administrator approval.",
                body.get("message")
        );

        verify(eventSubmissionService, times(1))
                .submitEvent(validRequest, ORGANIZER_EMAIL);
    }
}
