package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.model.Project;
import dk.ek.smartprojectplanner.service.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the web requests for the project list and for creating a project.
 * The rules live in ProjectService, so this class only connects the web to the service.
 */
@Controller
public class ProjectController {

    // ---------- Placeholders: change when the endpoint decision is made ----------
    static final String LIST_PATH = "/projects";
    static final String NEW_PATH = "/projects/new";
    static final String CREATE_PATH = "/projects";
    static final String LIST_VIEW = "projects/list";
    static final String FORM_VIEW = "projects/form";
    // -------------------------------------------------------------------------------

    // Model and flash names the templates read. "error" and "flash" match login.html and layout.html
    static final String PROJECT = "project";
    static final String PROJECTS = "projects";
    static final String ERROR = "error";
    static final String FLASH = "flash";

    static final String CREATED = "Projektet er oprettet.";
    static final String INVALID_DATE = "Datoen kunne ikke læses. Vælg den i kalenderen.";

    private final ProjectService projectService;

    /**
     * Creates the controller with the service it needs.
     *
     * @param projectService the service that creates and lists projects
     */
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Shows the logged-in user's projects.
     *
     * @param userId the logged-in user's id, from the session
     * @param model  gets the list of projects
     * @return the list template
     */
    @GetMapping(LIST_PATH)
    public String list(@SessionAttribute("userId") int userId, Model model) {
        model.addAttribute(PROJECTS, projectService.getProjects(userId));
        return LIST_VIEW;
    }

    /**
     * Shows an empty form for a new project.
     *
     * @param model gets an empty project for the form to bind to
     * @return the form template
     */
    @GetMapping(NEW_PATH)
    public String showCreateForm(Model model) {
        model.addAttribute(PROJECT, new Project());
        return FORM_VIEW;
    }

    /**
     * Receives the form. If the input is valid, the project is saved and the user is
     * redirected to the list with a confirmation. If not, the form is shown again
     * with the error message and what the user typed.
     *
     * @param userId        the logged-in user's id, from the session; never taken from the form
     * @param form          the form fields, bound into a Project
     * @param bindingResult errors from turning the text into a Project, e.g. an unreadable date
     * @param model         gets the error message if something is wrong
     * @param redirect      carries the confirmation across the redirect
     * @return a redirect to the list on success, otherwise the form template
     */
    @PostMapping(CREATE_PATH)
    public String create(@SessionAttribute("userId") int userId,
                         @ModelAttribute(PROJECT) Project form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ERROR, INVALID_DATE);
            return FORM_VIEW;
        }
        try {
            projectService.create(userId, form);
            redirect.addFlashAttribute(FLASH, CREATED);
            return "redirect:" + LIST_PATH;
        } catch (InvalidInputException e) {
            model.addAttribute(ERROR, e.getMessage());
            return FORM_VIEW;
        }
    }
}