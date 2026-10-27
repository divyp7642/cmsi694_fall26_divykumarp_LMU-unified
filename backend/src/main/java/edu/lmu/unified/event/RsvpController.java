
package edu.lmu.unified.event;

import edu.lmu.unified.auth.SessionService;

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
    private final SessionService sessionService;

    public RsvpController(
            RsvpService rsvpService,
            EventController eventController,
            SessionService sessionService
    ) {
        this.rsvpService = rsvpService;
        this.eventController = eventController;
        this.sessionService = sessionService;
    }

    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<?> addRsvp(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email = authenticatedEmail(authorization);

        if (email.isEmpty()) {
            return unauthorized();
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

    @DeleteMapping("/{eventId}/rsvp")
    public ResponseEntity<?> cancelRsvp(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email = authenticatedEmail(authorization);

        if (email.isEmpty()) {
            return unauthorized();
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

    @GetMapping("/{eventId}/rsvp")
    public ResponseEntity<?> getRsvpStatus(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Optional<String> email = authenticatedEmail(authorization);

        if (email.isEmpty()) {
            return unauthorized();
        }

        if (findEvent(eventId) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "hasRsvp", rsvpService.hasRsvp(eventId, email.get()),
                "rsvpCount", rsvpService.getRsvpCount(eventId)
        ));
    }

    private Optional<String> authenticatedEmail(
            String authorization
    ) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return Optional.empty();
        }

        return sessionService.getAuthenticatedEmail(token);
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "message",
                        "Please log in to access your RSVP."
                ));
    }

    private Event findEvent(Long eventId) {
        ResponseEntity<Event> response =
                eventController.getEventById(eventId);

        return response.getBody();
    }
}
