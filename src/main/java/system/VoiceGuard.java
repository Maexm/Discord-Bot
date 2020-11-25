package system;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import snowflakes.ChannelID;
import survey.Survey;

public class VoiceGuard extends Middleware {

    public VoiceGuard(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
            AudioEventHandler audioEventHandler) {
        super(guildId, client, audioProvider, surveys, audioEventHandler);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean handle() {
        if(this.msgObject.getChannelId().equals(ChannelID.VOICE) && !this.isAuthorVoiceConnected()){
            this.sendPrivateAnswer("Bitte verwende den VoiceChannel nur dann, wenn du auch wirklich in einem Voicechannel bist!\n"
            + "Für alles andere sind die anderen Textkanäle gedacht."
            +"\n\n"
            +"Danke!");
        }
        return true;
    }
    
}
