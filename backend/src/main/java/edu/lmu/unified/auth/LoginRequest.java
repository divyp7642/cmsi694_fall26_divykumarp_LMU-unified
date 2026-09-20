package edu.lmu.unified.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@(lion\\.)?lmu\\.edu$",
        message = "Use your @lmu.edu or @lion.lmu.edu email"
)
        String email

) {
}