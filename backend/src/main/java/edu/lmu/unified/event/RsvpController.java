
package edu.lmu.unified.event;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
public class RsvpController {

    private final RsvpService rsvpService;
    private final EventController eventController;
    private final AuthorizationService authorizationService;

    public RsvpController(
            RsvpService rsvpService,
            EventController eventController,
            AuthorizationService authorizationService
    ) {
        this.rsvpService = rsvpService;
        this.eventController = eventController;
        this.authorizationService = authorizationService;
    }

    // Register for an event - STUDENT only
    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<?> addRsvp(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email = authenticatedStudent(authorization);

        if (email.isEmpty()) {
            return accessDenied(authorization);
        }

        Event event = findEvent(eventId);

        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"ACTIVE".equalsIgnoreCase(event.status())
                || LocalDate.parse(event.date())
                        .isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "This event is unavailable for RSVP."
                    ));
        }

        boolean added = rsvpService.addRsvp(
                eventId,
                email.get()
        );

        if (!added) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "You have already RSVP'd to this event."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "RSVP successful!",
                "rsvpCount", rsvpService.getRsvpCount(eventId)
        ));
    }

    // Cancel registration - STUDENT only
    @DeleteMapping("/{eventId}/rsvp")
    public ResponseEntity<?> cancelRsvp(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email = authenticatedStudent(authorization);

        if (email.isEmpty()) {
            return accessDenied(authorization);
        }

        if (findEvent(eventId) == null) {
            return ResponseEntity.notFound().build();
        }

        boolean cancelled = rsvpService.cancelRsvp(
                eventId,
                email.get()
        );

        if (!cancelled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", "No existing RSVP was found."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "RSVP cancelled successfully!",
                "rsvpCount", rsvpService.getRsvpCount(eventId)
        ));
    }

    // Check RSVP status - STUDENT only
    @GetMapping("/{eventId}/rsvp")
    public ResponseEntity<?> getRsvpStatus(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email = authenticatedStudent(authorization);

        if (email.isEmpty()) {
            return accessDenied(authorization);
        }

        if (findEvent(eventId) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "hasRsvp", rsvpService.hasRsvp(eventId, email.get()),
                "rsvpCount", rsvpService.getRsvpCount(eventId)
        ));
    }

    // Validate session and STUDENT role
    private Optional<String> authenticatedStudent(
            String authorization
    ) {
        if (!authorizationService.hasRole(
                authorization,
                UserRole.STUDENT
        )) {
            return Optional.empty();
        }

        return authorizationService.getAuthenticatedEmail(
                authorization
        );
    }

    // Return 401 or 403 depending on authentication
    private ResponseEntity<?> accessDenied(
            String authorization
    ) {
        if (authorizationService
                .getAuthenticatedEmail(authorization)
                .isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please log in to access your RSVP."
                    ));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message",
                        "Access denied. Student role required."
                ));
    }

    private Event findEvent(Long eventId) {
        ResponseEntity<Event> response =
                eventController.getEventById(eventId);

        return response.getBody();
    }
}
