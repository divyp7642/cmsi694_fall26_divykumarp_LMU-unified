package edu.lmu.unified.auth;

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OtpService otpService;

    public AuthController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> beginLogin(
            @Valid @RequestBody LoginRequest request) {

        otpService.requestOtp(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "email", request.email()
        ));
    }
    @PostMapping("/verify")
public ResponseEntity<Map<String, String>> verifyOtp(
        @Valid @RequestBody VerifyOtpRequest request) {

    boolean verified = otpService.verifyOtp(
            request.email(),
            request.otp()
    );

    if (!verified) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "message", "Invalid or expired OTP"
        ));
    }

    return ResponseEntity.ok(Map.of(
            "message", "Authentication successful",
            "email", request.email()
    ));
}
}