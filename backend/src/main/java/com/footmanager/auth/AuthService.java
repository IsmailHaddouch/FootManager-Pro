package com.footmanager.auth;

import com.footmanager.auth.AuthDtos.AuthResponse;
import com.footmanager.auth.AuthDtos.LoginRequest;
import com.footmanager.auth.AuthDtos.RegisterRequest;
import com.footmanager.auth.AuthDtos.UserView;
import com.footmanager.security.JwtService;
import com.footmanager.user.Role;
import com.footmanager.user.User;
import com.footmanager.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    // Hash factice pour garder un temps de réponse constant quand l'e-mail est inconnu.
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa1Z1v0zYb0P3o2cJYtHnKX3Wv5y7m0G";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email()).orElse(null);
        boolean ok = encoder.matches(req.password(), user != null ? user.getPasswordHash() : DUMMY_HASH);
        if (user == null || !ok || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        return toResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet e-mail est déjà utilisé");
        }
        Role role = Boolean.TRUE.equals(req.clubManager()) ? Role.CLUB_MANAGER : Role.CUSTOMER;
        User user = users.save(new User(null, req.email(), encoder.encode(req.password()), req.fullName(), role));
        return toResponse(user);
    }

    public UserView view(User user) {
        return new UserView(user.getId(), user.getOrganizationId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwt.generate(user), jwt.expiresInSeconds(), view(user));
    }
}
