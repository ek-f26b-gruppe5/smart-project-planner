package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    HealthService healthService;

    @Test
    void index_showsDbStatus() throws Exception {
        when(healthService.databaseStatus()).thenReturn("OK");

        mockMvc.perform(get("/").sessionAttr("userId", 1).sessionAttr("userName", "Test"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("dbStatus", "OK"));

        verify(healthService).databaseStatus();
    }
}