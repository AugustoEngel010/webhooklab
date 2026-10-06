package dev.augusto.webhooklab;

import dev.augusto.webhooklab.application.port.in.DeliverEventUseCase;
import dev.augusto.webhooklab.application.port.out.DeliverySender;
import dev.augusto.webhooklab.application.port.out.DeliveryAttemptRepository;
import dev.augusto.webhooklab.application.port.out.DeliveryAttemptRepository.DeliveryClaim;
import dev.augusto.webhooklab.application.usecase.DeliveryConflictException;
import dev.augusto.webhooklab.application.usecase.DeliverEventService;
import dev.augusto.webhooklab.adapters.in.web.DeliveryController;
import dev.augusto.webhooklab.domain.EventStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(DeliveryAttemptIT.FakeDeliveryConfig.class)
class DeliveryAttemptIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DeliverEventUseCase deliver;
    @Autowired FakeSender sender;
    @Autowired MockMvc mockMvc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM delivery_attempts");
        jdbc.update("DELETE FROM events");
        sender.reset();
    }

    @Test
    void pendingEventBecomesSendingAndGetsStartedAttempt() {
        UUID eventId = insertEvent(EventStatus.PENDING);

        DeliveryClaim claim = deliver.deliver(eventId);

        assertEquals(EventStatus.DELIVERED, claim.event().status());
        assertEquals("SUCCEEDED", jdbc.queryForObject("SELECT outcome FROM delivery_attempts WHERE event_id = ?", String.class, eventId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM delivery_attempts WHERE event_id = ?", Integer.class, eventId));
        assertEquals(1, sender.calls.get());
        assertNotNull(jdbc.queryForObject("SELECT started_at FROM delivery_attempts WHERE event_id = ?", Instant.class, eventId));
    }

    @Test
    void concurrentExecutionsPersistOneAttemptAndSendOnce() throws Exception {
        UUID eventId = insertEvent(EventStatus.PENDING);
        CyclicBarrier ready = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Object> task = () -> {
            ready.await();
            try { return deliver.deliver(eventId); }
            catch (RuntimeException e) { return e; }
        };

        Future<Object> first = pool.submit(task);
        Future<Object> second = pool.submit(task);
        Object result1 = first.get(10, TimeUnit.SECONDS);
        Object result2 = second.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(1, java.util.stream.Stream.of(result1, result2).filter(DeliveryClaim.class::isInstance).count());
        assertEquals(1, java.util.stream.Stream.of(result1, result2).filter(DeliveryConflictException.class::isInstance).count());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM delivery_attempts WHERE event_id = ?", Integer.class, eventId));
        assertEquals(1, sender.calls.get());
    }

    @Test
    void secondDeliveryIsHttp409WithoutAnotherSend() throws Exception {
        UUID eventId = insertEvent(EventStatus.PENDING);
        deliver.deliver(eventId);

        mockMvc.perform(post("/events/{id}/deliver", eventId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DELIVERY_CONFLICT"));
        assertEquals(1, sender.calls.get());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM delivery_attempts WHERE event_id = ?", Integer.class, eventId));
    }

    @Test
    void sendingDeliveredAndFailedEventsAreRejected() {
        for (EventStatus status : new EventStatus[]{EventStatus.SENDING, EventStatus.DELIVERED, EventStatus.FAILED}) {
            UUID eventId = insertEvent(status);
            assertThrows(DeliveryConflictException.class, () -> deliver.deliver(eventId));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM delivery_attempts WHERE event_id = ?", Integer.class, eventId));
        }
        assertEquals(0, sender.calls.get());
    }

    @Test
    void failedAttemptInsertRollsBackEventClaim() {
        UUID eventId = insertEvent(EventStatus.PENDING);
        jdbc.update("INSERT INTO delivery_attempts (id, event_id, started_at, outcome) VALUES (?, ?, ?, 'STARTED')",
                UUID.randomUUID(), eventId, Timestamp.from(Instant.now()));

        assertThrows(RuntimeException.class, () -> deliver.deliver(eventId));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM events WHERE id = ?", String.class, eventId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM delivery_attempts WHERE event_id = ?", Integer.class, eventId));
        assertEquals(0, sender.calls.get());
    }

    @Test
    void senderRunsAfterDatabaseTransactionCommits() {
        UUID eventId = insertEvent(EventStatus.PENDING);

        deliver.deliver(eventId);

        assertFalse(sender.transactionWasActive);
    }

    @Test
    void unknownSenderResultLeavesSendingAndStarted() {
        UUID eventId = insertEvent(EventStatus.PENDING);
        sender.failOnSend = true;

        assertThrows(RuntimeException.class, () -> deliver.deliver(eventId));
        assertEquals("SENDING", jdbc.queryForObject("SELECT status FROM events WHERE id = ?", String.class, eventId));
        assertEquals("STARTED", jdbc.queryForObject("SELECT outcome FROM delivery_attempts WHERE event_id = ?", String.class, eventId));
    }

    private UUID insertEvent(EventStatus status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, 'TEST', '{}'::jsonb, ?, ?)",
                id, status.name(), Timestamp.from(Instant.now()));
        return id;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeDeliveryConfig {
        @Bean @Primary FakeSender deliverySender() { return new FakeSender(); }
        @Bean @Primary DeliverEventUseCase deliverEventUseCase(DeliveryAttemptRepository attempts, FakeSender sender, java.time.Clock clock) {
            return new DeliverEventService(attempts, sender, clock);
        }
        @Bean DeliveryController deliveryController(DeliverEventUseCase deliver) { return new DeliveryController(deliver); }
    }

    static class FakeSender implements DeliverySender {
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean transactionWasActive;
        volatile boolean failOnSend;

        @Override
        public DeliverySender.DeliveryResult send(dev.augusto.webhooklab.domain.Event event, dev.augusto.webhooklab.domain.DeliveryAttempt attempt) {
            calls.incrementAndGet();
            transactionWasActive |= TransactionSynchronizationManager.isActualTransactionActive();
            if (failOnSend) throw new RuntimeException("unknown delivery result");
            return new DeliverySender.DeliveryResult(dev.augusto.webhooklab.domain.DeliveryAttemptStatus.SUCCEEDED, 200);
        }

        void reset() { calls.set(0); transactionWasActive = false; failOnSend = false; }
    }
}
