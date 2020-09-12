package exceptions;

public class IllegalMagicException extends RuntimeException{
	public IllegalMagicException(String message) {
		super("BAKA! "+message);
	}
}
