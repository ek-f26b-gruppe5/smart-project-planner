package dk.ek.smartprojectplanner.repository;

import dk.ek.smartprojectplanner.model.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** SQL for the project table. */
@Repository
public class ProjectRepository {

    private static final String INSERT =
            "INSERT INTO project (manager_id, name, description, start_date, deadline) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_MANAGER =
            "SELECT id, manager_id, name, description, start_date, deadline FROM project "
                    + "WHERE manager_id = ? ORDER BY deadline, id";

    private static final RowMapper<Project> PROJECT_MAPPER = (rs, rowNum) -> new Project(
            rs.getInt("id"),
            rs.getInt("manager_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("deadline", LocalDate.class));

    private final JdbcTemplate jdbcTemplate;

    public ProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Saves a new project.
     *
     * @param project the project to save; its id is ignored
     * @return the id the database gave the new project
     */
    public int save(Project project) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT, new String[]{"id"});
            ps.setInt(1, project.getManagerId());
            ps.setString(2, project.getName());
            ps.setString(3, project.getDescription());
            ps.setDate(4, Date.valueOf(project.getStartDate()));
            ps.setDate(5, Date.valueOf(project.getDeadline()));
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No id returned").intValue();
    }

    /**
     * Finds all projects managed by one user, earliest deadline first.
     *
     * @param managerId the logged-in user's id
     * @return the projects; an empty list if there are none
     */
    public List<Project> findByManagerId(int managerId) {
        return jdbcTemplate.query(SELECT_BY_MANAGER, PROJECT_MAPPER, managerId);
    }
}