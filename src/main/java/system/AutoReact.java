package system;

import java.util.function.Predicate;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;

public class AutoReact extends Middleware {
	
	private final ReactionEmoji[] emojis;

	public AutoReact(MiddlewareConfig config, Predicate<Message> mayAccept, ReactionEmoji[] emojis){
		super(config, mayAccept);
		this.emojis = emojis;
	}
	
	public AutoReact(MiddlewareConfig config, Predicate<Message> mayAccept, ReactionEmoji emoji) {
		this(config, mayAccept, new ReactionEmoji[]{emoji});
	}

	@Override
	protected boolean handle() {

		for(ReactionEmoji emoji : this.emojis){
			try{
				this.getMessage().getMessageObject().addReaction(emoji).block();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return true;
	}
}
