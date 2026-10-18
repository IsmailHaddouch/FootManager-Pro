package com.footmanager.admin;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exemple de vérification de rôle côté serveur (RBAC) : à remplacer par les vrais endpoints
 * SuperAdmin (entreprises, plans, statistiques globales).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "scope", "super-admin");
    }
}
