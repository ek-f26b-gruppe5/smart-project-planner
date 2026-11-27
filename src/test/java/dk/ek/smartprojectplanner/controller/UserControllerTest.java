package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.exception.ForbiddenException;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
                .andExpect(request().sessionAttribute("userId", 1))
                .andExpect(request().sessionAttribute("userRole", Role.MANAGER));
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

    // ---------- create employee: who may see and use the form ----------

    /**
     * Not logged in → redirected to /login; the form is not a public sign-up page.
     */
    @Test
    void showEmployeeForm_notLoggedIn_redirectsToLogin() throws Exception {
        // Arrange: no session

        // Act + Assert
        mockMvc.perform(get(UserController.NEW_EMPLOYEE_PATH))
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(userService);
    }

    /**
     * Logged in as an employee → the 403 page instead of the form.
     */
    @Test
    void showEmployeeForm_employee_shows403Page() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Du har ikke adgang til denne side."))
                .when(userService).requireManager(Role.EMPLOYEE);

        // Act + Assert
        mockMvc.perform(get(UserController.NEW_EMPLOYEE_PATH)
                        .sessionAttr("userId", 2)
                        .sessionAttr("userRole", Role.EMPLOYEE))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    /**
     * Logged in as a manager → the empty employee form.
     */
    @Test
    void showEmployeeForm_manager_showsForm() throws Exception {
        // Arrange: requireManager does nothing for a manager (the mock's default)

        // Act + Assert
        mockMvc.perform(get(UserController.NEW_EMPLOYEE_PATH)
                        .sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER))
                .andExpect(status().isOk())
                .andExpect(view().name(UserController.EMPLOYEE_FORM_VIEW))
                // the rendered form must post to the controller's path (US5: IntelliJ once rewrote it to /templates/employees)
                .andExpect(content().string(containsString("action=\"" + UserController.CREATE_EMPLOYEE_PATH + "\"")))
                // the sidebar shows managers the "Medarbejdere" link
                .andExpect(content().string(containsString("href=\"" + UserController.NEW_EMPLOYEE_PATH + "\"")));
        verify(userService).requireManager(Role.MANAGER);
    }

    // ---------- create employee: submitting the form ----------

    /**
     * A manager submits valid input → the service gets the session's role and the fields, then redirect to an empty form with a confirmation.
     */
    @Test
    void createEmployee_validInput_redirectsToEmptyFormWithFlash() throws Exception {
        // Arrange: createEmployee does nothing (the mock's default), so the input counts as valid

        // Act + Assert
        mockMvc.perform(post(UserController.CREATE_EMPLOYEE_PATH)
                        .sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER)
                        .param("name", "Anna")
                        .param("email", "anna@mail.dk")
                        .param("password", "hunter22"))
                .andExpect(redirectedUrl(UserController.NEW_EMPLOYEE_PATH))
                .andExpect(flash().attribute(UserController.FLASH, UserController.EMPLOYEE_CREATED));
        verify(userService).createEmployee(Role.MANAGER, "Anna", "anna@mail.dk", "hunter22");
    }

    /**
     * Invalid input → the form is shown again with the error and the typed name and e-mail.
     */
    @Test
    void createEmployee_invalidInput_showsFormAgainWithErrorAndTypedValues() throws Exception {
        // Arrange
        doThrow(new InvalidInputException("Adgangskoden skal være mindst 8 tegn."))
                .when(userService).createEmployee(Role.MANAGER, "Anna", "anna@mail.dk", "kort");

        // Act + Assert
        mockMvc.perform(post(UserController.CREATE_EMPLOYEE_PATH)
                        .sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER)
                        .param("name", "Anna")
                        .param("email", "anna@mail.dk")
                        .param("password", "kort"))
                .andExpect(status().isOk())
                .andExpect(view().name(UserController.EMPLOYEE_FORM_VIEW))
                .andExpect(model().attribute("error", "Adgangskoden skal være mindst 8 tegn."))
                .andExpect(model().attribute("name", "Anna"))
                .andExpect(model().attribute("email", "anna@mail.dk"));
    }

    /**
     * An employee submits the form anyway (e.g. by hand) → the 403 page.
     */
    @Test
    void createEmployee_employee_shows403Page() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Du har ikke adgang til denne side."))
                .when(userService).createEmployee(Role.EMPLOYEE, "Anna", "anna@mail.dk", "hunter22");

        // Act + Assert
        mockMvc.perform(post(UserController.CREATE_EMPLOYEE_PATH)
                        .sessionAttr("userId", 2)
                        .sessionAttr("userRole", Role.EMPLOYEE)
                        .param("name", "Anna")
                        .param("email", "anna@mail.dk")
                        .param("password", "hunter22"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    /*
     * Potential ideas for additional tests:
     * logout: POST /logout invalidates the session and redirects to /login
     * session: after a correct login, "userName" is stored in the session too
     * rendering: the error message is actually shown in the HTML (content().string(containsString(...)))
     */
}