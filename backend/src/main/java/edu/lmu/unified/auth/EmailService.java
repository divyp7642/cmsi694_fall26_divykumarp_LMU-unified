package edu.lmu.unified.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your LMU Unified verification code");
        message.setText(
                "Your LMU Unified verification code is: " + otp
                        + "\n\nThis code expires in 5 minutes."
        );

        mailSender.send(message);
    }
}