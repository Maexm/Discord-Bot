package system;

import java.util.HashMap;
import java.util.HashSet;
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
	public final HashMap<Snowflake, HashSet<Snowflake>> voiceSubscriberMap;

	Message helloMessage;
    
    public MiddlewareConfig(final Snowflake guildId, MusicWrapper musicWrapper, GlobalDiscordProxy globalProxy, final Message helloMessage, HashMap<Snowflake, HashSet<Snowflake>> voiceSubscriberMap){
        this(guildId, musicWrapper, globalProxy, helloMessage, voiceSubscriberMap, msg -> true);
    }

    public MiddlewareConfig(final Snowflake guildId, MusicWrapper musicWrapper, GlobalDiscordProxy globalProxy, Message helloMessage, HashMap<Snowflake, HashSet<Snowflake>> voiceSubscriberMap, final Predicate<Message> mayAccept) {
		this.guildId = guildId;
		this.mayAccept = mayAccept;
		this.musicWrapper = musicWrapper;
		this.globalProxy = globalProxy;
		this.helloMessage = helloMessage;
		this.voiceSubscriberMap = voiceSubscriberMap;
	}


}
