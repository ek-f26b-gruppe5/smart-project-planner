package dk.ek.smartprojectplanner.service;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.model.Project;
import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Rules for creating and listing projects. */
@Service
public class ProjectService {

    static final int MAX_NAME_LENGTH = 100;          // matches VARCHAR(100)
    static final int MAX_DESCRIPTION_LENGTH = 1000;  // matches VARCHAR(1000)

    static final String NAME_REQUIRED = "Giv projektet et navn.";
    static final String NAME_TOO_LONG = "Navnet må højst være " + MAX_NAME_LENGTH + " tegn.";
    static final String DESCRIPTION_TOO_LONG = "Beskrivelsen må højst være " + MAX_DESCRIPTION_LENGTH + " tegn.";
    static final String START_DATE_REQUIRED = "Vælg en startdato.";
    static final String DEADLINE_REQUIRED = "Vælg en deadline.";
    static final String DEADLINE_BEFORE_START = "Deadline kan ikke ligge før startdatoen.";

    private final ProjectRepository projectRepository;
    private final UserService userService;

    /**
     * Creates the service with what it needs.
     *
     * @param projectRepository the SQL for projects
     * @param userService       owns the "only managers" rule, reused here
     */
    public ProjectService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    /**
     * Checks that the logged-in user may create projects (only managers).
     * The controller calls this before showing the form.
     *
     * @param role the logged-in user's role; may be null
     * @throws dk.ek.smartprojectplanner.exception.ForbiddenException if the role isn't MANAGER
     */
    public void requireCanCreate(Role role) {
        userService.requireManager(role);
    }

    /**
     * Creates a project for the given manager. Only managers may do this.
     *
     * @param creatorRole the logged-in user's role; checked first
     * @param managerId   the logged-in user's id
     * @param form        the values from the form
     * @return the new project's id
     * @throws dk.ek.smartprojectplanner.exception.ForbiddenException if the creator isn't a manager; nothing is checked or saved
     * @throws InvalidInputException if a rule is broken; nothing is saved
     */
    public int create(Role creatorRole, int managerId, Project form) {
        // 0. Only managers may create projects. Checked first, so nothing else happens if not
        requireCanCreate(creatorRole);

        // 1. clean
        String name = form.getName() == null ? "" : form.getName().trim();
        String description = form.getDescription() == null ? "" : form.getDescription().trim();

        // 2. validate
        if (name.isEmpty()) throw new InvalidInputException(NAME_REQUIRED);
        if (name.length() > MAX_NAME_LENGTH) throw new InvalidInputException(NAME_TOO_LONG);
        if (description.length() > MAX_DESCRIPTION_LENGTH) throw new InvalidInputException(DESCRIPTION_TOO_LONG);
        if (form.getStartDate() == null) throw new InvalidInputException(START_DATE_REQUIRED);
        if (form.getDeadline() == null) throw new InvalidInputException(DEADLINE_REQUIRED);
        if (form.getDeadline().isBefore(form.getStartDate())) throw new InvalidInputException(DEADLINE_BEFORE_START);

        // 3. save
        Project project = new Project(null, managerId, name,
                description.isEmpty() ? null : description,
                form.getStartDate(), form.getDeadline());
        return projectRepository.save(project);
    }

    /**
     * Lists the manager's projects.
     *
     * @param managerId the logged-in user's id
     * @return the projects, earliest deadline first
     */
    public List<Project> getProjects(int managerId) {
        return projectRepository.findByManagerId(managerId);
    }
}