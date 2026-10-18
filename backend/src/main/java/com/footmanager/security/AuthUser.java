package com.footmanager.security;

import com.footmanager.user.Role;
import java.util.UUID;

/**
 * Identité authentifiée, reconstruite à partir du jeton JWT.
 * L'organizationId sert de base à l'isolation multi-locataire : il doit être utilisé
 * dans chaque requête métier, jamais une valeur envoyée par le client.
 */
public record AuthUser(UUID id, UUID organizationId, String email, Role role) {
}
