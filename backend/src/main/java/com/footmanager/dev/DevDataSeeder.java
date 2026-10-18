package com.footmanager.dev;

import com.footmanager.user.Role;
import com.footmanager.user.User;
import com.footmanager.user.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Comptes de démonstration, actifs UNIQUEMENT avec le profil Spring "dev".
 * Ne jamais activer ce profil en production.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public DevDataSeeder(UserRepository users, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.users = users;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmailIgnoreCase("superadmin@footmanager.test")) {
            return;
        }
        UUID orgId = UUID.randomUUID();
        jdbc.update("INSERT INTO organizations (id, name, status) VALUES (?, ?, 'ACTIVE')", orgId, "Casablanca Foot");

        UUID proPlan = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.update("""
                INSERT INTO subscriptions (id, organization_id, plan_id, status, billing_cycle)
                VALUES (?, ?, ?, 'ACTIVE', 'MONTHLY')""", UUID.randomUUID(), orgId, proPlan);

        seed(null, "superadmin@footmanager.test", "Super Admin", Role.SUPER_ADMIN);
        seed(orgId, "owner@footmanager.test", "Propriétaire Démo", Role.OWNER);
        seed(orgId, "staff@footmanager.test", "Réceptionniste Démo", Role.STAFF);
        seed(null, "club@footmanager.test", "Gestionnaire Club Démo", Role.CLUB_MANAGER);
        seed(null, "client@footmanager.test", "Client Démo", Role.CUSTOMER);

        log.warn("Comptes de démonstration créés (profil dev) - mot de passe : {}", DEMO_PASSWORD);
    }

    private void seed(UUID orgId, String email, String name, Role role) {
        User user = new User(orgId, email, encoder.encode(DEMO_PASSWORD), name, role);
        user.setEmailVerified(true);
        users.save(user);
    }
}
