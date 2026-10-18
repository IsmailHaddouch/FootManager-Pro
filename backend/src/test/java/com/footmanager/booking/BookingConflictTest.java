package com.footmanager.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Critère d'acceptation : deux réservations simultanées sur le même créneau -> une seule est acceptée.
 * Nécessite Docker (Testcontainers démarre un vrai PostgreSQL).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BookingConflictTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbc;

    UUID orgId;
    UUID pitchId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM reservations");
        jdbc.update("DELETE FROM pitches");
        jdbc.update("DELETE FROM football_centers");
        jdbc.update("DELETE FROM organizations");

        orgId = UUID.randomUUID();
        UUID centerId = UUID.randomUUID();
        pitchId = UUID.randomUUID();
        jdbc.update("INSERT INTO organizations (id, name, status) VALUES (?, 'Test', 'ACTIVE')", orgId);
        jdbc.update("INSERT INTO football_centers (id, organization_id, name, slug) VALUES (?, ?, 'Centre', ?)",
                centerId, orgId, "centre-" + centerId);
        jdbc.update("INSERT INTO pitches (id, organization_id, center_id, name) VALUES (?, ?, ?, 'Terrain 1')",
                pitchId, orgId, centerId);
    }

    private void insert(Instant start, Instant end, String status) {
        jdbc.update("""
                INSERT INTO reservations (id, organization_id, pitch_id, reference, start_at, end_at, status, total_price)
                VALUES (?, ?, ?, ?, ?, ?, ?, 100)""",
                UUID.randomUUID(), orgId, pitchId, "R-" + UUID.randomUUID().toString().substring(0, 8),
                Timestamp.from(start), Timestamp.from(end), status);
    }

    @Test
    void overlappingReservationOnSamePitchIsRejected() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        insert(start, start.plus(60, ChronoUnit.MINUTES), "CONFIRMED");

        assertThatThrownBy(() -> insert(start.plus(30, ChronoUnit.MINUTES), start.plus(90, ChronoUnit.MINUTES), "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void adjacentReservationsAreAllowed() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        insert(start, start.plus(60, ChronoUnit.MINUTES), "CONFIRMED");
        insert(start.plus(60, ChronoUnit.MINUTES), start.plus(120, ChronoUnit.MINUTES), "CONFIRMED");

        Integer count = jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void canceledReservationFreesTheSlot() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        insert(start, start.plus(60, ChronoUnit.MINUTES), "CANCELED");
        insert(start, start.plus(60, ChronoUnit.MINUTES), "CONFIRMED");

        Integer count = jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void concurrentRequestsOnSameSlotOnlyOneSucceeds() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                try {
                    insert(start, start.plus(60, ChronoUnit.MINUTES), "CONFIRMED");
                    success.incrementAndGet();
                } catch (DataIntegrityViolationException expected) {
                    // créneau déjà pris
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(success.get()).isEqualTo(1);
    }
}
