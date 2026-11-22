package dk.ek.smartprojectplanner.exception;

/** Thrown when something doesn't exist, or doesn't belong to the current user. Shown as a 404 page. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}