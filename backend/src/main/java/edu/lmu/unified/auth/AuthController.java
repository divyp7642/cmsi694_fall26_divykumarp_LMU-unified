
package edu.lmu.unified.auth;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OtpService otpService;
    private final SessionService sessionService;
    private final RoleService roleService;

    public AuthController(
            OtpService otpService,
            SessionService sessionService,
            RoleService roleService
    ) {
        this.otpService = otpService;
        this.sessionService = sessionService;
        this.roleService = roleService;
    }

    // Step 1 - Send OTP to LMU email
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> beginLogin(
            @Valid @RequestBody LoginRequest request
    ) {
        otpService.requestOtp(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "email", request.email()
        ));
    }

    // Step 2 - Verify OTP and create session
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        boolean verified = otpService.verifyOtp(
                request.email(),
                request.otp()
        );

        if (!verified) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Invalid or expired OTP"
                    ));
        }

        String normalizedEmail =
                request.email().trim().toLowerCase();

        String token = sessionService.createSession(
                normalizedEmail
        );

        UserRole role = roleService.getRole(
                normalizedEmail
        );

        return ResponseEntity.ok(Map.of(
                "message", "Authentication successful",
                "email", normalizedEmail,
                "token", token,
                "role", role.name()
        ));
    }

    // Step 3 - Logout and invalidate session
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Missing session token"
                    ));
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Missing session token"
                    ));
        }

        sessionService.deleteSession(token);

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully"
        ));
    }
}
