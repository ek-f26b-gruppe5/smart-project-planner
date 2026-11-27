package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.exception.ForbiddenException;
import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.model.Project;
import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web tests for ProjectController: requests through MockMvc, ProjectService is mocked.
 * Tests that return a view render projects/list.html or projects/form.html.
 */
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    // ---------- Redirects ----------

    /**
     * Not logged in → redirected to /login, and the service is never called.
     */
    @Test
    void list_notLoggedIn_redirectsToLogin() throws Exception {
        // Arrange: no userId in the session

        // Act + Assert
        mockMvc.perform(get(ProjectController.LIST_PATH))
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(projectService);
    }

    /**
     * Valid form → the service gets the session's userId and the form values, then redirect to the list with the flash message.
     */
    @Test
    void create_validInput_callsServiceAndRedirectsToListWithFlash() throws Exception {
        // Arrange: nothing to stub; create returns 0, which the controller doesn't use

        // Act
        mockMvc.perform(post(ProjectController.CREATE_PATH).sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER)
                        .param("name", "Nyt parcelhus")
                        .param("description", "Byggeri")
                        .param("startDate", "2027-03-01")
                        .param("deadline", "2027-05-28"))
                // Assert
                .andExpect(redirectedUrl(ProjectController.LIST_PATH))
                .andExpect(flash().attribute(ProjectController.FLASH, ProjectController.CREATED));

        ArgumentCaptor<Project> form = ArgumentCaptor.forClass(Project.class);
        verify(projectService).create(eq(Role.MANAGER), eq(1), form.capture());
        assertEquals("Nyt parcelhus", form.getValue().getName());
        assertEquals("Byggeri", form.getValue().getDescription());
        assertEquals(LocalDate.of(2027, 3, 1), form.getValue().getStartDate());
        assertEquals(LocalDate.of(2027, 5, 28), form.getValue().getDeadline());
    }

    // ---------- Render a view (need the templates) ----------

    /**
     * Logged in → the list view gets the user's projects from the service.
     */
    @Test
    void list_loggedIn_putsUsersProjectsInModel() throws Exception {
        // Arrange
        List<Project> projects = List.of(new Project(5, 1, "Nyt parcelhus", null,
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 5, 28)));
        when(projectService.getProjects(1)).thenReturn(projects);

        // Act + Assert
        mockMvc.perform(get(ProjectController.LIST_PATH).sessionAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(view().name(ProjectController.LIST_VIEW))
                .andExpect(model().attribute(ProjectController.PROJECTS, projects));
    }

    /**
     * The empty form page gets an empty project to bind to.
     */
    @Test
    void showCreateForm_putsEmptyProjectInModel() throws Exception {
        // Arrange: logged in, nothing else

        // Act + Assert
        mockMvc.perform(get(ProjectController.NEW_PATH).sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER))
                .andExpect(status().isOk())
                .andExpect(view().name(ProjectController.FORM_VIEW))
                .andExpect(model().attributeExists(ProjectController.PROJECT))
                // the rendered form must post to the controller's path (the US5 lesson)
                .andExpect(content().string(containsString("action=\"" + ProjectController.CREATE_PATH + "\"")));
    }

    /**
     * Service rejects the input → form shown again with the service's message and the typed values.
     */
    @Test
    void create_ruleBroken_showsFormAgainWithMessageAndTypedValues() throws Exception {
        // Arrange
        when(projectService.create(eq(Role.MANAGER), eq(1), any()))
                .thenThrow(new InvalidInputException("Deadline kan ikke ligge før startdatoen."));

        // Act + Assert
        mockMvc.perform(post(ProjectController.CREATE_PATH).sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER)
                        .param("name", "Baglæns")
                        .param("startDate", "2027-05-28")
                        .param("deadline", "2027-03-01"))
                .andExpect(status().isOk())
                .andExpect(view().name(ProjectController.FORM_VIEW))
                .andExpect(model().attribute(ProjectController.ERROR, "Deadline kan ikke ligge før startdatoen."))
                .andExpect(model().attribute(ProjectController.PROJECT, hasProperty("name", is("Baglæns"))));
    }

    /**
     * A date that can't be read → form shown again with the date message, and nothing is created.
     */
    @Test
    void create_malformedDate_showsFormAndNeverCallsService() throws Exception {
        // Arrange: nothing to stub; the service must not be reached

        // Act + Assert
        mockMvc.perform(post(ProjectController.CREATE_PATH).sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER)
                        .param("name", "Projekt")
                        .param("startDate", "ikke-en-dato")
                        .param("deadline", "2027-05-28"))
                .andExpect(view().name(ProjectController.FORM_VIEW))
                .andExpect(model().attributeHasFieldErrors(ProjectController.PROJECT, "startDate"))
                .andExpect(model().attribute(ProjectController.ERROR, ProjectController.INVALID_DATE));
        verify(projectService, never()).create(any(), anyInt(), any());
    }

    // ---------- App shell on the list page (where users land after login) ----------

    /**
     * A manager sees "Medarbejdere" in the sidebar, linking to the employee form.
     */
    @Test
    void list_manager_sidebarShowsEmployeesLink() throws Exception {
        // Arrange: getProjects returns an empty list (the mock's default)

        // Act + Assert
        mockMvc.perform(get(ProjectController.LIST_PATH).sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER))
                .andExpect(content().string(containsString("href=\"" + UserController.NEW_EMPLOYEE_PATH + "\"")));
    }

    /**
     * An employee doesn't see the "Medarbejdere" link.
     */
    @Test
    void list_employee_sidebarHidesEmployeesLink() throws Exception {
        // Arrange: getProjects returns an empty list (the mock's default)

        // Act + Assert
        mockMvc.perform(get(ProjectController.LIST_PATH).sessionAttr("userId", 2)
                        .sessionAttr("userRole", Role.EMPLOYEE))
                .andExpect(content().string(not(containsString("href=\"" + UserController.NEW_EMPLOYEE_PATH + "\""))));
    }

    // ---------- only managers create projects ----------

    /**
     * An employee opens the create form → the 403 page.
     */
    @Test
    void showCreateForm_employee_shows403Page() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Du har ikke adgang til denne side."))
                .when(projectService).requireCanCreate(Role.EMPLOYEE);

        // Act + Assert
        mockMvc.perform(get(ProjectController.NEW_PATH).sessionAttr("userId", 2)
                        .sessionAttr("userRole", Role.EMPLOYEE))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    /**
     * An employee submits the form anyway (e.g. by hand) → the 403 page, and nothing is created.
     */
    @Test
    void create_employee_shows403PageAndCreatesNothing() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Du har ikke adgang til denne side."))
                .when(projectService).requireCanCreate(Role.EMPLOYEE);

        // Act + Assert
        mockMvc.perform(post(ProjectController.CREATE_PATH).sessionAttr("userId", 2)
                        .sessionAttr("userRole", Role.EMPLOYEE)
                        .param("name", "Projekt")
                        .param("startDate", "2027-03-01")
                        .param("deadline", "2027-05-28"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
        verify(projectService, never()).create(any(), anyInt(), any());
    }

    /**
     * A manager sees "+ Opret projekt" on the list.
     */
    @Test
    void list_manager_showsCreateButton() throws Exception {
        // Arrange: getProjects returns an empty list (the mock's default)

        // Act + Assert
        mockMvc.perform(get(ProjectController.LIST_PATH).sessionAttr("userId", 1)
                        .sessionAttr("userRole", Role.MANAGER))
                .andExpect(content().string(containsString("href=\"" + ProjectController.NEW_PATH + "\"")));
    }

    /**
     * An employee doesn't see "+ Opret projekt".
     */
    @Test
    void list_employee_hidesCreateButton() throws Exception {
        // Arrange: getProjects returns an empty list (the mock's default)

        // Act + Assert
        mockMvc.perform(get(ProjectController.LIST_PATH).sessionAttr("userId", 2)
                        .sessionAttr("userRole", Role.EMPLOYEE))
                .andExpect(content().string(not(containsString("href=\"" + ProjectController.NEW_PATH + "\""))));
    }
}