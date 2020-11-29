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
	
	private final ReactionEmoji[] emojis;

	public AutoReact(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
	AudioEventHandler audioEventHandler, Predicate<Message> mayAccept, ReactionEmoji[] emojis){
		super(guildId, client, audioProvider, surveys, audioEventHandler, mayAccept);
		this.emojis = emojis;
	}
	
	public AutoReact(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
			AudioEventHandler audioEventHandler, Predicate<Message> mayAccept, ReactionEmoji emoji) {
		this(guildId, client, audioProvider, surveys, audioEventHandler, mayAccept, new ReactionEmoji[]{emoji});
	}

	@Override
	protected boolean handle() {

		for(ReactionEmoji emoji : this.emojis){
			try{
				this.msgObject.addReaction(emoji);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return true;
	}
}
