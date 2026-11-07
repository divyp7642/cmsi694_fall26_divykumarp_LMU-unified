
package edu.lmu.unified.admin;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventController;
import edu.lmu.unified.event.EventRepository;
import edu.lmu.unified.event.RsvpService;
import edu.lmu.unified.notification.Notification;
import edu.lmu.unified.notification.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final AuthorizationService authorizationService;
    private final EventController eventController;
    private final RsvpService rsvpService;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    public AdminController(
            AuthorizationService authorizationService,
            EventController eventController,
            RsvpService rsvpService,
            EventRepository eventRepository,
            NotificationService notificationService
    ) {
        this.authorizationService = authorizationService;
        this.eventController = eventController;
        this.rsvpService = rsvpService;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    // SCRUM-20 - Administrator Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
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
                UserRole.ADMIN
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Administrator role required."
                    ));
        }

        List<Event> upcomingEvents =
                eventController.getUpcomingEvents();

        List<Map<String, Object>> eventsWithRsvpCounts =
                upcomingEvents.stream()
                        .map(event -> Map.<String, Object>of(
                                "id", event.id(),
                                "title", event.title(),
                                "description", event.description(),
                                "date", event.date(),
                                "time", event.time(),
                                "location", event.location(),
                                "category", event.category(),
                                "organizer", event.organizer(),
                                "status", event.status(),
                                "rsvpCount",
                                rsvpService.getRsvpCount(event.id())
                        ))
                        .toList();

        long totalRsvps = eventsWithRsvpCounts.stream()
                .mapToLong(event ->
                        (Long) event.get("rsvpCount")
                )
                .sum();

        return ResponseEntity.ok(Map.of(
                "message", "Welcome to the administrator dashboard!",
                "email", email.get(),
                "role", "ADMIN",
                "totalEvents", upcomingEvents.size(),
                "totalRsvps", totalRsvps,
                "events", eventsWithRsvpCounts
        ));
    }

    // SCRUM-23 - View pending events
    @GetMapping("/events/pending")
    public ResponseEntity<?> getPendingEvents(
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
                UserRole.ADMIN
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Administrator role required."
                    ));
        }

        List<Event> pendingEvents =
                eventRepository.findAll()
                        .stream()
                        .filter(event ->
                                "PENDING_APPROVAL".equals(
                                        event.status()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(Map.of(
                "message",
                "Pending events retrieved successfully.",
                "totalPending",
                pendingEvents.size(),
                "events",
                pendingEvents
        ));
    }

    // SCRUM-23/24 - Approve event and notify organizer.
    @PutMapping("/events/{id}/approve")
    public ResponseEntity<?> approveEvent(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @PathVariable Long id
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
                UserRole.ADMIN
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Administrator role required."
                    ));
        }

        Optional<Event> existingEvent =
                eventRepository.findById(id);

        if (existingEvent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", "Event not found."
                    ));
        }

        Event event = existingEvent.get();

        if (!"PENDING_APPROVAL".equals(event.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Only pending events can be approved."
                    ));
        }

        Event approvedEvent = new Event(
                event.id(),
                event.title(),
                event.description(),
                event.date(),
                event.time(),
                event.location(),
                event.category(),
                event.organizer(),
                "APPROVED"
        );

        eventRepository.save(approvedEvent);

        Notification notification =
                notificationService.createDecisionNotification(
                        approvedEvent
                );

        return ResponseEntity.ok(Map.of(
                "message",
                "Event approved successfully. Organizer notified.",
                "event",
                approvedEvent,
                "notificationId",
                notification.id()
        ));
    }

    // SCRUM-23/24 - Reject event and notify organizer.
    @PutMapping("/events/{id}/reject")
    public ResponseEntity<?> rejectEvent(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @PathVariable Long id
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
                UserRole.ADMIN
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Administrator role required."
                    ));
        }

        Optional<Event> existingEvent =
                eventRepository.findById(id);

        if (existingEvent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", "Event not found."
                    ));
        }

        Event event = existingEvent.get();

        if (!"PENDING_APPROVAL".equals(event.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Only pending events can be rejected."
                    ));
        }

        Event rejectedEvent = new Event(
                event.id(),
                event.title(),
                event.description(),
                event.date(),
                event.time(),
                event.location(),
                event.category(),
                event.organizer(),
                "REJECTED"
        );

        eventRepository.save(rejectedEvent);

        Notification notification =
                notificationService.createDecisionNotification(
                        rejectedEvent
                );

        return ResponseEntity.ok(Map.of(
                "message",
                "Event rejected successfully. Organizer notified.",
                "event",
                rejectedEvent,
                "notificationId",
                notification.id()
        ));
    }
}
