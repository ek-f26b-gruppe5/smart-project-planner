package dk.ek.smartprojectplanner.repository;

import dk.ek.smartprojectplanner.model.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for ProjectRepository against H2; h2init.sql resets the data before each test. */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/h2init.sql")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Saves a project, gets an id back, then finds the project by that id and compares all four fields.
     */
    @Test
    void save_validProject_canBeFoundByReturnedIdWithAllFields() {
        // Arrange
        Project project = new Project(null, 1, "Eksamen", "Gruppe 5",
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 16));

        // Act
        int id = projectRepository.save(project);

        // Assert
        Project saved = projectRepository.findByManagerId(1).stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Saved project not found by its id " + id));
        assertEquals("Eksamen", saved.getName());
        assertEquals("Gruppe 5", saved.getDescription());
        assertEquals(LocalDate.of(2026, 11, 16), saved.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 16), saved.getDeadline());
    }

    /**
     * Saves a project with description = null and checks it comes back as null.
     */
    @Test
    void save_withoutDescription_storesNull() {
        // Arrange
        Project project = new Project(null, 1, "Uden beskrivelse", null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1));

        // Act
        int id = projectRepository.save(project);

        // Assert
        Project saved = projectRepository.findByManagerId(1).stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Saved project not found by its id " + id));
        assertNull(saved.getDescription());
    }

    /**
     * Manager 1's list contains their project and not user 2's project.
     */
    @Test
    void findByManagerId_onlyReturnsThatManagersProjects() {
        // Arrange
        projectRepository.save(new Project(null, 1, "Mit projekt", null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)));
        projectRepository.save(new Project(null, 2, "Andens projekt", null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)));

        // Act
        List<String> names = projectRepository.findByManagerId(1).stream()
                .map(Project::getName)
                .toList();

        // Assert
        assertTrue(names.contains("Mit projekt"));
        assertFalse(names.contains("Andens projekt"));
    }

    /**
     * manager_id = 999 → exception from the foreign key.
     */
    @Test
    void save_unknownManager_isRejectedByForeignKey() {
        // Arrange
        Project project = new Project(null, 999, "Forkert ejer", null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1));

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class, () -> projectRepository.save(project));
    }

    /**
     * A deadline before the start date → exception from the database.
     */
    @Test
    void save_deadlineBeforeStart_isRejectedByCheckConstraint() {
        // Arrange: the service stops this first; the database is the last line of defence
        Project project = new Project(null, 1, "Baglæns", null,
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 1, 1));

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class, () -> projectRepository.save(project));
    }
}