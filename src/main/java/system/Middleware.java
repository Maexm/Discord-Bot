package system;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.object.presence.Presence;
import discord4j.rest.http.client.ClientException;
import discord4j.voice.AudioProvider;
import musicBot.MusicWrapper;
import reactor.core.publisher.Mono;
import security.SecurityLevel;
import start.RuntimeVariables;
import start.GlobalDiscordProxy;
import survey.Survey;


public abstract class Middleware {

	protected DecompiledMessage msg;
	protected final MiddlewareConfig config;
	protected final Predicate<Message> mayAccept;
	protected final boolean quietError;

	public Middleware(MiddlewareConfig config, boolean quietError) {
		this(config, quietError, config.UNSAFE_mayAccept());
	}

	public Middleware(MiddlewareConfig config, boolean quietError, Predicate<Message> mayAccept){
		this.config = config;
		this.mayAccept = mayAccept;
		this.quietError = quietError;
		
	}

	/**
	 * Accepts a MessageCreateEvent and saves the string content of the message in
	 * msgContent and the whole message object in msgObject
	 * 
	 * @param messageEvent
	 */
	public final boolean acceptEvent(final DecompiledMessage message) {
		this.msg = message;
		if(!this.mayAccept.test(message.getMessageObject())){
			return true; // Skip if this Middleware may not handle event
		}
		boolean ret = true;

		// Start redirecting
		try {
			ret = this.handle();
			// ERROR HANDLING
		} catch (Exception e) {
			e.printStackTrace();
				try {
					System.out.println("Message '"+this.getMessage().getContent()+"' in guild "+this.getGuildSecureName()+" caused an error!");
					if(!this.quietError){
						this.sendAnswer("seltsam...das hat bei mir einen Fehler ausgelöst!");
					}
				} catch (Exception e2) {
					System.out.println("Cannot send messages at all!");
				}
		}
		return ret;
	}

	protected abstract boolean handle();
	
	protected final DecompiledMessage getMessage(){
		return this.msg;
	}

	protected final MusicWrapper getMusicWrapper(){
		return this.config.musicWrapper;
	}

	protected final GlobalDiscordProxy getGlobalProxy(){
		return this.config.globalProxy;
	}

	protected final boolean authorIsGuildOwner(){
		if(this.isPrivate()){
			return false;
		}
		return this.getMessage().getUser().getId().equals(this.getGuild().getOwnerId());
	}

	protected final Guild getGuild(){
		try{
			return this.getGuildByID(this.getGuildId());
		}
		catch(Exception e){
			return null;
		}
	}

	public final Snowflake getGuildId(){
		return this.config.guildId;
	}

	protected final List<Guild> getGuildByName(String name){
		List<Guild> guilds = this.getClient().getGuilds().buffer().blockFirst();

		if(guilds == null){
			return new ArrayList<>();
		}

		guilds.removeIf(guild -> !guild.getName().equals(name));

		return guilds;
	}

	protected final List<Guild> parseGuild(final String identifier){
		List<Guild> ret = new ArrayList<>();

		try{
			Snowflake id = Snowflake.of(identifier);
			Guild guildById = this.getGuildByID(id);
			// Guild not found -> return try guild by name;
			if(guildById == null){
				throw new NullPointerException();
			}

			ret.add(guildById);
		}
		catch(Exception e){
			// guild by id did not work -> try to find guild by name
			List<Guild> guildsByName = this.getGuildByName(identifier);
			ret = guildsByName;
		}

		return ret;
	}

	protected final List<GuildChannel> getChannelByName(String name){
		List<GuildChannel> channels = new ArrayList<>();
		if(this.isPrivate()){
			return channels;
		}

		channels = this.getGuild().getChannels().buffer().blockFirst();

		if(channels == null){
			return new ArrayList<>();
		}

		channels.removeIf(channel -> !channel.getName().equals(name));

		return channels;
	}

	protected final List<GuildChannel> parseChannel(final String identifier){
		List<GuildChannel> ret = new ArrayList<>();
		if(this.isPrivate()){
			return ret;
		}

		try{
			Snowflake id = Snowflake.of(identifier);
			GuildChannel channelById = this.getChannelById(id);
			// Channel not found -> return try guild by name;
			if(channelById == null){
				throw new NullPointerException();
			}

			ret.add(channelById);
		}
		catch(Exception e){
			// channel by id did not work -> try to find channel by name
			List<GuildChannel> channelsByName = this.getChannelByName(identifier);
			ret = channelsByName;
		}

		return ret;
	}

	protected final List<MessageChannel> parseMsgChannel(final String identifier){
		List<MessageChannel> ret = new ArrayList<>();
		List<GuildChannel> channels = this.parseChannel(identifier);

		channels.forEach(channel -> {
			if(channel.getType().equals(Type.GUILD_TEXT)){
				ret.add((MessageChannel) channel);
			}
		});

		return ret;
	}

	protected final Role getRoleById(Snowflake id){
		try{
			return this.getGuild().getRoleById(id).block();
		}
		catch(Exception e){
			return null;
		}
	}

	protected final List<Role> getRoleByName(final String name){
		List<Role> roles = new ArrayList<>();
		if(this.isPrivate()){
			return roles;
		}

		roles = this.getGuild().getRoles().buffer().blockFirst();

		if(roles == null){
			return new ArrayList<>();
		}

		roles.removeIf(role -> !role.getName().equals(name));

		return roles;
	}

	protected final List<Role> parseRole(final String identifier){
		List<Role> ret = new ArrayList<>();
		if(this.isPrivate()){
			return ret;
		}

		try{
			Snowflake id = Snowflake.of(identifier);
			Role roleById = this.getRoleById(id);
			if(roleById == null){
				throw new NullPointerException();
			}

			ret.add(roleById);
		}
		catch(Exception e){
			List<Role> rolesByName = this.getRoleByName(identifier);
			ret = rolesByName;
		}

		return ret;
	}


	/**
	 * Returns the author of this message as Member instance.
	 * 
	 * @return The author of the message
	 */
	protected final Member getMessageAuthorMember() {
		if (!this.isPrivate()) {
			return this.getMessage().getEvent().getMember().orElse(null);
		}
		return null;
	}

	protected final User getMessageAuthor(){
		return this.getMessage().getMessageObject().getAuthor().orElse(null);
	}

	/**
	 * Returns the channel, where the received message was sent to
	 * 
	 * @return Channel associated to the received message as MessageChannel
	 */
	protected final MessageChannel getMessageChannel() {
		return this.getMessage().getMessageObject().getChannel().block();
	}

	/**
	 * Returns the corresponding MessageCreateEvent
	 * 
	 * @return the event
	 */
	protected final MessageCreateEvent getMessageEvent() {
		return this.getMessage().getEvent();
	}

	/**
	 * Returns the audio provider, linked to LavaPlayer
	 * 
	 * @return the audio provider
	 */
	protected final AudioProvider getAudioProvider() {
		return this.getMusicWrapper().getAudioProvider();
	}

	/**
	 * Returns the current bot's client object
	 * 
	 * @return This bot as client
	 */
	public final GatewayDiscordClient getClient() {
		return this.config.globalProxy.getClient();
	}

	/**
	 * The voice state of the author
	 * 
	 * @return Authors voice state
	 */
	protected final VoiceState getAuthorVoiceState() {
		return this.getMessageAuthorMember().getVoiceState().block();
	}

	/**
	 * Returns whether or not the author is connected to voice in this guild
	 * 
	 * @return Author connected to voice (true) or not (false)
	 */
	protected final boolean isAuthorVoiceConnectedGuildScoped() {
		return this.getAuthorVoiceState() != null && this.getAuthorVoiceState().getGuildId().equals(this.getGuildId());
	}

	protected final boolean isAuthorVoiceConnectedVerbose() {
		return this.getAuthorVoiceState() != null;
	}

	/**
	 * Returns the instance of the voice channel, the author is currently in
	 * 
	 * @return The voice channel, the author is currently connected to
	 */
	protected final VoiceChannel getAuthorVoiceChannel() {
		return this.getAuthorVoiceState().getChannel().block();
	}

	/**
	 * Returns the voice channel this bot is currently connected to Returns null, if
	 * not connected at all
	 * 
	 * @return The voice channel, the bot is currently connected to
	 */
	protected final VoiceChannel getMyVoiceChannel() {
		try{
			return this.getMyVoiceChannelAsync().block();
		}
		catch(Exception e){
			return null;
		}
		
	}
	protected final Mono<VoiceChannel> getMyVoiceChannelAsync() {
		return this.getClient().getSelf()
			.flatMap(self -> self.asMember(this.getGuildId()))
			.flatMap(member -> member.getVoiceState())
			.flatMap(voiceState -> voiceState != null ? voiceState.getChannel() : null);
	}

	/**
	 * Indicated whether or not this bot is connected to voice
	 * 
	 * @return
	 */
	protected final boolean isVoiceConnected() {
		return this.getMyVoiceChannel() != null;
	}

	/**
	 * Get private channel object of author
	 * 
	 * @return Private channel object of author
	 */
	protected final PrivateChannel getMessageAuthorPrivateChannel() {
		return this.getMessage().getUser().getPrivateChannel().block();
	}

	/**
	 * Returns whether or not the received message comes from a private channel
	 * 
	 * @return True if message comes from private channel, false otherwise
	 */
	protected final boolean isPrivate() {
		return this.getGuildId() == null;
	}

	protected final long getResponseTime() {
		return 0l;//this.getClient().getResponseTime();
	}

	protected final ApplicationInfo getAppInfo() {
		return this.getClient().getApplicationInfo().block();
	}

	protected final GuildEmoji getEmoji(Snowflake emojiID) {
		List<GuildEmoji> emojiList = this.getEmojiList();
		for (GuildEmoji emoji : emojiList) {
			if (emoji.getId().equals(emojiID)) {
				return emoji;
			}
		}
		return null;
	}

	protected final String getEmojiFormat(Snowflake emojiID) {
		GuildEmoji emoji = this.getEmoji(emojiID);
		if (emoji != null) {
			return emoji.asFormat();
		}
		return "";
	}

	protected final List<GuildEmoji> getEmojiList() {
		return this.getGuild().getEmojis().buffer().blockFirst();
	}

	protected final Guild getGuildByID(Snowflake guildID) {
		return this.getClient().getGuildById(guildID).block();
	}

	protected final GuildMessageChannel getMsgChannelById(Snowflake channelId) {
		GuildChannel temp = this.getChannelById(channelId);
		return temp.getType().equals(Type.GUILD_TEXT) ? (GuildMessageChannel) temp : null;
    }

	protected final GuildChannel getChannelById(Snowflake channelId){
		try{
			return this.getGuild().getChannelById(channelId).block();
		}
		catch(Exception e){
			return null;
		}
	}

    protected final Member getMember(Snowflake userId, Snowflake guildId){
        return this.getGuildByID(guildId).getMemberById(userId).block();
    }

    protected final Presence getMemberPresence(Snowflake userId, Snowflake guildId){
        return this.getMember(userId, guildId).getPresence().block();
	}
	
	protected final User getOwner(){
		return this.getAppInfo().getOwner().block();
	}

	public final Mono<String> getOwnerMentionAsync(){
		return this.getAppInfo().getOwner().map(owner -> owner.getMention());
	}

	public final MessageChannel getSystemChannel(){
		return this.getGuild().getSystemChannel().onErrorResume(err -> null).block();
	}

	public final MessageChannel getPsaChannel(boolean force){
		if(!force && !this.getConfig().psaNote){
			return null;
		}
		return this.config.announcementChannelId != null ? this.getMsgChannelById(this.config.announcementChannelId) : this.getSystemChannel();
	}

	public final MessageChannel getUpdateChannel(boolean force){
		if(!force && !this.getConfig().updateNote){
			return null;
		}
		return this.config.announcementChannelId != null ? this.getMsgChannelById(this.config.announcementChannelId) : this.getSystemChannel();
	}

	public final List<GuildChannel> getVoiceChannels(){
		return this.getGuild().getChannels()
		.filter(channel -> channel.getType().compareTo(Channel.Type.GUILD_VOICE) == 0)
		.buffer()
		.next()
		.block();
	}

	public String getGuildSecureName(){
		try{
			return this.getGuild().getName();
		}
		catch(Exception e){
			return "ERROR OR PRIVATE";
		}
	}

	public MiddlewareConfig getConfig(){
		return this.config;
	}

	protected GuildHandler getHandler(){
		if(this.isPrivate()){
			return null;
		}
		return this.getGlobalProxy().getGuildHandler(this.getGuildId());
	}

	protected GuildHandler getHandler(Snowflake guildId){
		return this.getGlobalProxy().getGuildHandler(guildId);
	}

	protected ResponseType getResponseType(Snowflake guildId){
		try{
			return this.getHandler(guildId).getResponseType();
		}
		catch(NullPointerException e){
			return null;
		}
	}

	// ########## INTERACTIVE METHODS ##########

	/**
	 * Sends a response to the same channel where the received message came from and
	 * response the message instance
	 * 
	 * @param message The message to be sent
	 * @return The instance of the message that was sent
	 */
	protected final Message sendInSameChannel(String message) {
		return this.getMessageChannel().createMessage(RuntimeVariables.getInstance().getAnsPrefix() + message + RuntimeVariables.getInstance().getAnsSuffix()).block();
	}

	/**
	 * Deletes a message
	 * 
	 * @param message The message to be deleted
	 */
	protected final void deleteMessage(Message message) {
		message.delete().block();
	}

	/**
	 * Deletes the message that was received, if possible
	 */
	protected final void deleteReceivedMessage() {
		if(!this.isPrivate()){
			try{
				this.getMessage().getMessageObject().delete().block();
			}
			catch(ClientException e){
				if(e.getStatus().code()/100 != 4){
					throw e;
				}
			}
			
		}
	}

	/**
	 * Sends a direct answer to the message author with mention "[MENTION], message"
	 * 
	 * @param message The message to be applied after mention
	 * @return The instance of the message that was sent
	 */
	protected final Message sendAnswer(String message) {
		return this.sendInSameChannel(this.getMessage().getUser().getMention() + ", " + message);
	}

	/**
	 * Sends a message to a dedicated channel in a guild
	 * 
	 * @param message
	 * @param channelID
	 * @param guildID
	 * @return
	 */
	public final Message sendInChannel(String message, Snowflake channelID) {
		MessageChannel channel = this.getMsgChannelById(channelID);
		return channel.createMessage(RuntimeVariables.getInstance().getAnsPrefix()+message + RuntimeVariables.getInstance().getAnsSuffix()).block();
	}

	/**
	 * Sends a private answer to the message author
	 * 
	 * @param message The message to be sent privately
	 * @return The instance of the message that was sent
	 */
	protected final Message sendPrivateAnswer(String message) {
		return this.getMessageAuthorPrivateChannel().createMessage(RuntimeVariables.getInstance().getAnsPrefix() + message + RuntimeVariables.getInstance().getAnsSuffix()).block();
	}

	/**
	 * Joins a specific voice channel with a specific audio provider. An
	 * NullPointerException is thrown, if the given channel is null
	 * 
	 * @param channel
	 * @param audioProvider
	 */
	protected final void joinVoiceChannel(VoiceChannel channel, AudioProvider audioProvider) {
		if (channel == null) {
			throw new NullPointerException("Cannot join null");
		} else if (audioProvider == null) {
			throw new NullPointerException("Cannot use null as audioProvider");
		} else {
			final String CHANNEL_NAME = channel.getName();
			if (this.isVoiceConnected() && this.getMyVoiceChannel().getId().equals(channel.getId())) {
				System.out.println("Already connected to same channel '" + CHANNEL_NAME + "'!");
			}
			System.out.println("Trying to connect to voice channel '" + CHANNEL_NAME + "'");
			/*this.voiceConnection = */channel.join(spec -> spec.setProvider(audioProvider)).log().block(Duration.ofSeconds(30l));
			//channel.sendConnectVoiceState(false, false).block(Duration.ofSeconds(30l));
			System.out.println("Connected to voice channel '" + CHANNEL_NAME + "'");
		}
	}

	/**
	 * Leaves voice channel, if bot is connected. Catches and prints any exceptions
	 * that may occur.
	 */
	public final void leaveVoiceChannel() {
		// if(!this.isVoiceConnected()){
		// 	System.out.println("Cannot leave voice channel, when not connected!");
		// 	return;
		// }
		try {
			this.getClient().getVoiceConnectionRegistry().disconnect(this.config.guildId).doOnTerminate(() -> {System.out.println("Left voice channel");}).subscribe();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/**
	 * Deletes all messages from a channel
	 * 
	 * @param channelID
	 * @param guildID
	 * @return The amount of deleted messages
	 */
	protected final int deleteAllMessages(Snowflake channelID) {
		int ret = 0;
		MessageChannel megChannel = this.getMsgChannelById(channelID);
		Message lastMessage = megChannel.getLastMessage().block();
		List<Message> messages = megChannel.getMessagesBefore(lastMessage.getId()).collectList().block();

		this.deleteMessage(lastMessage);
		ret++;
		for (Message message : messages) {
			this.deleteMessage(message);
			ret++;
		}
		return ret;
	}

	/**
	 * Deletes a certain amount of messages from a channel
	 * 
	 * @param channelID
	 * @param guildID
	 * @param amount
	 * @return The amount of deleted messages
	 */
	protected final int deleteMessages(Snowflake channelID, int amount) {
		int ret = 0;
		if (amount > 0) {
			MessageChannel channel = this.getMsgChannelById(channelID);
			for (int i = 0; i < amount; i++) {
				Message lastMessage = channel.getLastMessage().block();
				this.deleteMessage(lastMessage);
				ret++;
			}
		}
		return ret;
	}

	protected final Message sendMessageToOwner(String msg){
		final String MESSAGE = RuntimeVariables.getInstance().getAnsPrefix() + msg + RuntimeVariables.getInstance().getAnsSuffix();
		return this.getOwner().getPrivateChannel()
			.flatMap(channel -> channel.createMessage(MESSAGE))
			.block();
	}

	public final void globalAnnounce(final String content, final boolean update){
		List<MessageChannel> channels = update ? this.getGlobalProxy().getGlobalUpdateChannels(false) : this.getGlobalProxy().getGlobalPsaChannels(false);
		for(MessageChannel channel : channels){
			try{
				channel.createMessage(content).block();
			}
			catch(Exception e){
				System.out.println("Failed to send psa message in a guild");
				e.printStackTrace();
			}
		}
	}

	protected final String getLocalHomeTown(){
		return this.getConfig().homeTown != null && !this.getConfig().homeTown.equals("") ? this.getConfig().homeTown : RuntimeVariables.getInstance().getHometown();
	}

	// TECHNICAL METHODS

	protected final boolean surveyExistsUserScoped(String keyword, Snowflake userId){
		return this.getGlobalProxy().getSurveyForKeyUserScoped(keyword, userId) != null;
	}


	protected final Survey getSurveyForKeyUserScoped(String keyword, Snowflake userId) {
		return this.getGlobalProxy().getSurveyForKeyUserScoped(keyword, userId);
	}

	protected final Survey getSurveyForKeyVerbose(String keyword){
		return this.getGlobalProxy().getSurveyForKeyVerbose(keyword);
	}

	protected final boolean surveyExistsVerbose(String keyword){
		return this.getSurveyForKeyVerbose(keyword) != null;
	}

	protected final ArrayList<Survey> getSurveyListVerbose(){
		return this.getGlobalProxy().getSurveysVerbose();
	}

	protected final boolean hasPermission(SecurityLevel required){
		return this.config.getSecurityProvider().hasPermission(this.getMessage().getUser(), required);
	}
}
