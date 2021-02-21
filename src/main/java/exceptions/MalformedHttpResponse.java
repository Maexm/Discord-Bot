package exceptions;

public class MalformedHttpResponse extends Exception{

    private static final long serialVersionUID = 1L;

    public MalformedHttpResponse(String message) {
        super(message);
    }
}
