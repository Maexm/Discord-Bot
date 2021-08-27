package system;

import logging.QuickLogger;

public class Logger extends Middleware {

    public Logger(MiddlewareConfig config) {
        super(config, true);
    }

    @Override
    protected boolean handle() {
        String timeStamp = this.isTextCommand() ? this.getMessage().getMessage().get().getTimestamp().toString() : "NO TIMESTAMP";
        QuickLogger.logInfo("Received message '"
        + this.getMessage().getContent()+ "' by '"
        + this.getMessage().getAuthorName()+ "' with Snowflake"
        + this.getMessage().getUser().getId() + " at "
        + timeStamp);
        return true;
    }
    
}
