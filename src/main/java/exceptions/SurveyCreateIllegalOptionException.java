package exceptions;

public class SurveyCreateIllegalOptionException extends Exception{
	
	private static final long serialVersionUID = 1L;

	public SurveyCreateIllegalOptionException(String message) {
		super(message);
	}

}
