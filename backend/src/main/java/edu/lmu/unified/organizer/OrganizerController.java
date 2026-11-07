
package edu.lmu.unified.organizer;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.CreateEventRequest;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventRepository;
import edu.lmu.unified.event.EventSubmissionService;
import edu.lmu.unified.notification.Notification;
import edu.lmu.unified.notification.NotificationService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer")
@CrossOrigin(origins = "http://localhost:5173")
public class OrganizerController {

    private final AuthorizationService authorizationService;
    private final EventSubmissionService eventSubmissionService;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    public OrganizerController(
            AuthorizationService authorizationService,
            EventSubmissionService eventSubmissionService,
            EventRepository eventRepository,
            NotificationService notificationService
    ) {
        this.authorizationService = authorizationService;
        this.eventSubmissionService = eventSubmissionService;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    // SCRUM-22 - Submit an event for administrator approval.
    @PostMapping("/events")
    public ResponseEntity<?> submitEvent(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @Valid @RequestBody CreateEventRequest request
    ) {
        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        if (!authorizationService.hasRole(
                authorization,
                UserRole.ORGANIZER
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Organizer role required."
                    ));
        }

        Event createdEvent =
                eventSubmissionService.submitEvent(
                        request,
                        email.get()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Event submitted successfully. Pending administrator approval.",
                        "event", createdEvent
                ));
    }

    // SCRUM-24 - View events submitted by the logged-in organizer.
    @GetMapping("/events")
    public ResponseEntity<?> getMyEvents(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        if (!authorizationService.hasRole(
                authorization,
                UserRole.ORGANIZER
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Organizer role required."
                    ));
        }

        List<Event> myEvents =
                eventRepository.findAll()
                        .stream()
                        .filter(event ->
                                email.get().equalsIgnoreCase(
                                        event.organizer()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(Map.of(
                "message",
                "Your submitted events retrieved successfully.",
                "totalEvents",
                myEvents.size(),
                "events",
                myEvents
        ));
    }

    // SCRUM-24 - View notifications for the logged-in organizer.
    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        if (!authorizationService.hasRole(
                authorization,
                UserRole.ORGANIZER
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Organizer role required."
                    ));
        }

        List<Notification> notifications =
                notificationService.getOrganizerNotifications(
                        email.get()
                );

        long unreadCount =
                notifications.stream()
                        .filter(notification -> !notification.read())
                        .count();

        return ResponseEntity.ok(Map.of(
                "message",
                "Your notifications retrieved successfully.",
                "totalNotifications",
                notifications.size(),
                "unreadCount",
                unreadCount,
                "notifications",
                notifications
        ));
    }

    // SCRUM-24 - Mark one notification as read.
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @PathVariable String id
    ) {
        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        if (!authorizationService.hasRole(
                authorization,
                UserRole.ORGANIZER
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Organizer role required."
                    ));
        }

        Optional<Notification> updated =
                notificationService.markAsRead(
                        id,
                        email.get()
                );

        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", "Notification not found."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Notification marked as read.",
                "notification", updated.get()
        ));
    }

    // SCRUM-24 - Mark all notifications as read.
    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsAsRead(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        if (!authorizationService.hasRole(
                authorization,
                UserRole.ORGANIZER
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Organizer role required."
                    ));
        }

        int updatedCount =
                notificationService.markAllAsRead(
                        email.get()
                );

        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read.",
                "updatedCount", updatedCount
        ));
    }
}
