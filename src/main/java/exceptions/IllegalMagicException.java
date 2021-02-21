package exceptions;

public class IllegalMagicException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public IllegalMagicException(String message) {
		super("BAKA! "+message);
	}
}
