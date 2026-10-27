
package edu.lmu.unified.student;

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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:5173")
public class StudentController {

    private final AuthorizationService authorizationService;
    private final EventController eventController;
    private final RsvpService rsvpService;

    public StudentController(
            AuthorizationService authorizationService,
            EventController eventController,
            RsvpService rsvpService
    ) {
        this.authorizationService = authorizationService;
        this.eventController = eventController;
        this.rsvpService = rsvpService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getStudentDashboard(
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
                UserRole.STUDENT
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Student role required."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Welcome to the student dashboard!",
                "email", email.get(),
                "role", UserRole.STUDENT.name()
        ));
    }

    // SCRUM-19 - Retrieve events registered by the student
    @GetMapping("/registered-events")
    public ResponseEntity<?> getRegisteredEvents(
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
                UserRole.STUDENT
        )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Student role required."
                    ));
        }

        List<Event> registeredEvents =
                eventController.getAllEvents()
                        .stream()
                        .filter(event ->
                                rsvpService.hasRsvp(
                                        event.id(),
                                        email.get()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(registeredEvents);
    }
}
