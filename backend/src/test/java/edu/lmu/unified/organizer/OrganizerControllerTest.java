
package edu.lmu.unified.organizer;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.CreateEventRequest;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventRepository;
import edu.lmu.unified.event.EventSubmissionService;
import edu.lmu.unified.notification.Notification;
import edu.lmu.unified.notification.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
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

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrganizerController organizerController;

    private CreateEventRequest validRequest;

    private static final String TOKEN = "Bearer test-token";

    private static final String ORGANIZER_EMAIL =
            "organizer@lion.lmu.edu";

    private static final String OTHER_EMAIL =
            "other@lion.lmu.edu";

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

    private void mockOrganizer() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of(ORGANIZER_EMAIL));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(true);
    }

    private Event createEvent(
            Long id,
            String organizerEmail,
            String status
    ) {
        return new Event(
                id,
                "LMU Cloud Computing Workshop",
                "A hands-on workshop introducing students to AWS.",
                "2026-11-16",
                "4:00 PM",
                "University Hall",
                "Technology",
                organizerEmail,
                status
        );
    }

    private Notification createNotification(
            String id,
            String organizerEmail,
            String decision,
            boolean read
    ) {
        return new Notification(
                id,
                4L,
                "LMU Cloud Computing Workshop",
                organizerEmail,
                decision,
                "Your event has been "
                        + decision.toLowerCase() + ".",
                "2026-11-06T12:00:00Z",
                read
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
        mockOrganizer();

        Event pendingEvent = createEvent(
                4L,
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

        assertEquals(pendingEvent, body.get("event"));

        Event returnedEvent = (Event) body.get("event");

        assertEquals(
                "PENDING_APPROVAL",
                returnedEvent.status()
        );

        assertEquals(
                ORGANIZER_EMAIL,
                returnedEvent.organizer()
        );

        verify(eventSubmissionService)
                .submitEvent(validRequest, ORGANIZER_EMAIL);
    }

    // Test 5 - A successful response contains confirmation.
    @Test
    void shouldReturnSubmissionConfirmation() {
        mockOrganizer();

        Event pendingEvent = createEvent(
                5L,
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

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(
                "Event submitted successfully. Pending administrator approval.",
                body.get("message")
        );

        verify(eventSubmissionService)
                .submitEvent(validRequest, ORGANIZER_EMAIL);
    }

    // Test 6 - Organizer sees their own event statuses.
    @Test
    void shouldReturnOrganizerSubmittedEvents() {
        mockOrganizer();

        Event pending = createEvent(
                4L, ORGANIZER_EMAIL, "PENDING_APPROVAL"
        );

        Event approved = createEvent(
                5L, ORGANIZER_EMAIL, "APPROVED"
        );

        Event rejected = createEvent(
                6L, ORGANIZER_EMAIL, "REJECTED"
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(pending, approved, rejected));

        ResponseEntity<?> response =
                organizerController.getMyEvents(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(3, body.get("totalEvents"));
        assertEquals(
                List.of(pending, approved, rejected),
                body.get("events")
        );
    }

    // Test 7 - Organizer cannot see another organizer's events.
    @Test
    void shouldHideOtherOrganizersEvents() {
        mockOrganizer();

        Event ownEvent = createEvent(
                4L, ORGANIZER_EMAIL, "APPROVED"
        );

        Event otherEvent = createEvent(
                5L, OTHER_EMAIL, "REJECTED"
        );

        when(eventRepository.findAll())
                .thenReturn(List.of(ownEvent, otherEvent));

        ResponseEntity<?> response =
                organizerController.getMyEvents(TOKEN);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(1, body.get("totalEvents"));
        assertEquals(List.of(ownEvent), body.get("events"));
    }

    // Test 8 - Organizer can view their notifications.
    @Test
    void shouldReturnOrganizerNotifications() {
        mockOrganizer();

        Notification approved = createNotification(
                "notification-1",
                ORGANIZER_EMAIL,
                "APPROVED",
                false
        );

        Notification rejected = createNotification(
                "notification-2",
                ORGANIZER_EMAIL,
                "REJECTED",
                true
        );

        when(notificationService.getOrganizerNotifications(
                ORGANIZER_EMAIL
        )).thenReturn(List.of(approved, rejected));

        ResponseEntity<?> response =
                organizerController.getMyNotifications(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(2, body.get("totalNotifications"));
        assertEquals(1L, body.get("unreadCount"));

        assertEquals(
                List.of(approved, rejected),
                body.get("notifications")
        );

        verify(notificationService)
                .getOrganizerNotifications(ORGANIZER_EMAIL);
    }

    // Test 9 - Notification requests use authenticated email.
    @Test
    void shouldRequestOnlyAuthenticatedOrganizersNotifications() {
        mockOrganizer();

        when(notificationService.getOrganizerNotifications(
                ORGANIZER_EMAIL
        )).thenReturn(List.of());

        ResponseEntity<?> response =
                organizerController.getMyNotifications(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(notificationService)
                .getOrganizerNotifications(ORGANIZER_EMAIL);

        verify(notificationService, never())
                .getOrganizerNotifications(OTHER_EMAIL);
    }

    // Test 10 - Unauthenticated users cannot view events.
    @Test
    void shouldRejectUnauthenticatedEventsRequest() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                organizerController.getMyEvents(TOKEN);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(eventRepository);
    }

    // Test 11 - Students cannot view organizer events.
    @Test
    void shouldRejectStudentEventsRequest() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("student@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN, UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                organizerController.getMyEvents(TOKEN);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(eventRepository);
    }

    // Test 12 - Unauthenticated users cannot view notifications.
    @Test
    void shouldRejectUnauthenticatedNotificationsRequest() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                organizerController.getMyNotifications(TOKEN);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(notificationService);
    }

    // Test 13 - Students cannot view organizer notifications.
    @Test
    void shouldRejectStudentNotificationsRequest() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("student@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN, UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                organizerController.getMyNotifications(TOKEN);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(notificationService);
    }

    // Test 14 - Organizer can mark their notification as read.
    @Test
    void shouldMarkNotificationAsRead() {
        mockOrganizer();

        Notification updated = createNotification(
                "notification-1",
                ORGANIZER_EMAIL,
                "APPROVED",
                true
        );

        when(notificationService.markAsRead(
                "notification-1",
                ORGANIZER_EMAIL
        )).thenReturn(Optional.of(updated));

        ResponseEntity<?> response =
                organizerController.markNotificationAsRead(
                        TOKEN,
                        "notification-1"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(
                "Notification marked as read.",
                body.get("message")
        );
        assertEquals(updated, body.get("notification"));
        assertTrue(((Notification) body.get("notification")).read());

        verify(notificationService).markAsRead(
                "notification-1",
                ORGANIZER_EMAIL
        );
    }

    // Test 15 - Missing or non-owned notifications return 404.
    @Test
    void shouldReturnNotFoundWhenNotificationCannotBeMarked() {
        mockOrganizer();

        when(notificationService.markAsRead(
                "unknown-notification",
                ORGANIZER_EMAIL
        )).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                organizerController.markNotificationAsRead(
                        TOKEN,
                        "unknown-notification"
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        verify(notificationService).markAsRead(
                "unknown-notification",
                ORGANIZER_EMAIL
        );
    }

    // Test 16 - Mark as read always uses authenticated email.
    @Test
    void shouldUseAuthenticatedEmailWhenMarkingRead() {
        mockOrganizer();

        when(notificationService.markAsRead(
                "notification-2",
                ORGANIZER_EMAIL
        )).thenReturn(Optional.of(createNotification(
                "notification-2",
                ORGANIZER_EMAIL,
                "REJECTED",
                true
        )));

        ResponseEntity<?> response =
                organizerController.markNotificationAsRead(
                        TOKEN,
                        "notification-2"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(notificationService).markAsRead(
                "notification-2",
                ORGANIZER_EMAIL
        );

        verify(notificationService, never()).markAsRead(
                "notification-2",
                OTHER_EMAIL
        );
    }

    // Test 17 - Unauthenticated users cannot mark as read.
    @Test
    void shouldRejectUnauthenticatedMarkAsRead() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                organizerController.markNotificationAsRead(
                        TOKEN,
                        "notification-1"
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(notificationService);
    }

    // Test 18 - Students cannot mark notifications as read.
    @Test
    void shouldRejectStudentMarkAsRead() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("student@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                organizerController.markNotificationAsRead(
                        TOKEN,
                        "notification-1"
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(notificationService);
    }

    // Test 19 - Organizer can mark all notifications as read.
    @Test
    void shouldMarkAllNotificationsAsRead() {
        mockOrganizer();

        when(notificationService.markAllAsRead(
                ORGANIZER_EMAIL
        )).thenReturn(2);

        ResponseEntity<?> response =
                organizerController.markAllNotificationsAsRead(
                        TOKEN
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(
                "All notifications marked as read.",
                body.get("message")
        );
        assertEquals(2, body.get("updatedCount"));

        verify(notificationService)
                .markAllAsRead(ORGANIZER_EMAIL);
    }

    // Test 20 - Unauthenticated users cannot mark all as read.
    @Test
    void shouldRejectUnauthenticatedMarkAllAsRead() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                organizerController.markAllNotificationsAsRead(
                        TOKEN
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(notificationService);
    }

    // Test 21 - Students cannot mark all notifications as read.
    @Test
    void shouldRejectStudentMarkAllAsRead() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("student@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN,
                UserRole.ORGANIZER
        )).thenReturn(false);

        ResponseEntity<?> response =
                organizerController.markAllNotificationsAsRead(
                        TOKEN
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(notificationService);
    }
}
