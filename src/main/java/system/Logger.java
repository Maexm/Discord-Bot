package system;

public class Logger extends Middleware {

    public Logger(MiddlewareConfig config) {
        super(config);
    }

    @Override
    protected boolean handle() {
        System.out.println("Received message '"
        + this.getMessage().getContent()+ "' by '"
        + this.getMessage().getAuthorName()+ "' with Snowflake"
        + this.getMessage().getUser().getId() + " at "
        + this.getMessage().getMessageObject().getTimestamp());
        return true;
    }
    
}
