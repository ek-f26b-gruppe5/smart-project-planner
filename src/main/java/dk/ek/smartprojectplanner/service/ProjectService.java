package dk.ek.smartprojectplanner.service;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.model.Project;
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

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Creates a project for the given manager.
     *
     * @param managerId the logged-in user's id
     * @param form      the values from the form
     * @return the new project's id
     * @throws InvalidInputException if a rule is broken; nothing is saved
     */
    public int create(int managerId, Project form) {
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