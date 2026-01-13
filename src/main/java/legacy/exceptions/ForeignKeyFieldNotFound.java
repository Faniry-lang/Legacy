package legacy.exceptions;

public class ForeignKeyFieldNotFound extends RuntimeException {
    public ForeignKeyFieldNotFound(String message) {
        super(message);
    }
}
