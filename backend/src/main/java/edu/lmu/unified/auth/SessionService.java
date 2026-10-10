
package edu.lmu.unified.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final Duration SESSION_DURATION =
            Duration.ofHours(8);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Create a secure session after OTP verification
    public String createSession(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        redisTemplate.opsForValue().set(
                "lmu:session:" + token,
                normalizedEmail,
                SESSION_DURATION
        );

        return token;
    }

    // Find the authenticated student for a session token
    public Optional<String> getAuthenticatedEmail(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String email = redisTemplate.opsForValue().get(
                "lmu:session:" + token
        );

        return Optional.ofNullable(email);
    }

    // Remove session on logout
    public void deleteSession(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete("lmu:session:" + token);
        }
    }
}
