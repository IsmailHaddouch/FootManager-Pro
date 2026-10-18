-- FootManager Pro - schéma initial (socle)
-- Les autres entités du cahier des charges (Club, Team, Payment, Refund, Receipt,
-- Subscription*, Promotion, Review, Notification...) seront ajoutées par des migrations V2+.

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------- Organisations
CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- Utilisateurs
-- organization_id est NULL pour le SuperAdmin et pour les clients/clubs (comptes transverses).
CREATE TABLE users (
    id               UUID PRIMARY KEY,
    organization_id  UUID REFERENCES organizations (id),
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,
    full_name        VARCHAR(150) NOT NULL,
    phone            VARCHAR(30),
    role             VARCHAR(20)  NOT NULL
                     CHECK (role IN ('SUPER_ADMIN', 'OWNER', 'STAFF', 'CLUB_MANAGER', 'CUSTOMER')),
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE INDEX idx_users_org ON users (organization_id);

-- ---------------------------------------------------------------- Plans SaaS
-- Prix, limites et fonctionnalités configurables par le SuperAdmin (jamais codés en dur).
CREATE TABLE subscription_plans (
    id             UUID PRIMARY KEY,
    code           VARCHAR(30)    NOT NULL UNIQUE,
    name           VARCHAR(80)    NOT NULL,
    price_monthly  NUMERIC(10, 2) NOT NULL CHECK (price_monthly >= 0),
    price_yearly   NUMERIC(10, 2) NOT NULL CHECK (price_yearly >= 0),
    currency       CHAR(3)        NOT NULL DEFAULT 'MAD',
    max_centers    INT,
    max_pitches    INT,
    max_staff      INT,
    features       JSONB          NOT NULL DEFAULT '{}'::jsonb,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id                UUID PRIMARY KEY,
    organization_id   UUID        NOT NULL REFERENCES organizations (id),
    plan_id           UUID        NOT NULL REFERENCES subscription_plans (id),
    status            VARCHAR(20) NOT NULL
                      CHECK (status IN ('TRIAL', 'ACTIVE', 'GRACE', 'PAST_DUE', 'CANCELED', 'EXPIRED')),
    billing_cycle     VARCHAR(10) NOT NULL DEFAULT 'MONTHLY' CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    renews_at         TIMESTAMPTZ,
    trial_ends_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_subscriptions_org ON subscriptions (organization_id);

-- ---------------------------------------------------------------- Centres et terrains
CREATE TABLE football_centers (
    id               UUID PRIMARY KEY,
    organization_id  UUID         NOT NULL REFERENCES organizations (id),
    name             VARCHAR(150) NOT NULL,
    slug             VARCHAR(80)  NOT NULL UNIQUE,
    city             VARCHAR(80),
    address          VARCHAR(255),
    phone            VARCHAR(30),
    whatsapp         VARCHAR(30),
    logo_url         VARCHAR(500),
    public_page_enabled BOOLEAN   NOT NULL DEFAULT FALSE,
    auto_confirm     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_centers_org ON football_centers (organization_id);

CREATE TABLE pitches (
    id               UUID PRIMARY KEY,
    organization_id  UUID         NOT NULL REFERENCES organizations (id),
    center_id        UUID         NOT NULL REFERENCES football_centers (id),
    name             VARCHAR(100) NOT NULL,
    format           VARCHAR(20)  NOT NULL DEFAULT '5v5',
    indoor           BOOLEAN      NOT NULL DEFAULT FALSE,
    surface          VARCHAR(40)  NOT NULL DEFAULT 'ARTIFICIAL_GRASS',
    capacity         INT,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (center_id, name)
);
CREATE INDEX idx_pitches_org ON pitches (organization_id);

CREATE TABLE opening_hours (
    id               UUID PRIMARY KEY,
    organization_id  UUID     NOT NULL REFERENCES organizations (id),
    center_id        UUID     NOT NULL REFERENCES football_centers (id),
    day_of_week      SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at         TIME,
    closes_at        TIME,
    closed           BOOLEAN  NOT NULL DEFAULT FALSE,
    UNIQUE (center_id, day_of_week)
);

CREATE TABLE blocked_time_slots (
    id               UUID PRIMARY KEY,
    organization_id  UUID        NOT NULL REFERENCES organizations (id),
    pitch_id         UUID        NOT NULL REFERENCES pitches (id),
    start_at         TIMESTAMPTZ NOT NULL,
    end_at           TIMESTAMPTZ NOT NULL,
    reason           VARCHAR(200),
    CHECK (end_at > start_at)
);

CREATE TABLE pricing_rules (
    id               UUID PRIMARY KEY,
    organization_id  UUID           NOT NULL REFERENCES organizations (id),
    pitch_id         UUID           NOT NULL REFERENCES pitches (id),
    label            VARCHAR(80)    NOT NULL,
    day_type         VARCHAR(10)    NOT NULL DEFAULT 'ALL' CHECK (day_type IN ('ALL', 'WEEKDAY', 'WEEKEND')),
    start_time       TIME           NOT NULL,
    end_time         TIME           NOT NULL,
    price_per_hour   NUMERIC(10, 2) NOT NULL CHECK (price_per_hour >= 0),
    priority         INT            NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------- Clients
CREATE TABLE customers (
    id               UUID PRIMARY KEY,
    organization_id  UUID         NOT NULL REFERENCES organizations (id),
    user_id          UUID REFERENCES users (id),
    full_name        VARCHAR(150) NOT NULL,
    phone            VARCHAR(30),
    email            VARCHAR(255),
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_customers_org ON customers (organization_id);

-- ---------------------------------------------------------------- Réservations
CREATE TABLE reservations (
    id               UUID PRIMARY KEY,
    organization_id  UUID           NOT NULL REFERENCES organizations (id),
    pitch_id         UUID           NOT NULL REFERENCES pitches (id),
    customer_id      UUID REFERENCES customers (id),
    reference        VARCHAR(20)    NOT NULL UNIQUE,
    start_at         TIMESTAMPTZ    NOT NULL,
    end_at           TIMESTAMPTZ    NOT NULL,
    status           VARCHAR(30)    NOT NULL
                     CHECK (status IN ('PENDING', 'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED',
                                       'CANCELED', 'REJECTED', 'NO_SHOW', 'REFUND_PENDING', 'REFUNDED')),
    source           VARCHAR(10)    NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'ONLINE')),
    total_price      NUMERIC(10, 2) NOT NULL CHECK (total_price >= 0),
    hold_expires_at  TIMESTAMPTZ,
    created_by       UUID REFERENCES users (id),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CHECK (end_at > start_at),

    -- Garantie ultime anti double réservation : deux réservations "actives" ne peuvent pas
    -- se chevaucher sur le même terrain, même en cas de requêtes simultanées.
    CONSTRAINT no_overlapping_reservations
        EXCLUDE USING gist (pitch_id WITH =, tstzrange(start_at, end_at) WITH &&)
        WHERE (status IN ('PENDING', 'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED'))
);
CREATE INDEX idx_reservations_org_start ON reservations (organization_id, start_at);

-- ---------------------------------------------------------------- Journal d'audit
CREATE TABLE audit_logs (
    id               UUID PRIMARY KEY,
    organization_id  UUID,
    actor_user_id    UUID,
    entity_type      VARCHAR(60)  NOT NULL,
    entity_id        UUID,
    action           VARCHAR(40)  NOT NULL,
    details          JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_org ON audit_logs (organization_id, created_at);

-- ---------------------------------------------------------------- Plans par défaut
-- Valeurs initiales modifiables ensuite par le SuperAdmin (les tarifs ne sont pas codés en dur).
INSERT INTO subscription_plans (id, code, name, price_monthly, price_yearly, max_centers, max_pitches, max_staff, features)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'NORMAL', 'Normal', 0, 0, 1, 3, 1,
   '{"online_booking": false, "public_page": false, "promo_codes": false, "advanced_reports": false}'),
  ('00000000-0000-0000-0000-000000000002', 'PRO', 'PRO', 0, 0, NULL, NULL, NULL,
   '{"online_booking": true, "public_page": true, "promo_codes": true, "advanced_reports": true}');
