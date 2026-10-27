
package edu.lmu.unified.auth;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final SessionService sessionService;
    private final RoleService roleService;

    public AuthorizationService(
            SessionService sessionService,
            RoleService roleService
    ) {
        this.sessionService = sessionService;
        this.roleService = roleService;
    }

    public Optional<String> getAuthenticatedEmail(
            String authorization
    ) {
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return Optional.empty();
        }

        return sessionService.getAuthenticatedEmail(token);
    }

    public Optional<UserRole> getAuthenticatedRole(
            String authorization
    ) {
        return getAuthenticatedEmail(authorization)
                .map(roleService::getRole);
    }

    public boolean hasRole(
            String authorization,
            UserRole requiredRole
    ) {
        return getAuthenticatedRole(authorization)
                .map(role -> role == requiredRole)
                .orElse(false);
    }
}
