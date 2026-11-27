package dk.ek.smartprojectplanner.service;

import dk.ek.smartprojectplanner.exception.ForbiddenException;
import dk.ek.smartprojectplanner.exception.InvalidInputException;
import dk.ek.smartprojectplanner.exception.InvalidLoginException;
import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.model.User;
import dk.ek.smartprojectplanner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 *
 * Business rules for users: only managers may create users, new users are always
 * employees, and the input is validated, the e-mail checked and the password hashed
 * before anything is saved through UserRepository
 *
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    static final String INVALID_LOGIN = "Forkert e-mail eller adgangskode";

    // Max lengths - they match the columns in schema.sql, so the database
    // never has to reject a value that is too long
    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_EMAIL_LENGTH = 255;
    static final int MIN_PASSWORD_LENGTH = 8;


    // The error messages live in constants so they are written once and the tests
    // can reuse them. This is what is shown to the user
    static final String NAME_REQUIRED = "Skriv medarbejderens navn.";
    static final String NAME_TOO_LONG = "Navnet må højst være " + MAX_NAME_LENGTH + " tegn.";
    static final String EMAIL_REQUIRED = "Skriv medarbejderens e-mail.";
    static final String EMAIL_INVALID = "E-mailen er ikke gyldig.";
    static final String EMAIL_TOO_LONG = "E-mailen må højst være " + MAX_EMAIL_LENGTH + " tegn.";
    static final String EMAIL_TAKEN = "Der findes allerede en bruger med den e-mail.";
    static final String PASSWORD_REQUIRED = "Skriv en adgangskode.";
    static final String ACCESS_DENIED = "Du har ikke adgang til denne side.";
    static final String PASSWORD_TOO_SHORT = "Adgangskoden skal være mindst " + MIN_PASSWORD_LENGTH + " tegn.";


    // Constructor injection: Spring sees that this class needs a UserRepository,
    // creates it, and passes it in.
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    /**
     * The same error is used for an unknown e-mail and a wrong password, so the
     * message doesn't reveal which e-mails have an account.
     *
     * @param email       the e-mail the user typed
     * @param rawPassword the password the user typed, in plain text
     * @return the logged-in user
     * @throws InvalidLoginException if no user has that e-mail, or the password doesn't match
     */
    public User login(String email, String rawPassword) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> {
                    log.warn("Failed login: unknown e-mail");
                    return new InvalidLoginException(INVALID_LOGIN);
                });
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("Failed login for user {}", user.getId());
            throw new InvalidLoginException(INVALID_LOGIN);
        }
        log.info("User {} logged in", user.getId());
        return user;
    }


    /**
     * Checks that the logged-in user is a manager. Used before showing and before
     * saving anything only managers may do, so the rule lives in one place.
     *
     * @param role the logged-in user's role; may be null
     * @throws ForbiddenException if the role isn't MANAGER
     */
    public void requireManager(Role role) {
        if (role != Role.MANAGER) {
            log.warn("Manager-only action refused for role {}", role);
            throw new ForbiddenException(ACCESS_DENIED);
        }
    }

    /**
     * Creates a new employee. Only a manager may do this; the new user always gets the role EMPLOYEE.
     * <p>
     *     The role is checked first, before the input, so a user who isn't allowed gets no
     *     validation feedback. Name and email are trimmed and the email is made lower case
     *     before it is checked and saved. The password is not trimmed. It is hashed with
     *     BCrypt, so the plain password is never stored.
     * </p>
     * @param creatorRole the role of the logged-in user who creates the employee; may be null
     * @param name        the name the manager typed
     * @param email       the email the manager typed
     * @param password    the initial password the manager typed, in plain text
     * @throws ForbiddenException    if the creator isn't a manager
     * @throws InvalidInputException if a rule is broken or the e-mail is taken
     */
    public void createEmployee(Role creatorRole, String name, String email, String password) {

        // 0. Only managers may create users. Checked first, so nothing else happens if not
        requireManager(creatorRole);

        // 1. Clean the input:
        // Makes sure that the String is always a real text,
        // and is ready to be correctly checked by the rules below
        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();


        // 2. Check the rules, one at a time
        // "throw" stops the method immediately
        if (cleanName.isEmpty()) {
            throw new InvalidInputException(NAME_REQUIRED);
        }
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException(NAME_TOO_LONG);
        }
        if (cleanEmail.isEmpty()) {
            throw new InvalidInputException(EMAIL_REQUIRED);
        }
        if (cleanEmail.length() > MAX_EMAIL_LENGTH) {
            throw new InvalidInputException(EMAIL_TOO_LONG);
        }
        // Email format check:
        // matches() compares the cleanEmail text with this pattern (a regex),
        // to make sure that it fits the standard email pattern.
        if (!cleanEmail.matches("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")) {
            throw new InvalidInputException(EMAIL_INVALID);
        }
        if (password == null || password.isEmpty()) {
            throw new InvalidInputException(PASSWORD_REQUIRED);
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidInputException(PASSWORD_TOO_SHORT);
        }

        // 3. Is the email already taken?
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new InvalidInputException(EMAIL_TAKEN);
        }


        // 4. Turn the plain password into a BCrypt hash.
        // From here on only the hash is used, and the plain password is never saved
        String passwordHash = passwordEncoder.encode(password);


        // 5. Save the user.

        // If two managers create a user with the same email at the same time:
        // both will pass step 3, because nothing is saved yet.
        // The UNIQUE rule in the database then rejects the second INSERT with a
        // DataIntegrityViolationException - we catch it and show the error message
        try {
            userRepository.save(cleanName, cleanEmail, passwordHash, Role.EMPLOYEE);
            log.info("New employee created");
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidInputException(EMAIL_TAKEN);
        }
    }
}