package dk.ek.smartprojectplanner.config;

import dk.ek.smartprojectplanner.controller.HomeController;
import dk.ek.smartprojectplanner.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests the login rule through a protected page (/): logged out → login page, logged in → the page. */
@WebMvcTest(HomeController.class)
class LoginInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    void protectedPage_notLoggedIn_redirectsToLogin() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void protectedPage_loggedIn_isShown() throws Exception {
        // Arrange
        when(healthService.databaseStatus()).thenReturn("OK");

        // Act + Assert
        mockMvc.perform(get("/").sessionAttr("userId", 1).sessionAttr("userName", "Test"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    /**
     * Potential ideas for additional tests:
     * static files: GET /css/style.css is not redirected when logged out (the /css/** exclusion)
     * unknown paths: a page that doesn't exist also redirects to /login when logged out
     * session: a session without "userId" is treated as logged out
     */
}