package system;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gson.Gson;

import config.FileManager;
import config.GuildConfig;
import config.GuildConfig.VoiceSubscription;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Permission;
import musicBot.MusicWrapper;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import security.SecurityLevel;
import start.RuntimeVariables;
import util.Pair;
import start.GlobalDiscordProxy;

public final class GuildHandler {

	private ResponseType responseSet;
	private final ArrayList<Middleware> middlewareBefore;
	private final ArrayList<Middleware> middlewareAfter;
	private final Snowflake guildId;
	private final TaskManager<RefinedTimerTask> localTasks;
	private final MiddlewareConfig middlewareConfig;
	private final GlobalDiscordProxy globalProxy;
	private final MusicWrapper musicWrapper;
	private final Guild guild;

	public GuildHandler(final Snowflake guildId, final GlobalDiscordProxy globalProxy, final MusicWrapper musicWrapper) {

		// Initialize objects with defaults
		this.middlewareBefore = new ArrayList<Middleware>();
		this.middlewareAfter = new ArrayList<Middleware>();
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
			e.printStackTrace();
		}
		

		// ########## RESPONSE SETS ##########
		// TODO: Shorten this hell
		//this.middlewareBefore.add(new Logger(this.middlewareConfig));
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
		this.middlewareBefore.add(new RoleFilter(this.middlewareConfig,
				msg -> RuntimeVariables.isDebug()
						&& msg.getContent().toLowerCase().startsWith(RuntimeVariables.getInstance().getCommandPrefix().toLowerCase()),
				SecurityLevel.DEV, "meine Dienste sind im Preview Modus nicht verfügbar!"));
		this.middlewareBefore.add(new HelpSection(this.middlewareConfig));
		// this.middlewareBefore.add(new VoiceGuard(this.guildId, client,
		// this.audioProvider, this.surveys, this.playerEventHandler));
		//this.middlewareBefore.add(new MusicRecommendation(this.middlewareConfig));
		this.responseSet = new Megumin(this.middlewareConfig, this.localTasks);
		this.middlewareAfter.add(new MusicInfoPusher(this.middlewareConfig));
	}

	/**
	 * Allow this message to be further analyzed, if its author is NOT this bot.
	 * 
	 * @param msgEvent The message event
	 * @param client   This client
	 */
	public void onMessageReceived(DecompiledMessage msg) {
		if (msg.getUser() != null && !msg.getUser().getId().equals(this.globalProxy.getClient().getSelfId())) {

			boolean shouldContinue = true;

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

			// ########## MIDDLEWARE AFTER ##########
			for (Middleware middleware : this.middlewareAfter) {
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
		this.globalProxy.saveAllGuilds();
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

	public HashMap<Snowflake, Pair<Calendar, HashSet<Snowflake>>> getVoiceSubscriptions(){
		return this.middlewareConfig.voiceSubscriberMap;
	}

	public boolean hasUser(Snowflake userId){
		return this.getMember(userId) != null;
	}

	public Member getMember(Snowflake userId){
		try{
			return this.guild.getMemberById(userId).block();
		}
		catch(Exception e){
			return null;
		}
	}
	/**
	 * Checks if a user by the give id has the given permission. Also checks if user is present in this guild and returns false, if user is not guild member
	 * @param userId
	 * @param permission
	 * @return
	 */
	public boolean hasPermission(Snowflake userId, Permission permission){
		Member member = this.getMember(userId);

		if(this.guild != null && this.guild.getOwnerId().equals(userId)){
			return true;
		}

		if(member == null){
			return false;
		}

		List<Role> roles = member.getRoles().buffer().blockFirst();
		if(roles == null){
			return false;
		}
		for(Role role : roles){
			if(role.getPermissions().contains(permission)){
				return true;
			}
		}

		return false;
	}

	public final Guild getGuild(){
		return this.guild;
	}

	public final MiddlewareConfig getMiddlewareConfig(){
		return this.middlewareConfig;
	}

	public final void onVoiceChannelDeleted(VoiceChannelDeleteEvent event){
		this.middlewareConfig.voiceSubscriberMap.remove(event.getChannel().getId());
		this.globalProxy.saveAllGuilds();
	}

	public final void onUserRemoved(MemberLeaveEvent event){
		this.middlewareConfig.voiceSubscriberMap.forEach((channelId, pair) -> {
			pair.value.remove(event.getUser().getId());
		});
		this.globalProxy.saveAllGuilds();
	}

	public final void onTextChannelDeleted(TextChannelDeleteEvent event){
		if(this.middlewareConfig.announcementChannelId != null && this.middlewareConfig.announcementChannelId.equals(event.getChannel().getId())){
			this.middlewareConfig.announcementChannelId = null;
		}
		if(this.middlewareConfig.musicWrapper.getMusicChannelId() != null && this.middlewareConfig.musicWrapper.getMusicChannelId().equals(event.getChannel().getId())){
			this.middlewareConfig.musicWrapper.setMusicChannelId(null);
		}
		this.globalProxy.saveAllGuilds();
	}

	public final void onRoleDeleted(RoleDeleteEvent event){
		if(this.middlewareConfig.getSecurityProvider().specialRoleId != null && this.middlewareConfig.getSecurityProvider().specialRoleId.equals(event.getRoleId())){
			this.middlewareConfig.getSecurityProvider().specialRoleId = null;
			this.globalProxy.saveAllGuilds();
		}
	}

	public final ResponseType getResponseType(){
		return this.responseSet;
	}

	public final boolean isVoiceChannel(Snowflake id){
		try{
			return this.getGuild().getChannelById(id)
			.onErrorResume(err -> null)
			.map(channel -> channel != null && channel.getType().equals(Type.GUILD_VOICE))
			.block();
		}
		catch(Exception e){
			return false;
		}
		
	}

	public final boolean isTextChannel(Snowflake id){
		try{
			return this.getGuild().getChannelById(id)
			.onErrorResume(err -> null)
			.map(channel -> channel != null && channel.getType().equals(Type.GUILD_TEXT))
			.block();
		}
		catch(Exception e){
			return false;
		}
	}

	public final boolean isRole(Snowflake id){
		try{
			return this.getGuild().getRoleById(id)
			.onErrorResume(err -> null)
			.map(role -> role != null)
			.block();
		}
		catch(Exception e){
			return false;
		}
	}

	public final void loadConfig(){
		File configFile = new File("./botConfig/guildConfig.json");
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
				if(guildConfig.musicChannelId != null && guildConfig.musicChannelId != 0){
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
						if(voiceSubscription.voiceChannelId == null || voiceSubscription.voiceChannelId == 0 || voiceSubscription.userIds == null){
							continue;
						}
						
						// Check users if voiceId corresponds to a valid voice channel
						Snowflake voiceId = Snowflake.of(voiceSubscription.voiceChannelId);
						if(this.isVoiceChannel(voiceId)){
							//  Put voiceId into map, if not already present in map
							if(!this.getVoiceSubscriptions().containsKey(voiceId)){
								this.getVoiceSubscriptions().put(voiceId, new Pair<>(null, new HashSet<>()));
							}
							// Check and add subscribers (users)
							for(Long rawUserId : voiceSubscription.userIds){
								// Skip empty user id
								if(rawUserId == null || rawUserId == 0){
									continue;
								}
								Snowflake userId = Snowflake.of(rawUserId);
								// Add user if userId is present in this guild
								if(this.hasUser(userId)){
									this.getVoiceSubscriptions().get(voiceId).value.add(userId);
								}
							}
						}
					}
				}

				// ########## ANNOUNCEMENT CHANNEL ID ##########
				this.middlewareConfig.announcementChannelId = null;
				if(guildConfig.announcementChannelId != null && guildConfig.announcementChannelId != 0){
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
				if(guildConfig.specialRoleId != null && guildConfig.specialRoleId != 0){
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

	public GuildConfig createGuildConfig(){
		GuildConfig ret = new GuildConfig();

		ret.announcementChannelId = this.middlewareConfig.announcementChannelId != null ? this.middlewareConfig.announcementChannelId.asLong() : null;
		ret.guildId = this.guildId != null ?  this.guildId.asLong() : null;
		ret.homeTown = this.middlewareConfig.homeTown;
		ret.musicChannelId = this.musicWrapper.getMusicChannelId().isPresent() ? this.musicWrapper.getMusicChannelId().get().asLong() : null;
		ret.psaNote = this.middlewareConfig.psaNote;
		ret.specialRoleId = this.middlewareConfig.getSecurityProvider().specialRoleId != null ? this.middlewareConfig.getSecurityProvider().specialRoleId.asLong() : null;
		ret.updateNote = this.middlewareConfig.updateNote;
		
		// Convert HashMap with Snowflakes into arraylist with VoiceSubscription objects containing longs
		ArrayList<VoiceSubscription> helperSubscriptions = new ArrayList<>();
		this.middlewareConfig.voiceSubscriberMap.forEach((voiceId, subscriberIds) -> {
			VoiceSubscription subscription = new VoiceSubscription();
			subscription.voiceChannelId = voiceId.asLong();

			// Convert subscriber hashSet to long array
			subscription.userIds = new Long[subscriberIds.value.size()];
			int index = 0;
			for(Snowflake subscriberId : subscriberIds.value){
				subscription.userIds[index] = subscriberId.asLong();
				index++;
			}

			helperSubscriptions.add(subscription);
		});
		ret.voiceSubscriptions = helperSubscriptions.toArray(new VoiceSubscription[helperSubscriptions.size()]);

		return ret;
	}
}
