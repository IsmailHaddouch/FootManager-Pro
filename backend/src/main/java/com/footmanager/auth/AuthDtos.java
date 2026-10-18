package com.footmanager.auth;

import com.footmanager.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    /** Inscription d'un client ou d'un gestionnaire de club (les propriétaires passent par l'intégration). */
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 150) String fullName,
            Boolean clubManager) {
    }

    public record UserView(UUID id, UUID organizationId, String email, String fullName, Role role) {
    }

    public record AuthResponse(String accessToken, long expiresInSeconds, UserView user) {
    }
}
