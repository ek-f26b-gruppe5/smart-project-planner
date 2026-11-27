package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.exception.InvalidLoginException;
import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.model.User;
import dk.ek.smartprojectplanner.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Handles logging in and out, and a manager creating employees.
 * A successful login stores the user's id, name and role in the session.
 * The rules (who may create users, validation, hashing and saving) live in
 * UserService, so this class only connects the web to the service.
 */
@Controller
public class UserController {

    // ---------- Placeholders: change when the endpoint decision is made ----------
    static final String NEW_EMPLOYEE_PATH = "/employees/new";
    static final String CREATE_EMPLOYEE_PATH = "/employees";
    static final String EMPLOYEE_FORM_VIEW = "employees/form";
    // -------------------------------------------------------------------------------

    // The name the layout reads to show a one-time confirmation message.
    // It must match "flash" in fragments/layout.html
    static final String FLASH = "flash";

    static final String EMPLOYEE_CREATED = "Medarbejderen er oprettet.";

    private final UserService userService;


    /**
     * Creates the controller with the service it needs.
     *
     * @param userService the service that logs users in and creates employees
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Shows the login form. */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    /**
     * Shows the empty form for creating an employee. Only managers see it; anyone else
     * gets the 403 page, because the service throws ForbiddenException.
     *
     * @param userRole the logged-in user's role from the session, or null if there is none
     * @return the employee form template
     */
    @GetMapping(NEW_EMPLOYEE_PATH)
    public String showEmployeeForm(@SessionAttribute(name = "userRole", required = false) Role userRole) {
        userService.requireManager(userRole);
        return EMPLOYEE_FORM_VIEW;
    }


    /**
     * Receives the employee form. If the input is valid, the employee is saved and the
     * manager is sent back to an empty form with a confirmation, ready to create the next one.
     * If a rule is broken, the form is shown again with the error message and what was typed.
     * If the user isn't a manager, the service throws ForbiddenException, which becomes the 403 page.
     *
     * @param userRole  the logged-in user's role from the session, or null if there is none
     * @param name      the name field from the form
     * @param email     the email field from the form
     * @param password  the initial password field from the form
     * @param model     used to send the error message and typed values to the template
     * @param redirect  used to carry the confirmation message across the redirect
     * @return a redirect to the empty form on success, otherwise the form template
     */
    @PostMapping(CREATE_EMPLOYEE_PATH)
    public String createEmployee(@SessionAttribute(name = "userRole", required = false) Role userRole,
                                 @RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 Model model,
                                 RedirectAttributes redirect) {
        try {
            userService.createEmployee(userRole, name, email, password);

            redirect.addFlashAttribute(FLASH, EMPLOYEE_CREATED);
            return "redirect:" + NEW_EMPLOYEE_PATH;
        } catch (InvalidInputException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return EMPLOYEE_FORM_VIEW;
        }
    }


    /**
     * Logs the user in and sends them to their front page.
     *
     * @param email    the e-mail the user typed
     * @param password the password the user typed, in plain text
     * @param session  the session where the logged-in user's id, name and role are stored
     * @param model    gets an {@code "error"} message if the login fails
     * @return a redirect to {@code /} on success, otherwise the login page with an error
     */
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = userService.login(email, password);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getName());   // shown in the header
            session.setAttribute("userRole", user.getRole());   // used for "only managers" checks
            return "redirect:/";
        } catch (InvalidLoginException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    /**
     * Logs the user out by ending the session.
     *
     * @param session the current session, which is invalidated
     * @return a redirect to {@code /login}
     */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}