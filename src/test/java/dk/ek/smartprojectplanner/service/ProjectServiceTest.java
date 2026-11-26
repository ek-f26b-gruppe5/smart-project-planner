package dk.ek.smartprojectplanner.service;

import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.model.Project;
import dk.ek.smartprojectplanner.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for ProjectService: the repository is mocked. */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final int MANAGER = 1;
    private static final LocalDate START = LocalDate.of(2027, 3, 1);
    private static final LocalDate DEADLINE = LocalDate.of(2027, 5, 28);

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService service;

    /** Builds a project the way the form would send it. */
    private static Project form(String name, String description, LocalDate start, LocalDate deadline) {
        Project p = new Project();
        p.setName(name);
        p.setDescription(description);
        p.setStartDate(start);
        p.setDeadline(deadline);
        return p;
    }

    // ---------- valid input ----------

    /**
     * Valid input with spaces around the text → trimmed, saved with the manager's id, and the new id returned.
     */
    @Test
    void create_validInput_trimsAndSavesForManager() {
        // Arrange
        when(projectRepository.save(any())).thenReturn(7);

        // Act
        int id = service.create(MANAGER, form("  Nyt parcelhus  ", "  Byggeri ", START, DEADLINE));

        // Assert
        assertEquals(7, id);
        ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(saved.capture());
        assertEquals(MANAGER, saved.getValue().getManagerId());
        assertEquals("Nyt parcelhus", saved.getValue().getName());
        assertEquals("Byggeri", saved.getValue().getDescription());
        assertEquals(START, saved.getValue().getStartDate());
        assertEquals(DEADLINE, saved.getValue().getDeadline());
    }

    /**
     * A description of only spaces → saved as null.
     */
    @Test
    void create_blankDescription_savesNull() {
        // Arrange
        Project form = form("Projekt", "   ", START, DEADLINE);

        // Act
        service.create(MANAGER, form);

        // Assert
        ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(saved.capture());
        assertNull(saved.getValue().getDescription());
    }

    // ---------- name ----------

    /**
     * Name null, empty, spaces or a tab → "Giv projektet et navn." and nothing saved.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void create_missingName_throwsAndSavesNothing(String name) {
        // Arrange
        Project form = form(name, null, START, DEADLINE);

        // Act + Assert
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> service.create(MANAGER, form));
        assertEquals(ProjectService.NAME_REQUIRED, e.getMessage());
        verify(projectRepository, never()).save(any());
    }

    /**
     * A name of exactly 100 characters → saved.
     */
    @Test
    void create_nameAtMaxLength_isAccepted() {
        // Arrange
        Project form = form("a".repeat(ProjectService.MAX_NAME_LENGTH), null, START, DEADLINE);

        // Act
        service.create(MANAGER, form);

        // Assert
        verify(projectRepository).save(any());
    }

    /**
     * A name of 101 characters → "Navnet må højst være 100 tegn." and nothing saved.
     */
    @Test
    void create_nameOneOverMaxLength_throwsAndSavesNothing() {
        // Arrange
        Project form = form("a".repeat(ProjectService.MAX_NAME_LENGTH + 1), null, START, DEADLINE);

        // Act + Assert
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> service.create(MANAGER, form));
        assertEquals(ProjectService.NAME_TOO_LONG, e.getMessage());
        verify(projectRepository, never()).save(any());
    }

    // ---------- description ----------

    /**
     * A description of 1001 characters → "Beskrivelsen må højst være 1000 tegn." and nothing saved.
     */
    @Test
    void create_descriptionOneOverMaxLength_throwsAndSavesNothing() {
        // Arrange
        Project form = form("Projekt", "a".repeat(ProjectService.MAX_DESCRIPTION_LENGTH + 1), START, DEADLINE);

        // Act + Assert
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> service.create(MANAGER, form));
        assertEquals(ProjectService.DESCRIPTION_TOO_LONG, e.getMessage());
        verify(projectRepository, never()).save(any());
    }

    // ---------- dates ----------

    /**
     * No start date → "Vælg en startdato." and nothing saved.
     */
    @Test
    void create_missingStartDate_throwsAndSavesNothing() {
        // Arrange
        Project form = form("Projekt", null, null, DEADLINE);

        // Act + Assert
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> service.create(MANAGER, form));
        assertEquals(ProjectService.START_DATE_REQUIRED, e.getMessage());
        verify(projectRepository, never()).save(any());
    }

    /**
     * No deadline → "Vælg en deadline." and nothing saved.
     */
    @Test
    void create_missingDeadline_throwsAndSavesNothing() {
        // Arrange
        Project form = form("Projekt", null, START, null);

        // Act + Assert
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> service.create(MANAGER, form));
        assertEquals(ProjectService.DEADLINE_REQUIRED, e.getMessage());
        verify(projectRepository, never()).save(any());
    }

    /**
     * Deadline one day before the start date → "Deadline kan ikke ligge før startdatoen." and nothing saved.
     */
    @Test
    void create_deadlineOneDayBeforeStart_throwsAndSavesNothing() {
        // Arrange
        Project form = form("Projekt", null, START, START.minusDays(1));

        // Act + Assert
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> service.create(MANAGER, form));
        assertEquals(ProjectService.DEADLINE_BEFORE_START, e.getMessage());
        verify(projectRepository, never()).save(any());
    }

    /**
     * Deadline on the same day as the start date → saved.
     */
    @Test
    void create_deadlineSameDayAsStart_isAccepted() {
        // Arrange
        Project form = form("Endagsprojekt", null, START, START);

        // Act
        service.create(MANAGER, form);

        // Assert
        verify(projectRepository).save(any());
    }
}