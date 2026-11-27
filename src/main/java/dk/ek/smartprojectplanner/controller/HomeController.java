package dk.ek.smartprojectplanner.controller;

import dk.ek.smartprojectplanner.service.HealthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The front page and the health check.
 * The health check is public, so monitoring can use it without logging in.
 */
@Controller
public class HomeController {

    static final String HEALTH_PATH = "/health";

    private final HealthService healthService;

    /**
     * Creates the controller with the service it needs.
     *
     * @param healthService the service that checks the database
     */
    public HomeController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * The front page, showing the database status.
     *
     * @param model gets the database status
     * @return the front page template
     */
    @GetMapping("/")
    public String index() {
        return "redirect:" + ProjectController.LIST_PATH;
    }

    /**
     * Health check for monitoring (e.g. curl in the standup workflow). Public, no login needed.
     *
     * @return "OK" if the database answers, otherwise "ERROR", as plain text
     */
    @GetMapping(HEALTH_PATH)
    @ResponseBody
    public String health() {
        return healthService.databaseStatus();
    }

    /**
     * Health check for monitoring (e.g. curl in the standup workflow). Public, no login needed.
     *
     * @return "OK" if the database answers, otherwise "ERROR", as plain text
     */
    @GetMapping(HEALTH_PATH)
    @ResponseBody
    public String health() {
        return healthService.databaseStatus();
    }
}