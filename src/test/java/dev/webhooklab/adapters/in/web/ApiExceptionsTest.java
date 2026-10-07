package dev.webhooklab.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.webhooklab.application.port.in.GetEventUseCase;
import dev.webhooklab.application.port.in.ListEventsUseCase;
import dev.webhooklab.application.port.in.RegisterEventUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class ApiExceptionsTest {
  @Test
  void persistenceFailureUsesPublic503ContractWithoutTechnicalDetail() throws Exception {
    GetEventUseCase get =
        id -> {
          throw new DataAccessResourceFailureException("jdbc password must not leak");
        };
    EventController controller =
        new EventController(
            (RegisterEventUseCase) (type, payload) -> null,
            get,
            (ListEventsUseCase) (page, size) -> null,
            new JsonMapper());
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
