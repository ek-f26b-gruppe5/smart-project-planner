package dk.ek.smartprojectplanner.service;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    // ---------- register ----------

    @Test
    void register_validInput_savesCleanedInputWithHashedPassword() {
        // Arrange
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.empty());

        // Act
        service.register("  Anna  ", "  ANNA@Mail.dk ", "hunter22");

        // Assert: name trimmed, e-mail lower-cased, password stored as a BCrypt hash
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).save(eq("Anna"), eq("anna@mail.dk"), hash.capture());
        assertTrue(passwordEncoder.matches("hunter22", hash.getValue()));
    }

    @Test
    void register_passwordTooShort_throwsAndDoesNotSave() {
        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> service.register("Anna", "anna@mail.dk", "kort"));

        assertEquals(UserService.PASSWORD_TOO_SHORT, ex.getMessage());
        verify(repository, never()).save(anyString(), anyString(), anyString());
    }

    @Test
    void register_emailAlreadyTaken_throwsAndDoesNotSave() {
        // Arrange
        User existing = new User(1, "Anna", "anna@mail.dk", "hash", Role.MANAGER);
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.of(existing));

        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> service.register("Anna", "anna@mail.dk", "hunter22"));

        assertEquals(UserService.EMAIL_TAKEN, ex.getMessage());
        verify(repository, never()).save(anyString(), anyString(), anyString());
    }

    // ---------- login ----------

    @Test
    void login_correctPassword_returnsUser() {
        // Arrange
        User user = new User(1, "Anna", "anna@mail.dk", passwordEncoder.encode("hunter22"), Role.MANAGER);
        when(repository.findByEmail("anna@mail.dk")).thenReturn(Optional.of(user));

        // Act: the e-mail is typed with spaces and capitals
        User result = service.login("  ANNA@mail.dk ", "hunter22");

        // Assert
        assertEquals(user, result);
    }

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

    /**
     * Potenial ideas for additional tests:
     * boundary values: a password of exactly 7 and 8 characters, a name of exactly 100 and 101 characters
     * equivalence classes: several invalid e-mails in one parameterized test
     * missing input: null or blank fields
     * security: an unknown e-mail gives the same message as a wrong password
     * race condition: two people registering the same e-mail at the same moment
     */
}