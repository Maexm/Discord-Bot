package system;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Predicate;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import musicBot.MusicWrapper;
import security.SecurityProvider;
import start.GlobalDiscordProxy;
import util.Pair;

public class MiddlewareConfig {

	private final Predicate<Message> mayAccept;
	public final Snowflake guildId;
	final MusicWrapper musicWrapper;
	final GlobalDiscordProxy globalProxy;
	public final HashMap<Snowflake, Pair<Calendar, HashSet<Snowflake>>> voiceSubscriberMap;
	public Snowflake announcementChannelId = null;
	public boolean updateNote = true;
	public boolean psaNote = true;
	private final SecurityProvider securityProvider;
	public String homeTown = null;

	Message helloMessage = null;
    
    public MiddlewareConfig(final Snowflake guildId, MusicWrapper musicWrapper, GlobalDiscordProxy globalProxy, final Message helloMessage, HashMap<Snowflake, Pair<Calendar, HashSet<Snowflake>>> voiceSubscriberMap){
        this(guildId, musicWrapper, globalProxy, helloMessage, voiceSubscriberMap, msg -> true);
    }

    public MiddlewareConfig(final Snowflake guildId, MusicWrapper musicWrapper, GlobalDiscordProxy globalProxy, Message helloMessage, HashMap<Snowflake, Pair<Calendar, HashSet<Snowflake>>> voiceSubscriberMap, final Predicate<Message> mayAccept) {
		this.guildId = guildId;
		this.mayAccept = mayAccept;
		this.musicWrapper = musicWrapper;
		this.globalProxy = globalProxy;
		this.helloMessage = helloMessage;
		this.voiceSubscriberMap = voiceSubscriberMap;
		this.securityProvider = new SecurityProvider(null, this);
	}

	public Predicate<Message> UNSAFE_mayAccept(){
		return this.mayAccept;
	}

	public SecurityProvider getSecurityProvider(){
		return this.securityProvider;
	}

	public GlobalDiscordProxy getGlobalDiscordProxy(){
		return this.globalProxy;
	}

	public MusicWrapper getMusicWrapper(){
		return this.musicWrapper;
	}
}
