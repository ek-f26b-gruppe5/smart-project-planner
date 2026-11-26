package dk.ek.smartprojectplanner.model;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** A project owned by one project manager. */
public class Project {
    private Integer id;
    private Integer managerId;
    private String name;
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)   // <input type="date"> sends 2027-03-01
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    /** Needed so Spring can fill the object from the form. */
    public Project() { }

    public Project(Integer id, Integer managerId, String name, String description,
                   LocalDate startDate, LocalDate deadline) {
        this.id = id;
        this.managerId = managerId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.deadline = deadline;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer managerId) { this.managerId = managerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}