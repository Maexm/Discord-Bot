package system;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gson.Gson;

import config.FileManager;
import config.GuildConfig;
import config.GuildConfig.VoiceSubscription;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel.Type;
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

	public GuildHandler(final Snowflake guildId, final GlobalDiscordProxy globalProxy, final MusicWrapper musicWrapper) {

		// Initialize objects with defaults
		this.middlewareBefore = new ArrayList<Middleware>();
		this.localTasks = new TaskManager<>(true);
		this.guildId = guildId;
		this.globalProxy = globalProxy;

		this.musicWrapper = musicWrapper;
		this.middlewareConfig = new MiddlewareConfig(this.guildId, this.musicWrapper, this.globalProxy, null, new HashMap<>());
		this.guild = this.guildId != null ? this.globalProxy.getClient().getGuildById(this.guildId).block() : null;

		// Load from save file
		try{
			this.loadConfig();
		}catch(Exception e){
		}
		

		// ########## RESPONSE SETS ##########
		// TODO: Shorten this hell
		//this.middlewareBefore.add(new Logger(this.middlewareConfig));
		this.middlewareBefore.add(new RoleFilter(this.middlewareConfig,
				msg -> RuntimeVariables.IS_DEBUG
						&& msg.getContent().toLowerCase().startsWith(RuntimeVariables.MESSAGE_PREFIX.toLowerCase()),
				SecurityLevel.DEV, "meine Dienste sind im Preview Modus nicht verfügbar!"));

		this.middlewareBefore.add(new HelpSection(this.middlewareConfig));
		this.middlewareBefore.add(
				new AutoReact(this.middlewareConfig, msg -> {
					final String[] expressions = { "explosion", "kaboom", "bakuhatsu", "bakuretsu", "ばくれつ", "爆裂", "ばくはつ",
							"爆発", "explode", "feuerwerk", "böller", "explosiv", "detonation", "explodier", "hanabi", "はなび", "花火"};
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
		//this.middlewareBefore.add(new MusicRecommendation(this.middlewareConfig));
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
			System.out.println("Purge completed!");
		}
		catch(Exception e){
			System.out.println("Failed to purge guild session, continuing...");
			e.printStackTrace();
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

	public final MiddlewareConfig getMiddlewareConfig(){
		return this.middlewareConfig;
	}

	public final void onVoiceChannelDeleted(VoiceChannelDeleteEvent event){
		this.middlewareConfig.voiceSubscriberMap.remove(event.getChannel().getId());
	}

	public final void onUserRemoved(MemberLeaveEvent event){
		this.middlewareConfig.voiceSubscriberMap.forEach((channelId, set) -> {
			set.remove(event.getUser().getId());
		});
	}

	public final void onTextChannelDeleted(TextChannelDeleteEvent event){
		if(this.middlewareConfig.announcementChannelId != null && this.middlewareConfig.announcementChannelId.equals(event.getChannel().getId())){
			this.middlewareConfig.announcementChannelId = null;
		}
		if(this.middlewareConfig.musicWrapper.getMusicChannelId() != null && this.middlewareConfig.musicWrapper.getMusicChannelId().equals(event.getChannel().getId())){
			this.middlewareConfig.musicWrapper.setMusicChannelId(null);
		}
	}

	public final void onRoleDeleted(RoleDeleteEvent event){
		if(this.middlewareConfig.getSecurityProvider().specialRoleId != null && this.middlewareConfig.getSecurityProvider().specialRoleId.equals(event.getRoleId())){
			this.middlewareConfig.getSecurityProvider().specialRoleId = null;
		}
	}

	public final ResponseType getResponseType(){
		return this.responseSet;
	}

	public final boolean isVoiceChannel(Snowflake id){
		return this.getGuild().getChannelById(id)
		.onErrorResume(err -> null)
		.map(channel -> channel != null && channel.getType().equals(Type.GUILD_VOICE))
		.block();
	}

	public final boolean isTextChannel(Snowflake id){
		return this.getGuild().getChannelById(id)
		.onErrorResume(err -> null)
		.map(channel -> channel != null && channel.getType().equals(Type.GUILD_TEXT))
		.block();
	}

	public final boolean isRole(Snowflake id){
		return this.getGuild().getRoleById(id)
		.onErrorResume(err -> null)
		.map(role -> role != null)
		.block();
	}

	public final void loadConfig(){
		File configFile = new File("guildConfig.json");
		String rawConfig = FileManager.read(configFile);
		if(rawConfig == null){
			System.out.println("Warning: No guild config file found: "+configFile.getAbsolutePath());
		}
		else{
			// Read config file and find correct config for this guild
			Gson gson = new Gson();
			GuildConfig[] allGuildConfigs = gson.fromJson(rawConfig, GuildConfig[].class);
			GuildConfig  guildConfig = null;
			for(GuildConfig config : allGuildConfigs){
				if(Snowflake.of(config.guildId).equals(this.guildId)){
					guildConfig = config;
					break;
				}
			}
			// Apply values from config, if guild was found in config file
			if(guildConfig != null){
				// ########## MUSIC TEXTCHANNEL ##########
				this.musicWrapper.setMusicChannelId(null);
				if(guildConfig.musicChannelId != 0){
					Snowflake musicChannelId = Snowflake.of(guildConfig.musicChannelId);
					if(this.isTextChannel(musicChannelId)){
						this.musicWrapper.setMusicChannelId(musicChannelId);
					}
				}
				// ########## VOICE SUBSCRIPTIONS ##########
				this.getVoiceSubscriptions().clear();
				if(guildConfig.voiceSubscriptions != null){
					for(VoiceSubscription voiceSubscription : guildConfig.voiceSubscriptions){
						// Ignore broken voice subscriptions (empty values)
						if(voiceSubscription.voiceChannelId == 0 || voiceSubscription.userIds == null){
							continue;
						}
						
						// Check users if voiceId corresponds to a valid voice channel
						Snowflake voiceId = Snowflake.of(voiceSubscription.voiceChannelId);
						if(this.isVoiceChannel(voiceId)){
							//  Put voiceId into map, if not already present in map
							if(!this.getVoiceSubscriptions().containsKey(voiceId)){
								this.getVoiceSubscriptions().put(voiceId, new HashSet<>());
							}
							// Check and add subscribers (users)
							for(long rawUserId : voiceSubscription.userIds){
								// Skip empty user id
								if(rawUserId == 0){
									continue;
								}
								Snowflake userId = Snowflake.of(rawUserId);
								// Add user if userId is present in this guild
								if(this.hasUser(userId)){
									this.getVoiceSubscriptions().get(voiceId).add(userId);
								}
							}
						}
					}
				}

				// ########## ANNOUNCEMENT CHANNEL ID ##########
				this.middlewareConfig.announcementChannelId = null;
				if(guildConfig.announcementChannelId != 0){
					Snowflake announcementChannelId = Snowflake.of(guildConfig.announcementChannelId);
					if(this.isTextChannel(announcementChannelId)){
						this.middlewareConfig.announcementChannelId = announcementChannelId;
					}
				}

				// ########## RECEIVE UPDATE NOTIFICATION ##########
				this.middlewareConfig.updateNote = guildConfig.updateNote;

				// ########## RECEIVE PSA NOTIFICATION ##########
				this.middlewareConfig.psaNote = guildConfig.psaNote;

				// ########## SPECIAL ROLE ID ##########
				this.middlewareConfig.getSecurityProvider().specialRoleId= null;
				if(guildConfig.specialRoleId != 0){
					Snowflake specialRoleId = Snowflake.of(guildConfig.specialRoleId);
					if(this.isRole(specialRoleId)){
						this.middlewareConfig.getSecurityProvider().specialRoleId = specialRoleId;
					}
				}

				// ########## HOME TOWN ##########
				this.middlewareConfig.homeTown = null;
				if(guildConfig.homeTown != null && !guildConfig.homeTown.equals("")){
					this.middlewareConfig.homeTown = guildConfig.homeTown;
				}
			}
		}
	}
}
