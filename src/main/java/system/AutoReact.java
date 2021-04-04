package system;

import java.util.function.Predicate;

import discord4j.core.object.reaction.ReactionEmoji;

public class AutoReact extends Middleware {
	
	private final ReactionEmoji[] emojis;

	public AutoReact(MiddlewareConfig config, Predicate<DecompiledMessage> mayAccept, ReactionEmoji[] emojis){
		super(config, true, mayAccept);
		this.emojis = emojis;
	}
	
	public AutoReact(MiddlewareConfig config, Predicate<DecompiledMessage> mayAccept, ReactionEmoji emoji) {
		this(config, mayAccept, new ReactionEmoji[]{emoji});
	}

	@Override
	protected boolean handle() {
		if(!this.isTextCommand()){
			return true;
		}

		for(ReactionEmoji emoji : this.emojis){
			try{
				this.getMessage().getMessage().get().addReaction(emoji).block();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return true;
	}
}
