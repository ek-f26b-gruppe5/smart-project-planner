package dk.ek.smartprojectplanner.repository;

import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for UserRepository against H2; h2init.sql resets the data before each test. */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/h2init.sql")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // ---------- findByEmail ----------

    @Test
    void findByEmail_existingEmail_returnsUserWithRole() {
        // Act
        Optional<User> user = userRepository.findByEmail("manager@test.dk");

        // Assert
        assertTrue(user.isPresent());
        assertEquals("Test Manager", user.get().getName());
        assertEquals(Role.MANAGER, user.get().getRole());
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        // Act
        Optional<User> user = userRepository.findByEmail("ukendt@test.dk");

        // Assert
        assertTrue(user.isEmpty());
    }

    // ---------- save ----------

    @Test
    void save_newUser_canBeFoundWithDefaultRole() {
        // Act
        userRepository.save("Ny Bruger", "ny@test.dk", "hash");

        // Assert
        Optional<User> saved = userRepository.findByEmail("ny@test.dk");
        assertTrue(saved.isPresent());
        assertEquals("Ny Bruger", saved.get().getName());
        assertEquals(Role.MANAGER, saved.get().getRole());   // the database default
    }

    /**
     * Potential ideas for additional tests:
     * constraints: saving an e-mail that already exists throws DataIntegrityViolationException (UNIQUE)
     * roles: the employee from h2init.sql comes back with Role.EMPLOYEE
     * edge cases: an empty string as e-mail returns empty
     * exact match: the repository doesn't lower-case, so "MANAGER@test.dk" is not found (that's the service's job)
     */
}