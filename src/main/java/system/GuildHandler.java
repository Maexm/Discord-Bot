package system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import musicBot.MusicWrapper;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import security.SecurityLevel;
import start.RuntimeVariables;
import start.GlobalDiscordHandler.GlobalDiscordProxy;

public final class GuildHandler {

	private ResponseType responseSet;
	private final ArrayList<Middleware> middlewareBefore;
	private final Snowflake guildId;
	private final TaskManager<RefinedTimerTask> localTasks;
	private final MiddlewareConfig middlewareConfig;
	private final GlobalDiscordProxy globalProxy;
	private final MusicWrapper musicWrapper;
	private final Guild guild;

	public GuildHandler(final Snowflake guildId, final GlobalDiscordProxy globalProxy) {

		this.middlewareBefore = new ArrayList<Middleware>();
		this.localTasks = new TaskManager<>(true);
		this.guildId = guildId;
		this.globalProxy = globalProxy;
		this.musicWrapper = new MusicWrapper();
		this.middlewareConfig = new MiddlewareConfig(this.guildId, this.musicWrapper, this.globalProxy, null, new HashMap<>());
		this.guild = this.globalProxy.getClient().getGuildById(this.guildId).block();

		// ########## RESPONSE SETS ##########
		// TODO: Shorten this hell
		this.middlewareBefore.add(new Logger(this.middlewareConfig));
		this.middlewareBefore.add(new RoleFilter(this.middlewareConfig,
				msg -> RuntimeVariables.IS_DEBUG
						&& msg.getContent().toLowerCase().startsWith(RuntimeVariables.MESSAGE_PREFIX.toLowerCase()),
				SecurityLevel.DEV, "meine Dienste sind im Preview Modus nicht verfügbar!"));

		this.middlewareBefore.add(new HelpSection(this.middlewareConfig));
		this.middlewareBefore.add(
				new AutoReact(this.middlewareConfig, msg -> {
					final String[] expressions = { "explosion", "kaboom", "bakuhatsu", "bakuretsu", "ばくれつ", "爆裂", "ばくはつ",
							"爆発", "explode", "feuerwerk", "böller", "explosiv", "detonation", "explodier"};
					final String evalStr = msg.getContent().toLowerCase();
					for (String expr : expressions) {
						if (evalStr.contains(expr)) {
							return true;
						}
					}
					;
					return false; // React if evalStr contains something from array, skip otherwise
				}, new ReactionEmoji[] { ReactionEmoji.unicode("\u2764")/* , ReactionEmoji.unicode("\u1F386") */ }));
		// this.middlewareBefore.add(new VoiceGuard(this.guildId, client,
		// this.audioProvider, this.surveys, this.playerEventHandler));
		this.middlewareBefore.add(new MusicRecommendation(this.middlewareConfig));
		this.responseSet = new Megumin(this.middlewareConfig, this.localTasks);

		// ########## TASKS ##########
		// // TODO: Move to a dedicated file
		// this.localTasks.addTask(new RefinedTimerTask(null, Long.valueOf(Time.DAY),
		// 		Time.getNext(3, 0, 0).getTime(), this.localTasks) {

		// 	@Override
		// 	public void runTask() {
		// 		System.out.println("Executing CleanUp task!");
		// 		try {
		// 			MessageChannel channelRef = (MessageChannel) globalProxy.getClient().getGuildById(guildId)
		// 					.flatMap(guild -> guild.getChannelById(ChannelID.MEGUMIN)).block();
		// 			Message lastMessage = channelRef.getLastMessage().block();
		// 			List<Message> messages = channelRef.getMessagesBefore(lastMessage.getId()).collectList().block();
		// 			messages.add(0, lastMessage);

		// 			final String cleanUpFinish = "Täglicher CleanUp beendet! :sparkles:";

		// 			Message infoMessage = channelRef.createMessage("Täglicher CleanUp wird ausgeführt! :wastebasket:").block();

		// 			for (Message message : messages) {
		// 				final String content = message.getContent();
		// 				switch (content) {
		// 					case cleanUpFinish:
		// 					case AudioEventHandler.MUSIC_STOPPED:
		// 						message.delete().block();
		// 						break;
		// 				}
		// 			}

		// 			infoMessage.edit(edit -> edit.setContent(cleanUpFinish)).block();
		// 			System.out.println("CleanUp task finished!");
		// 		} catch (Exception e) {
		// 			System.out.println("CleanUp task failed!");
		// 			e.printStackTrace();
		// 		}
		// 	}

		// });
	}

	/**
	 * Allow this message to be further analyzed, if its author is NOT this bot.
	 * 
	 * @param msgEvent The message event
	 * @param client   This client
	 */
	public void onMessageReceived(MessageCreateEvent msgEvent) {
		if (!msgEvent.getMessage().getAuthor().get().getId().equals(this.globalProxy.getClient().getSelfId())) {

			boolean shouldContinue = true;
			DecompiledMessage msg = new DecompiledMessage(msgEvent);
			if(msg.isBroken()){
				return;
			}

			// ########## MIDDLEWARE BEFORE ##########
			for (Middleware middleware : this.middlewareBefore) {
				try {
					shouldContinue = middleware.acceptEvent(msg);
				} catch (Exception e) {
					System.out.println("Error while using middleware: '" + middleware + "'");
					System.out.println(e);
				}
				if (!shouldContinue) {
					System.out.println(middleware + " canceled event digest");
					return;
				}
			}

			// ########## RESPONSE TYPE ##########
			shouldContinue = this.responseSet.acceptEvent(msg);

			if (!shouldContinue) {
				System.out.println("ResponseType canceled event digest");
				return;
			}
		}
	}

	public void onPurge(){
		try{
			this.responseSet.purge();
		}
		catch(Exception e){
			System.out.println("Failed to purge guild session, continuing...");
		}
	}

	public void setHelloMessage(Message msg){
		this.middlewareConfig.helloMessage = msg;
	}

	public void onVoiceStateEvent(VoiceStateUpdateEvent event){
		try{
			this.responseSet.onVoiceStateEvent(event);
		}
		catch(Exception e){
			System.out.println("Something went wrong while evaluating VoiceStateUpdateEvent for guild "+this.guild.getName());
		}
	}

	public HashMap<Snowflake, HashSet<Snowflake>> getVoiceSubscriptions(){
		return this.middlewareConfig.voiceSubscriberMap;
	}

	public boolean hasUser(Snowflake userId){
		try{
			return this.guild.getMemberById(userId).block() != null;
		}
		catch(Exception e){
			return false;
		}
	}

	public final Guild getGuild(){
		return this.guild;
	}

	public final void onVoiceChannelDeleted(VoiceChannelDeleteEvent event){
		this.middlewareConfig.voiceSubscriberMap.remove(event.getChannel().getId());
	}

	public final void onUserRemoved(MemberLeaveEvent event){
		this.middlewareConfig.voiceSubscriberMap.forEach((channelId, set) -> {
			set.remove(event.getUser().getId());
		});
	}
}
