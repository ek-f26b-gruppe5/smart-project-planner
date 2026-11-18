package dk.ek.smartprojectplanner.repository;

import dk.ek.smartprojectplanner.model.Role;
import dk.ek.smartprojectplanner.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Reads users from the app_user table. */
/** Reads and saves users in the app_user table. */
@Repository
public class UserRepository {

    private static final String INSERT_USER =
            "INSERT INTO app_user (name, email, password_hash) VALUES (?, ?, ?)";
    private static final String SELECT_BY_EMAIL =
            "SELECT id, name, email, password_hash, role FROM app_user WHERE email = ?";

    // Turns one row into one User; the column names must match schema.sql
    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) -> new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")));

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds the user with the given e-mail.
     *
     * @param email the e-mail to look for; must match exactly
     * @return the user, or empty if no user has that e-mail
     */
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(SELECT_BY_EMAIL, USER_MAPPER, email)
                .stream()
                .findFirst();
    }

    /** Saves a new user. The password must already be hashed. */
    public void save(String name, String email, String passwordHash) {
        jdbcTemplate.update(INSERT_USER, name, email, passwordHash);
    }
}