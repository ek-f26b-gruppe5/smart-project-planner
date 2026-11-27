package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Web tests for HomeController: the front page and the public health check. */
@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    HealthService healthService;

    /**
     * The front page gets the database status from the service.
     */
    @Test
    void index_showsDbStatus() throws Exception {
        // Arrange
        when(healthService.databaseStatus()).thenReturn("OK");

        // Act + Assert
        mockMvc.perform(get("/").sessionAttr("userId", 1).sessionAttr("userName", "Test"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    /**
     * The database is down → the health check answers "ERROR", still with status 200.
     */
    @Test
    void health_databaseDown_returnsError() throws Exception {
        // Arrange
        when(healthService.databaseStatus()).thenReturn("ERROR");

        // Act + Assert
        mockMvc.perform(get(HomeController.HEALTH_PATH))
                .andExpect(content().string("ERROR"));
    }

    // ---------- health check ----------

    /**
     * Not logged in, the health check still answers "OK" as plain text.
     */
    @Test
    void health_notLoggedIn_returnsStatusAsText() throws Exception {
        // Arrange: no session, the database answers
        when(healthService.databaseStatus()).thenReturn("OK");

        // Act + Assert
        mockMvc.perform(get(HomeController.HEALTH_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    /**
     * The database is down → the health check answers "ERROR", still with status 200.
     */
    @Test
    void health_databaseDown_returnsError() throws Exception {
        // Arrange
        when(healthService.databaseStatus()).thenReturn("ERROR");

        // Act + Assert
        mockMvc.perform(get(HomeController.HEALTH_PATH))
                .andExpect(content().string("ERROR"));
    }
}