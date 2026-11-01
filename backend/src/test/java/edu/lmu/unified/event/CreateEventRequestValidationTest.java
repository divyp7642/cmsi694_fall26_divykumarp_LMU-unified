
package edu.lmu.unified.event;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreateEventRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private CreateEventRequest validRequest() {
        return new CreateEventRequest(
                "LMU Technology Workshop",
                "A hands-on technology workshop.",
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "University Hall",
                "Technology"
        );
    }

    @Test
    void shouldAcceptValidRequest() {
        assertTrue(
                validator.validate(validRequest()).isEmpty()
        );
    }

    @Test
    void shouldRejectBlankTitle() {
        CreateEventRequest request = new CreateEventRequest(
                "",
                "Workshop description",
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectTitleOver150Characters() {
        CreateEventRequest request = new CreateEventRequest(
                "A".repeat(151),
                "Workshop description",
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankDescription() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "",
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectDescriptionOver2000Characters() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "A".repeat(2001),
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectMissingDate() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "Workshop description",
                null,
                "4:00 PM",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectPastDate() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "Workshop description",
                LocalDate.now().minusDays(1),
                "4:00 PM",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankTime() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "Workshop description",
                LocalDate.now().plusDays(7),
                "",
                "University Hall",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankLocation() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "Workshop description",
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "",
                "Technology"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectBlankCategory() {
        CreateEventRequest request = new CreateEventRequest(
                "LMU Workshop",
                "Workshop description",
                LocalDate.now().plusDays(7),
                "4:00 PM",
                "University Hall",
                ""
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
