package system;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import survey.Survey;

public class Logger extends Middleware {

    public Logger(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
            AudioEventHandler audioEventHandler) {
        super(guildId, client, audioProvider, surveys, audioEventHandler);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean handle() {
        System.out.println("Received message '" + this.msgContent + "' by '" + this.msgAuthorName + "' with "
					+ this.msgAuthorObject.getId() + " at " + this.msgObject.getTimestamp());
        return true;
    }
    
}
