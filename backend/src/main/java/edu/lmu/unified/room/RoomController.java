
package edu.lmu.unified.room;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/organizer/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomRepository roomRepository;

    private final AuthorizationService authorizationService;

    public RoomController(
            RoomRepository roomRepository,
            AuthorizationService authorizationService
    ) {
        this.roomRepository = roomRepository;
        this.authorizationService = authorizationService;
    }

    // Retrieve the campus room catalog.
    @GetMapping
    public ResponseEntity<?> getRooms(
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
}
