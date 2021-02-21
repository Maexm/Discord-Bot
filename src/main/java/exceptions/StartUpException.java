package exceptions;

public class StartUpException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public StartUpException(String message) {
        super(message);
    }
}
