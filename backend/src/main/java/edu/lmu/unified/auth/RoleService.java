
package edu.lmu.unified.auth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    private final Set<String> adminEmails;

    public RoleService(
            @Value("${lmu.admin.emails:}") String configuredEmails
    ) {
        this.adminEmails = Arrays.stream(
                        configuredEmails.split(",")
                )
                .map(email -> email.trim().toLowerCase())
                .filter(email -> !email.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public UserRole getRole(String email) {
        if (email == null || email.isBlank()) {
            return UserRole.STUDENT;
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (adminEmails.contains(normalizedEmail)) {
            return UserRole.ADMIN;
        }

        return UserRole.STUDENT;
    }
}
