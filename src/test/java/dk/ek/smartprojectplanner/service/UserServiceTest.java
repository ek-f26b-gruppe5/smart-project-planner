package dk.ek.smartprojectplanner.service;

import dk.ek.smartprojectplanner.exception.ForbiddenException;
import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.exception.InvalidLoginException;
import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.model.User;
import dk.ek.smartprojectplanner.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for UserService: the repository is mocked, the password encoder is real. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    // ---------- requireManager ----------

    /**
     * A manager → no exception.
     */
    @Test
    void requireManager_manager_doesNotThrow() {
        // Arrange: nothing

        // Act + Assert
        assertDoesNotThrow(() -> service.requireManager(Role.MANAGER));
    }

    /**
     * An employee → ForbiddenException with "Du har ikke adgang til denne side."
     */
    @Test
    void requireManager_employee_throwsForbidden() {
        // Arrange: nothing

        // Act + Assert
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.requireManager(Role.EMPLOYEE));
        assertEquals(UserService.ACCESS_DENIED, ex.getMessage());
    }

    // ---------- createEmployee: who may create ----------

    /**
     * An employee tries to create a user → ForbiddenException, and the repository and encoder are never touched.
     */
    @Test
    void createEmployee_creatorIsEmployee_throwsForbiddenAndTouchesNothing() {
        // Arrange: valid input, so only the role can be the reason

        // Act + Assert
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.createEmployee(Role.EMPLOYEE, "Anna", "anna@mail.dk", "hunter22"));

        assertEquals(UserService.ACCESS_DENIED, ex.getMessage());
        verifyNoInteractions(repository, passwordEncoder);
    }

    /**
     * No role (e.g. a session without one) → ForbiddenException, and nothing is saved.
     */
    @Test
    void createEmployee_creatorRoleNull_throwsForbidden() {
        // Arrange: valid input, the role is missing

        // Act + Assert
        assertThrows(ForbiddenException.class,
                () -> service.createEmployee(null, "Anna", "anna@mail.dk", "hunter22"));

        verifyNoInteractions(repository);
    }

    /**
     * An employee with invalid input gets ForbiddenException, not a validation message.
     */
    @Test
    void createEmployee_creatorIsEmployeeAndInputInvalid_throwsForbiddenNotInvalidInput() {
        // Arrange: both the role and the password are wrong

        // Act + Assert
        assertThrows(ForbiddenException.class,
                () -> service.createEmployee(Role.EMPLOYEE, "", "ikke-en-email", "kort"));
    }

    // ---------- createEmployee: valid input ----------

    /**
     * A manager creates an employee → name trimmed, e-mail lower-cased, password hashed, saved with role EMPLOYEE.
     */
    @Test
    void createEmployee_validInput_savesCleanedInputAsEmployeeWithHashedPassword() {
        // Arrange
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.empty());

        // Act
        service.createEmployee(Role.MANAGER, "  Anna  ", "  ANNA@Mail.dk ", "hunter22");

        // Assert
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).save(eq("Anna"), eq("anna@mail.dk"), hash.capture(), eq(Role.EMPLOYEE));
        assertTrue(passwordEncoder.matches("hunter22", hash.getValue()));
    }

    // ---------- createEmployee: invalid input ----------

    /**
     * A password under 8 characters → "Adgangskoden skal være mindst 8 tegn." and nothing saved.
     */
    @Test
    void createEmployee_passwordTooShort_throwsAndDoesNotSave() {
        // Arrange: a manager, everything valid except the password

        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> service.createEmployee(Role.MANAGER, "Anna", "anna@mail.dk", "kort"));

        assertEquals(UserService.PASSWORD_TOO_SHORT, ex.getMessage());
        verify(repository, never()).save(anyString(), anyString(), anyString(), any(Role.class));
    }

    /**
     * The e-mail already exists → "Der findes allerede en bruger med den e-mail." and nothing saved.
     */
    @Test
    void createEmployee_emailAlreadyTaken_throwsAndDoesNotSave() {
        // Arrange
        User existing = new User(1, "Anna", "anna@mail.dk", "hash", Role.EMPLOYEE);
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.of(existing));

        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> service.createEmployee(Role.MANAGER, "Anna", "anna@mail.dk", "hunter22"));

        assertEquals(UserService.EMAIL_TAKEN, ex.getMessage());
        verify(repository, never()).save(anyString(), anyString(), anyString(), any(Role.class));
    }

    /**
     * The e-mail is free when checked but taken when saved (two at once) → the database's error becomes "e-mail taken".
     */
    @Test
    void createEmployee_emailTakenBetweenCheckAndSave_throwsEmailTaken() {
        // Arrange: findByEmail finds nothing, but the UNIQUE constraint rejects the INSERT
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("Duplicate entry"))
                .when(repository).save(anyString(), anyString(), anyString(), any(Role.class));

        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> service.createEmployee(Role.MANAGER, "Anna", "anna@mail.dk", "hunter22"));

        assertEquals(UserService.EMAIL_TAKEN, ex.getMessage());
    }

    // ---------- login ----------

    /**
     * The right password, with the e-mail typed with spaces and capitals → the user is returned.
     */
    @Test
    void login_correctPassword_returnsUser() {
        // Arrange
        User user = new User(1, "Anna", "anna@mail.dk", passwordEncoder.encode("hunter22"), Role.MANAGER);
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.of(user));

        // Act
        User result = service.login("  ANNA@mail.dk ", "hunter22");

        // Assert
        assertEquals(user, result);
    }

    /**
     * A wrong password → "Forkert e-mail eller adgangskode".
     */
    @Test
    void login_wrongPassword_throws() {
        // Arrange
        User user = new User(1, "Anna", "anna@mail.dk", passwordEncoder.encode("hunter22"), Role.MANAGER);
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.of(user));

        // Act + Assert
        InvalidLoginException ex = assertThrows(InvalidLoginException.class,
                () -> service.login("anna@mail.dk", "forkert1"));

        assertEquals(UserService.INVALID_LOGIN, ex.getMessage());
    }

    /*
     * Potential ideas for additional tests:
     * boundary values: a password of exactly 7 and 8 characters, a name of exactly 100 and 101 characters
     * equivalence classes: several invalid e-mails in one parameterized test
     * missing input: null or blank fields
     * security: an unknown e-mail gives the same message as a wrong password
     */
}