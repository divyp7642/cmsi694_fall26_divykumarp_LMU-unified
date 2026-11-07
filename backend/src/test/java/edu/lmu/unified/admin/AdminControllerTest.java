
package edu.lmu.unified.admin;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventController;
import edu.lmu.unified.event.EventRepository;
import edu.lmu.unified.event.RsvpService;
import edu.lmu.unified.notification.Notification;
import edu.lmu.unified.notification.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private EventController eventController;

    @Mock
    private RsvpService rsvpService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminController adminController;

    private static final String TOKEN = "Bearer test-token";

    private static final String ADMIN_EMAIL =
            "admin@lion.lmu.edu";

    private Event createEvent(Long id, String status) {
        return new Event(
                id,
                "LMU AI and Cybersecurity Workshop",
                "A workshop exploring AI security.",
                "2026-11-15",
                "3:00 PM",
                "University Hall",
                "Technology",
                "organizer@lion.lmu.edu",
                status
        );
    }

    private Notification createNotification(
            Event event
    ) {
        return new Notification(
                "notification-" + event.id(),
                event.id(),
                event.title(),
                event.organizer(),
                event.status(),
                "Your event status has been updated.",
                "2026-11-06T12:00:00Z",
                false
        );
    }

    private void mockAdmin() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of(ADMIN_EMAIL));

        when(authorizationService.hasRole(
                TOKEN, UserRole.ADMIN
        )).thenReturn(true);
    }

    private void mockNonAdmin() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.of("student@lion.lmu.edu"));

        when(authorizationService.hasRole(
                TOKEN, UserRole.ADMIN
        )).thenReturn(false);
    }

    // Test 1 - Unauthenticated users cannot view pending events.
    @Test
    void shouldRejectUnauthenticatedPendingEventsRequest() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                adminController.getPendingEvents(TOKEN);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(eventRepository);
    }

    // Test 2 - Students cannot view pending events.
    @Test
    void shouldRejectStudentPendingEventsRequest() {
        mockNonAdmin();

        ResponseEntity<?> response =
                adminController.getPendingEvents(TOKEN);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(eventRepository);
    }

    // Test 3 - Admin can retrieve pending events only.
    @Test
    void shouldReturnOnlyPendingEvents() {
        mockAdmin();

        Event pending = createEvent(4L, "PENDING_APPROVAL");
        Event approved = createEvent(5L, "APPROVED");
        Event rejected = createEvent(6L, "REJECTED");

        when(eventRepository.findAll())
                .thenReturn(List.of(pending, approved, rejected));

        ResponseEntity<?> response =
                adminController.getPendingEvents(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        assertEquals(1, body.get("totalPending"));
        assertEquals(List.of(pending), body.get("events"));
    }

    // Test 4 - Admin approval generates a notification.
    @Test
    void shouldApprovePendingEvent() {
        mockAdmin();

        Event pending = createEvent(4L, "PENDING_APPROVAL");
        Event expectedApproved = createEvent(4L, "APPROVED");
        Notification notification =
                createNotification(expectedApproved);

        when(eventRepository.findById(4L))
                .thenReturn(Optional.of(pending));

        when(notificationService.createDecisionNotification(
                expectedApproved
        )).thenReturn(notification);

        ResponseEntity<?> response =
                adminController.approveEvent(TOKEN, 4L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        Event approved = (Event) body.get("event");

        assertEquals("APPROVED", approved.status());
        assertEquals(pending.id(), approved.id());
        assertEquals(pending.title(), approved.title());
        assertEquals(pending.organizer(), approved.organizer());

        assertEquals(
                "Event approved successfully. Organizer notified.",
                body.get("message")
        );

        assertEquals(
                notification.id(),
                body.get("notificationId")
        );

        verify(eventRepository).save(approved);
        verify(notificationService)
                .createDecisionNotification(approved);
    }

    // Test 5 - Admin rejection generates a notification.
    @Test
    void shouldRejectPendingEvent() {
        mockAdmin();

        Event pending = createEvent(5L, "PENDING_APPROVAL");
        Event expectedRejected = createEvent(5L, "REJECTED");
        Notification notification =
                createNotification(expectedRejected);

        when(eventRepository.findById(5L))
                .thenReturn(Optional.of(pending));

        when(notificationService.createDecisionNotification(
                expectedRejected
        )).thenReturn(notification);

        ResponseEntity<?> response =
                adminController.rejectEvent(TOKEN, 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);

        Event rejected = (Event) body.get("event");

        assertEquals("REJECTED", rejected.status());
        assertEquals(pending.id(), rejected.id());
        assertEquals(pending.title(), rejected.title());

        assertEquals(
                "Event rejected successfully. Organizer notified.",
                body.get("message")
        );

        assertEquals(
                notification.id(),
                body.get("notificationId")
        );

        verify(eventRepository).save(rejected);
        verify(notificationService)
                .createDecisionNotification(rejected);
    }

    // Test 6 - Students cannot approve events.
    @Test
    void shouldPreventStudentFromApproving() {
        mockNonAdmin();

        ResponseEntity<?> response =
                adminController.approveEvent(TOKEN, 4L);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(
                eventRepository,
                notificationService
        );
    }

    // Test 7 - Students cannot reject events.
    @Test
    void shouldPreventStudentFromRejecting() {
        mockNonAdmin();

        ResponseEntity<?> response =
                adminController.rejectEvent(TOKEN, 4L);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        verifyNoInteractions(
                eventRepository,
                notificationService
        );
    }

    // Test 8 - Unauthenticated users cannot approve.
    @Test
    void shouldPreventUnauthenticatedApproval() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                adminController.approveEvent(TOKEN, 4L);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(
                eventRepository,
                notificationService
        );
    }

    // Test 9 - Unauthenticated users cannot reject.
    @Test
    void shouldPreventUnauthenticatedRejection() {
        when(authorizationService.getAuthenticatedEmail(TOKEN))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                adminController.rejectEvent(TOKEN, 4L);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verifyNoInteractions(
                eventRepository,
                notificationService
        );
    }

    // Test 10 - Approving a missing event returns 404.
    @Test
    void shouldReturnNotFoundWhenApprovingMissingEvent() {
        mockAdmin();

        when(eventRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                adminController.approveEvent(TOKEN, 999L);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        verify(eventRepository, never())
                .save(any(Event.class));

        verifyNoInteractions(notificationService);
    }

    // Test 11 - Rejecting a missing event returns 404.
    @Test
    void shouldReturnNotFoundWhenRejectingMissingEvent() {
        mockAdmin();

        when(eventRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                adminController.rejectEvent(TOKEN, 999L);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        verify(eventRepository, never())
                .save(any(Event.class));

        verifyNoInteractions(notificationService);
    }

    // Test 12 - An approved event cannot be approved again.
    @Test
    void shouldPreventApprovingAlreadyApprovedEvent() {
        mockAdmin();

        when(eventRepository.findById(4L))
                .thenReturn(Optional.of(
                        createEvent(4L, "APPROVED")
                ));

        ResponseEntity<?> response =
                adminController.approveEvent(TOKEN, 4L);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        verify(eventRepository, never())
                .save(any(Event.class));

        verifyNoInteractions(notificationService);
    }

    // Test 13 - A rejected event cannot be approved.
    @Test
    void shouldPreventApprovingRejectedEvent() {
        mockAdmin();

        when(eventRepository.findById(5L))
                .thenReturn(Optional.of(
                        createEvent(5L, "REJECTED")
                ));

        ResponseEntity<?> response =
                adminController.approveEvent(TOKEN, 5L);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        verify(eventRepository, never())
                .save(any(Event.class));

        verifyNoInteractions(notificationService);
    }

    // Test 14 - An approved event cannot be rejected.
    @Test
    void shouldPreventRejectingApprovedEvent() {
        mockAdmin();

        when(eventRepository.findById(4L))
                .thenReturn(Optional.of(
                        createEvent(4L, "APPROVED")
                ));

        ResponseEntity<?> response =
                adminController.rejectEvent(TOKEN, 4L);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        verify(eventRepository, never())
                .save(any(Event.class));

        verifyNoInteractions(notificationService);
    }

    // Test 15 - A rejected event cannot be rejected again.
    @Test
    void shouldPreventRejectingAlreadyRejectedEvent() {
        mockAdmin();

        when(eventRepository.findById(5L))
                .thenReturn(Optional.of(
                        createEvent(5L, "REJECTED")
                ));

        ResponseEntity<?> response =
                adminController.rejectEvent(TOKEN, 5L);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        verify(eventRepository, never())
                .save(any(Event.class));

        verifyNoInteractions(notificationService);
    }
}
