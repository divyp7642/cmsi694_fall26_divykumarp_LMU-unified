
package edu.lmu.unified.organizer;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.CreateEventRequest;
import edu.lmu.unified.event.Event;
import edu.lmu.unified.event.EventSubmissionService;

import jakarta.validation.Valid;

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

    public OrganizerController(
            AuthorizationService authorizationService,
            EventSubmissionService eventSubmissionService
    ) {
        this.authorizationService = authorizationService;
        this.eventSubmissionService = eventSubmissionService;
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

        // Authentication is required.
        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Please log in first."
                    ));
        }

        // Only authorized organizers can submit events.
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
}
