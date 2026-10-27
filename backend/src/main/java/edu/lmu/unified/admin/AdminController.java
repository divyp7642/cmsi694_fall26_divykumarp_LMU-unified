
package edu.lmu.unified.admin;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventController;
import edu.lmu.unified.event.RsvpService;

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

    public AdminController(
            AuthorizationService authorizationService,
            EventController eventController,
            RsvpService rsvpService
    ) {
        this.authorizationService = authorizationService;
        this.eventController = eventController;
        this.rsvpService = rsvpService;
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

        // User must be logged in
        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        // Only administrators can access this dashboard
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

        // Reuse the existing upcoming events
        List<Event> upcomingEvents =
                eventController.getUpcomingEvents();

        // Include RSVP counts for every event
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

        // Total registrations across upcoming events
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
}
