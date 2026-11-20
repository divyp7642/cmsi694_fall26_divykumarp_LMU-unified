
package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/organizer/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomRepository roomRepository;
    private final AuthorizationService authorizationService;
    private final RoomConflictService roomConflictService;

    public RoomController(
            RoomRepository roomRepository,
            AuthorizationService authorizationService,
            RoomConflictService roomConflictService
    ) {
        this.roomRepository = roomRepository;
        this.authorizationService = authorizationService;
        this.roomConflictService = roomConflictService;
    }

    // Retrieve the complete campus room catalog.
    @GetMapping
    public ResponseEntity<?> getRooms(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        ResponseEntity<?> accessError =
                checkOrganizerAccess(authorization);

        if (accessError != null) {
            return accessError;
        }

        List<Room> rooms = roomRepository.findAll();

        return ResponseEntity.ok(Map.of(
                "message",
                "Campus rooms retrieved successfully.",
                "totalRooms",
                rooms.size(),
                "rooms",
                rooms
        ));
    }

    // Retrieve rooms available for a selected date and time.
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableRooms(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,

            @RequestParam(required = false)
            String date,

            @RequestParam(required = false)
            String startTime,

            @RequestParam(required = false)
            String endTime
    ) {
        ResponseEntity<?> accessError =
                checkOrganizerAccess(authorization);

        if (accessError != null) {
            return accessError;
        }

        if (date == null || date.isBlank()
                || startTime == null || startTime.isBlank()
                || endTime == null || endTime.isBlank()) {

            return badRequest(
                    "Date, start time, and end time are required."
            );
        }

        final LocalDate selectedDate;
        final LocalTime selectedStartTime;
        final LocalTime selectedEndTime;

        try {
            selectedDate = LocalDate.parse(date);
            selectedStartTime = LocalTime.parse(startTime);
            selectedEndTime = LocalTime.parse(endTime);
        } catch (DateTimeParseException exception) {
            return badRequest(
                    "Invalid date or time format. Use YYYY-MM-DD for date "
                            + "and HH:mm for time."
            );
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            return badRequest(
                    "Reservation date cannot be in the past."
            );
        }

        if (!selectedStartTime.isBefore(selectedEndTime)) {
            return badRequest(
                    "End time must be after start time."
            );
        }

        List<Room> availableRooms =
                roomRepository.findAll()
                        .stream()
                        .filter(room ->
                                roomConflictService.isRoomAvailable(
                                        room.id(),
                                        selectedDate,
                                        selectedStartTime,
                                        selectedEndTime
                                )
                        )
                        .toList();

        String message = availableRooms.isEmpty()
                ? "No rooms are available for the selected date and time."
                : "Available campus rooms retrieved successfully.";

        return ResponseEntity.ok(Map.of(
                "message", message,
                "date", selectedDate.toString(),
                "startTime", selectedStartTime.toString(),
                "endTime", selectedEndTime.toString(),
                "totalRooms", availableRooms.size(),
                "rooms", availableRooms
        ));
    }

    // Require a logged-in organizer for both endpoints.
    private ResponseEntity<?> checkOrganizerAccess(
            String authorization
    ) {
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

        return null;
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", message));
    }
}
