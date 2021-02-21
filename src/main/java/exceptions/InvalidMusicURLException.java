package exceptions;

public class InvalidMusicURLException extends Exception{
	
	private static final long serialVersionUID = 1L;

	public InvalidMusicURLException(String message) {
		super(message);
	}
}
