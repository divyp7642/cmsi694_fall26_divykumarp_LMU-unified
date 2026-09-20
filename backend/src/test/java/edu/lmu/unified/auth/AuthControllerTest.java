package edu.lmu.unified.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsLmuEmail() {
        LoginRequest request =
                new LoginRequest("dpatel52@lion.lmu.edu");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsNonLmuEmail() {
        LoginRequest request =
                new LoginRequest("test@gmail.com");

        assertFalse(validator.validate(request).isEmpty());
    }
}