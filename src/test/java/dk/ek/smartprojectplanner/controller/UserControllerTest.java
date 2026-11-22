package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.exception.InvalidLoginException;
import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.model.User;
import dk.ek.smartprojectplanner.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Web tests for UserController: requests through MockMvc, UserService is mocked. */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ---------- login ----------

    @Test
    void showLoginPage_returnsLoginView() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void login_correctCredentials_storesUserInSessionAndRedirectsToFrontPage() throws Exception {
        // Arrange
        User user = new User(1, "Test Manager", "manager@test.dk", "hash", Role.MANAGER);
        when(userService.login("manager@test.dk", "password123")).thenReturn(user);

        // Act + Assert
        mockMvc.perform(post("/login")
                        .param("email", "manager@test.dk")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute("userId", 1));
    }

    @Test
    void login_wrongCredentials_showsLoginPageWithError() throws Exception {
        // Arrange
        when(userService.login("manager@test.dk", "forkert1"))
                .thenThrow(new InvalidLoginException("Forkert e-mail eller adgangskode"));

        // Act + Assert
        mockMvc.perform(post("/login")
                        .param("email", "manager@test.dk")
                        .param("password", "forkert1"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Forkert e-mail eller adgangskode"));
    }

    // ---------- register ----------

    @Test
    void register_invalidInput_showsFormAgainWithErrorAndTypedValues() throws Exception {
        // Arrange
        doThrow(new InvalidInputException("Adgangskoden skal være mindst 8 tegn."))
                .when(userService).register("Anna", "anna@mail.dk", "kort");

        // Act + Assert
        mockMvc.perform(post("/register")
                        .param("name", "Anna")
                        .param("email", "anna@mail.dk")
                        .param("password", "kort"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Adgangskoden skal være mindst 8 tegn."))
                .andExpect(model().attribute("email", "anna@mail.dk"));
    }

    /**
     * Potential ideas for additional tests:
     * register success: redirects to /login with a flash message (flash().attributeExists("flash"))
     * pages: GET /register shows the register view
     * logout: POST /logout invalidates the session and redirects to /login
     * session: after a correct login, "userName" is stored in the session too
     * rendering: the error message is actually shown in the HTML (content().string(containsString(...)))
     */
}