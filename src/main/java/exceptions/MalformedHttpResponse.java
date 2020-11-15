package exceptions;

public class MalformedHttpResponse extends Exception{
    public MalformedHttpResponse(String message){
        super(message);
    }
}
