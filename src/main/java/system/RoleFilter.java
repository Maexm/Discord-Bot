package system;

import java.util.ArrayList;
import java.util.function.Predicate;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import survey.Survey;

public class RoleFilter extends Middleware {

    public int required;
    public String message;

    public RoleFilter(GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
            AudioEventHandler audioEventHandler, Predicate<Message> mayAccept, int required) {
            this(client, audioProvider, surveys, audioEventHandler, mayAccept, required, "");
    }

    public RoleFilter(GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
            AudioEventHandler audioEventHandler, Predicate<Message> mayAccept, int required, String message) {
        super(client, audioProvider, surveys, audioEventHandler, mayAccept);
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
