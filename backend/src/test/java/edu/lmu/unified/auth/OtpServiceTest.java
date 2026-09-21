package edu.lmu.unified.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OtpServiceTest {

    @Test
    void requestOtpGeneratesSixDigitCodeAndNormalizesEmail() {
        EmailService emailService = mock(EmailService.class);
        OtpService otpService = new OtpService(emailService);

        otpService.requestOtp(" Student@Lion.LMU.edu ");

        ArgumentCaptor<String> emailCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> otpCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(emailService).sendOtp(
                emailCaptor.capture(),
                otpCaptor.capture()
        );

        assertEquals("student@lion.lmu.edu", emailCaptor.getValue());
        assertTrue(otpCaptor.getValue().matches("\\d{6}"));
    }

    @Test
    void userCanRequestAnotherOtp() {
        EmailService emailService = mock(EmailService.class);
        OtpService otpService = new OtpService(emailService);

        otpService.requestOtp("student@lmu.edu");
        otpService.requestOtp("student@lmu.edu");

        verify(emailService, times(2))
                .sendOtp(eq("student@lmu.edu"), anyString());
    }
}