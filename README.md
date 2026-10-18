# FootManager Pro

Plateforme SaaS multi-tenant de gestion de terrains de mini-foot.
Stack : **React + Vite + TypeScript + Tailwind** (frontend) · **Spring Boot 3 / Java 21 / Maven** (backend) · **PostgreSQL 16**.

```
footmanager/
├── backend/             API REST Spring Boot (Maven)
├── frontend/            Application React (Vite)
├── docker-compose.yml   PostgreSQL local
└── .env.example         Variables d'environnement
```

## Prérequis

| Outil | Version |
|---|---|
| JDK | 21 |
| Maven | 3.9+ (ou utiliser un wrapper : `mvn -N wrapper:wrapper`) |
| Node.js | 20+ |
| Docker | pour PostgreSQL et les tests (Testcontainers) |

## Installation

### 1. Variables d'environnement

```bash
cp .env.example .env
```

Le backend lit ses variables dans l'environnement (valeurs par défaut adaptées au développement local).
Pour les charger depuis `.env` : `export $(grep -v '^#' .env | xargs)` ou configurez-les dans votre IDE.

### 2. Base de données

```bash
docker compose up -d
```

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

- API : http://localhost:8080
- Santé : http://localhost:8080/actuator/health
- Les migrations Flyway (`src/main/resources/db/migration`) s'exécutent au démarrage.
- Profil `dev` (par défaut dans `.env.example`) : crée les comptes de démonstration.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Application : http://localhost:5173 (les appels `/api` sont proxifiés vers le backend).

## Comptes de démonstration (profil `dev` uniquement)

Mot de passe commun : `Password123!`

| Rôle | E-mail |
|---|---|
| SuperAdmin | superadmin@footmanager.test |
| Propriétaire | owner@footmanager.test |
| Personnel | staff@footmanager.test |
| Gestionnaire de club | club@footmanager.test |
| Client | client@footmanager.test |

> N'activez **jamais** `SPRING_PROFILES_ACTIVE=dev` en production.

## Tests

```bash
cd backend
mvn test
```

`BookingConflictTest` démarre un vrai PostgreSQL (Docker requis) et vérifie le critère d'acceptation
« deux réservations simultanées sur le même créneau : une seule est acceptée » grâce à la contrainte
d'exclusion `no_overlapping_reservations`.

## Ce qui est déjà en place

- Schéma initial Flyway : organisations, utilisateurs, plans, abonnements, centres, terrains,
  horaires, créneaux bloqués, règles de prix, clients, réservations (avec contrainte anti-chevauchement),
  journal d'audit.
- Authentification JWT (connexion, inscription client/club, `/api/me`), mots de passe BCrypt.
- RBAC côté serveur (`@PreAuthorize`) : exemple `GET /api/admin/ping` réservé au SuperAdmin.
- Frontend : connexion (React Hook Form + Zod), routes protégées, layout avec identité visuelle football.

## Prochaines étapes (plan de développement du cahier des charges)

1. Isolation multi-tenant (filtre Hibernate sur `organization_id`) et inscription des centres.
2. Gestion des terrains et de la tarification.
3. Calendrier et réservations manuelles (service de réservation + audit).
4. Droits Normal/PRO appliqués côté serveur.
5. Pages publiques, clubs, paiements (webhooks idempotents), tableaux de bord, notifications.

## Déploiement (aperçu)

- Backend : `mvn -DskipTests package` → `target/footmanager-backend-0.1.0.jar`, à lancer avec
  `java -jar` (variables d'environnement de production, profil **sans** `dev`).
- Frontend : `npm run build` → dossier `dist/` à servir en statique (Nginx, Vercel, Netlify…),
  avec un proxy `/api` vers le backend.
- Base : PostgreSQL managé avec sauvegardes automatiques.
