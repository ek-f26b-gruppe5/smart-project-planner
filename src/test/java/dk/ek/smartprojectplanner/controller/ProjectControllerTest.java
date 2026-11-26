package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.model.Project;
import dk.ek.smartprojectplanner.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web tests for ProjectController: requests through MockMvc, ProjectService is mocked.
 * Tests that return a view render its template, so they need projects/list.html and
 * projects/form.html to exist. Tests that end in a redirect don't.
 */
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    // ---------- Work without templates (redirects) ----------

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
                        .param("name", "Nyt parcelhus")
                        .param("description", "Byggeri")
                        .param("startDate", "2027-03-01")
                        .param("deadline", "2027-05-28"))
                // Assert
                .andExpect(redirectedUrl(ProjectController.LIST_PATH))
                .andExpect(flash().attribute(ProjectController.FLASH, ProjectController.CREATED));

        ArgumentCaptor<Project> form = ArgumentCaptor.forClass(Project.class);
        verify(projectService).create(eq(1), form.capture());
        assertEquals("Nyt parcelhus", form.getValue().getName());
        assertEquals("Byggeri", form.getValue().getDescription());
        assertEquals(LocalDate.of(2027, 3, 1), form.getValue().getStartDate());
        assertEquals(LocalDate.of(2027, 5, 28), form.getValue().getDeadline());
    }
}