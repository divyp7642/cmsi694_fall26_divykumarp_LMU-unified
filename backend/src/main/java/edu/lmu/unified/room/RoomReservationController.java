
package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/organizer/room-reservations")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomReservationController {

    private final AuthorizationService authorizationService;
    private final RoomReservationService reservationService;

    public RoomReservationController(
            AuthorizationService authorizationService,
            RoomReservationService reservationService
    ) {
        this.authorizationService = authorizationService;
        this.reservationService = reservationService;
    }

    // SCRUM-26 - Reserve a room for an approved event.
    @PostMapping
    public ResponseEntity<?> reserveRoom(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @Valid @RequestBody
            CreateRoomReservationRequest request
    ) {

        // Verify that the user is authenticated.
        Optional<String> email =
                authorizationService.getAuthenticatedEmail(
                        authorization
                );

        if (email.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Please log in first."
                    ));
        }

        // Verify that the user has the organizer role.
        if (!authorizationService.hasRole(
                authorization,
                UserRole.ORGANIZER
        )) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied. Organizer role required."
                    ));
        }

        try {
            RoomReservation reservation =
                    reservationService.reserveRoom(
                            request,
                            email.get()
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message",
                            "Room reserved successfully.",
                            "reservation",
                            reservation
                    ));

        } catch (ResponseStatusException exception) {

            String message =
                    exception.getReason() != null
                            ? exception.getReason()
                            : "Unable to complete reservation.";

            return ResponseEntity
                    .status(exception.getStatusCode())
                    .body(Map.of(
                            "message",
                            message
                    ));
        }
    }

    // Return clear messages for invalid reservation fields.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>>
            handleValidationException(
                    MethodArgumentNotValidException exception
            ) {

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(value -> value != null
                        && !value.isBlank())
                .findFirst()
                .orElse("Please provide valid reservation information.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }
}
