package system;

import java.util.function.Predicate;

import discord4j.core.object.entity.Message;

public class RoleFilter extends Middleware {

    public int required;
    public String message;

    public RoleFilter(MiddlewareConfig config, Predicate<Message> mayAccept, int required) {
            this(config, mayAccept, required, "");
    }

    public RoleFilter(MiddlewareConfig config, Predicate<Message> mayAccept, int required, String message) {
        super(config, mayAccept);
        this.message = message;
        this.required = required;
    }

    @Override
    protected boolean handle() {
        boolean hasPermission = this.hasPermission(required);
        if (!hasPermission && this.message != null && this.message != "") {
            this.sendAnswer(this.message);
        }
        return hasPermission;
    }

}
