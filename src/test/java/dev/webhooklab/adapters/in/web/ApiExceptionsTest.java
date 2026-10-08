package dev.webhooklab.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.webhooklab.application.port.in.EventPage;
import dev.webhooklab.application.port.out.EventRepository;
import dev.webhooklab.application.usecase.EventService;
import dev.webhooklab.domain.Event;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class ApiExceptionsTest {
  @Test
  void persistenceFailureUsesPublic503ContractWithoutTechnicalDetail() throws Exception {
    EventRepository repository =
        new EventRepository() {
          public Event save(Event event) {
            return event;
          }

          public Optional<EventService.EventHistory> findById(UUID id) {
            throw new DataAccessResourceFailureException("jdbc password must not leak");
          }

          public EventPage findPage(int page, int size) {
            return null;
          }
        };
    EventService events = new EventService(repository, payload -> true, Clock.systemUTC());
    EventController controller = new EventController(events, new JsonMapper());
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiExceptions())
            .build();

    String body =
        mvc.perform(get("/events/{id}", UUID.randomUUID()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.code").value("PERSISTENCE_FAILURE"))
            .andExpect(jsonPath("$.detail").value("Event history could not be read"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(body.contains("jdbc password"));
  }
}
