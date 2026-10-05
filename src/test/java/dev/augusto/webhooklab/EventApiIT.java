package dev.augusto.webhooklab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class EventApiIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanEvents() {
        jdbc.update("DELETE FROM events");
    }

    @Test
    void createsEventPersistsPayloadAndReturnsPendingEvent() throws Exception {
        var result = mockMvc.perform(post("/events").contentType("application/json")
                        .content("{\"eventType\":\"ORDER_APPROVED\",\"payload\":{\"orderId\":\"DEMO-001\",\"amount\":150.00}}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/events/[0-9a-f-]+")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payload.orderId").value("DEMO-001"))
                .andReturn();

        String id = new tools.jackson.databind.json.JsonMapper().readTree(result.getResponse().getContentAsString()).get("id").asText();
        Integer rows = jdbc.queryForObject("SELECT count(*) FROM events WHERE id = ? AND payload->>'orderId' = 'DEMO-001'", Integer.class, java.util.UUID.fromString(id));
        org.junit.jupiter.api.Assertions.assertEquals(1, rows);
    }

    @Test
    void getsExistingEventById() throws Exception {
        var created = mockMvc.perform(post("/events").contentType("application/json")
                        .content("{\"eventType\":\"INVOICE_CREATED\",\"payload\":{\"invoiceId\":\"INV-1\"}}"))
                .andExpect(status().isCreated()).andReturn();
        String id = new tools.jackson.databind.json.JsonMapper().readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.eventType").value("INVOICE_CREATED"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempt").doesNotExist())
                .andExpect(jsonPath("$.payload.invoiceId").value("INV-1"));
    }

    @Test
    void rejectsInvalidInputWithoutPersisting() throws Exception {
        int before = jdbc.queryForObject("SELECT count(*) FROM events", Integer.class);
        mockMvc.perform(post("/events").contentType("application/json").content("{\"eventType\":\"   \",\"payload\":{}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/events").contentType("application/json").content("{\"eventType\":\"TYPE\",\"payload\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/events").contentType("application/json").content("{\"eventType\":\"TYPE\",\"payload\":[]}"))
                .andExpect(status().isBadRequest());
        String tooLong = "A".repeat(81);
        mockMvc.perform(post("/events").contentType("application/json").content("{\"eventType\":\"" + tooLong + "\",\"payload\":{}}"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM events", Integer.class));
    }

    @Test
    void rejectsMalformedUuidAndUnknownEvent() throws Exception {
        mockMvc.perform(get("/events/not-a-uuid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/events/{id}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    void listsWithDefaultPageAndSize() throws Exception {
        for (int i = 0; i < 21; i++) {
            createEvent("TYPE-" + i);
        }

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listsDistinctPagesInStableOrder() throws Exception {
        var older = java.time.Instant.parse("2026-01-01T00:00:00Z");
        var newestA = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        var newestB = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003");
        insertEvent(older, java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "OLDER");
        var newest = java.time.Instant.parse("2026-01-02T00:00:00Z");
        insertEvent(newest, newestA, "NEWEST-A");
        insertEvent(newest, newestB, "NEWEST-B");

        mockMvc.perform(get("/events").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(newestB.toString()))
                .andExpect(jsonPath("$.content[1].id").value(newestA.toString()));
        mockMvc.perform(get("/events").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void returnsEmptyPageAndAcceptsMaximumSize() throws Exception {
        mockMvc.perform(get("/events").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalPages").value(0));
        createEvent("ONLY");
        mockMvc.perform(get("/events").param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        for (var params : java.util.List.of(
                new String[]{"page", "-1"}, new String[]{"size", "0"},
                new String[]{"size", "-1"}, new String[]{"size", "101"},
                new String[]{"page", "abc"}, new String[]{"size", "abc"})) {
            mockMvc.perform(get("/events").param(params[0], params[1]))
                    .andExpect(status().isBadRequest());
        }
    }

    private void createEvent(String type) throws Exception {
        mockMvc.perform(post("/events").contentType("application/json")
                .content("{\"eventType\":\"" + type + "\",\"payload\":{}}"))
                .andExpect(status().isCreated());
    }

    private void insertEvent(java.time.Instant createdAt, java.util.UUID id, String type) {
        jdbc.update("INSERT INTO events (id, event_type, payload, status, created_at) VALUES (?, ?, '{}'::jsonb, 'PENDING', ?)",
                id, type, java.sql.Timestamp.from(createdAt));
    }
}
