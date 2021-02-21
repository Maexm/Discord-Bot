package exceptions;

public class SurveyCreateIllegalDurationException extends Exception{
	
	private static final long serialVersionUID = 1L;

	public SurveyCreateIllegalDurationException(String message) {
		super(message);
	}

}
