package system;

import snowflakes.ChannelID;

public class VoiceGuard extends Middleware {

    public VoiceGuard(MiddlewareConfig config) {
        super(config, true);
    }

    @Override
    protected boolean handle() {
        if(this.getMessage().getChannel().getId().equals(ChannelID.VOICE) && !this.isAuthorVoiceConnectedGuildScoped()){
            this.sendPrivateAnswer("Bitte verwende den VoiceChannel nur dann, wenn du auch wirklich in einem Voicechannel bist!\n"
            + "Für alles andere sind die anderen Textkanäle gedacht."
            +"\n\n"
            +"Danke!");
        }
        return true;
    }
    
}
