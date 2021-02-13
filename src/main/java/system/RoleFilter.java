package system;

import java.util.function.Predicate;

import discord4j.core.object.entity.Message;
import security.SecurityLevel;

public class RoleFilter extends Middleware {

    public SecurityLevel required;
    public String message;

    public RoleFilter(MiddlewareConfig config, Predicate<Message> mayAccept, SecurityLevel required) {
            this(config, mayAccept, required, "");
    }

    public RoleFilter(MiddlewareConfig config, Predicate<Message> mayAccept, SecurityLevel required, String message) {
        super(config, true, mayAccept);
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
