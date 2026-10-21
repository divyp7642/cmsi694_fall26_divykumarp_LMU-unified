
package edu.lmu.unified.admin;

import edu.lmu.unified.auth.AuthorizationService;
import edu.lmu.unified.auth.UserRole;

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

    public AdminController(
            AuthorizationService authorizationService
    ) {
        this.authorizationService = authorizationService;
    }

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

        return ResponseEntity.ok(Map.of(
                "message", "Welcome to the administrator dashboard!",
                "email", email.get(),
                "role", "ADMIN"
        ));
    }
}
