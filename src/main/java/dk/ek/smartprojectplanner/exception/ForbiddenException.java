package dk.ek.smartprojectplanner.exception;

/**
 * Thrown by services when a logged-in user isn't allowed to do something, e.g. an
 * employee trying to create users. The message is shown to the user, so it is in Danish.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}