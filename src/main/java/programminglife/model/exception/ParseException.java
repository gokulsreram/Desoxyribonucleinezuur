package programminglife.model.exception;

/**
 * A checked {@link Exception} thrown when an unknown type identifier (H/S/L) is read from a GFA file.
 */
public class ParseException extends Exception {
    /**
     * Default constructor for {@link ParseException} containing just a message.
     *
     * @param message the message about the unknown identifier.
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constructor for {@link ParseException} containing a message and a cause.
     *
     * @param message the message about the unknown identifier.
     * @param cause   The cause of this Exception.
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
