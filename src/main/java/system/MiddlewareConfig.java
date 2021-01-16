package system;

import java.util.function.Predicate;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import musicBot.MusicWrapper;
import start.GlobalDiscordHandler.GlobalDiscordProxy;

public class MiddlewareConfig {

	final Predicate<Message> mayAccept;
	final Snowflake guildId;
	final MusicWrapper musicWrapper;
	final GlobalDiscordProxy globalProxy;
	Message helloMessage;
    
    public MiddlewareConfig(final Snowflake guildId, MusicWrapper musicWrapper, GlobalDiscordProxy globalProxy, final Message helloMessage){
        this(guildId, musicWrapper, globalProxy, helloMessage, msg -> true);
    }

    public MiddlewareConfig(final Snowflake guildId, MusicWrapper musicWrapper, GlobalDiscordProxy globalProxy, Message helloMessage, final Predicate<Message> mayAccept) {
		this.guildId = guildId;
		this.mayAccept = mayAccept;
		this.musicWrapper = musicWrapper;
		this.globalProxy = globalProxy;
		this.helloMessage = helloMessage;
	}


}
