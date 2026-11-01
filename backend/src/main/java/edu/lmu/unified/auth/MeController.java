
package edu.lmu.unified.auth;

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
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class MeController {

    private final AuthorizationService authorizationService;
    private final RoleService roleService;

    public MeController(
            AuthorizationService authorizationService,
            RoleService roleService
    ) {
        this.authorizationService = authorizationService;
        this.roleService = roleService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
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

        UserRole role = roleService.getRole(email.get());

        return ResponseEntity.ok(Map.of(
                "email", email.get(),
                "role", role.name()
        ));
    }
}
