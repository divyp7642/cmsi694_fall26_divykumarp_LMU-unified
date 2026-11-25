package edu.lmu.unified.student;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;
import edu.lmu.unified.event.SavedEventService;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student/saved-events")
@CrossOrigin(origins = "http://localhost:5173")
public class SavedEventController {

    private final AuthorizationService authorizationService;
    private final SavedEventService savedEventService;

    public SavedEventController(
            AuthorizationService authorizationService,
            SavedEventService savedEventService
    ) {
        this.authorizationService = authorizationService;
        this.savedEventService = savedEventService;
    }

    // SCRUM-28 - Save an event.
    @PostMapping("/{eventId}")
    public ResponseEntity<?> saveEvent(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        try {
            String email = requireStudent(authorization);

            boolean saved = savedEventService.saveEvent(
                    eventId,
                    email
            );

            if (!saved) {
                return ResponseEntity.ok(Map.of(
                        "message", "Event is already saved.",
                        "eventId", eventId,
                        "saved", true
                ));
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Event saved successfully.",
                            "eventId", eventId,
                            "saved", true
                    ));

        } catch (ResponseStatusException exception) {
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .body(Map.of(
                            "message", exception.getReason()
                    ));
        }
    }

    // SCRUM-28 - Remove a saved event.
    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> removeSavedEvent(
            @PathVariable Long eventId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        try {
            String email = requireStudent(authorization);

            boolean removed =
                    savedEventService.removeSavedEvent(
                            eventId,
                            email
                    );

            if (!removed) {
                return ResponseEntity.ok(Map.of(
                        "message", "Event is not in your saved events.",
                        "eventId", eventId,
                        "saved", false
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Saved event removed successfully.",
                    "eventId", eventId,
                    "saved", false
            ));

        } catch (ResponseStatusException exception) {
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .body(Map.of(
                            "message", exception.getReason()
                    ));
        }
    }

    // SCRUM-28 - Retrieve the student's saved event IDs.
    @GetMapping("/ids")
    public ResponseEntity<?> getSavedEventIds(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        try {
            String email = requireStudent(authorization);

            return ResponseEntity.ok(
                    savedEventService.getSavedEventIds(email)
            );

        } catch (ResponseStatusException exception) {
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .body(Map.of(
                            "message", exception.getReason()
                    ));
        }
    }

    // Authenticate the user and enforce student-only access.
    private String requireStudent(String authorization) {

        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first."
            );
        }

        if (!authorizationService.hasRole(
                authorization,
                UserRole.STUDENT
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied. Student role required."
            );
        }

        return email.get();
    }
}