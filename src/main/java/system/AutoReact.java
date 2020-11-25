package system;

import java.util.ArrayList;
import java.util.function.Predicate;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import survey.Survey;

public class AutoReact extends Middleware {
	
	private final ReactionEmoji emoji;
	
	public AutoReact(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
			AudioEventHandler audioEventHandler, Predicate<Message> mayAccept, ReactionEmoji emoji) {
		super(guildId, client, audioProvider, surveys, audioEventHandler, mayAccept);
		this.emoji = emoji;
	}

	@Override
	protected boolean handle() {

		this.msgObject.addReaction(this.emoji);
		
		return false;
	}

	
    
    
}
