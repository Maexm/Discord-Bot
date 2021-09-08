package system;

import discord4j.core.object.entity.channel.MessageChannel;

public class MusicInfoPusher extends Middleware{

    public MusicInfoPusher(MiddlewareConfig config) {
        super(config, true);
    }

    @Override
    protected boolean handle() {
        if(this.isPrivate()){
            return true;
        }
        
        MessageChannel musicChannel = this.getConfig().musicWrapper.getMusicBotHandler().getRadioMessageChannel().orElse(null);
        if(musicChannel != null && this.getMessageChannel().getId().equals(musicChannel.getId())){
            this.getConfig().musicWrapper.getMusicBotHandler().tryDeleteRadioMessage();
        }
        return true;
    }
    
}
