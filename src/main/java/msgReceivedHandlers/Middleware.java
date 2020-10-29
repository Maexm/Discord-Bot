package msgReceivedHandlers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.presence.Presence;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import musicBot.AudioEventHandler;
import survey.Survey;


public abstract class Middleware {

    protected String msgContent;
	protected MessageCreateEvent msgEvent;
	protected Message msgObject;
	protected String msgAuthorName;
	protected User msgAuthorObject;
	protected VoiceConnection voiceConnection = null;
	protected final GatewayDiscordClient client;
	protected String commandSection = "";
	protected String argumentSection = "";
	protected final ArrayList<Survey> surveys;

	// AUDIO
	protected final AudioEventHandler audioEventHandler;

	protected final AudioProvider audioProvider;

	/**
	 * Creates a Middleware object
	 * 
	 * @param client              The discord client object representing this bot
	 * @param audioProvider       The audioProvider for this bot
	 */
	public Middleware(final GatewayDiscordClient client, final AudioProvider audioProvider,
			final ArrayList<Survey> surveys, final AudioEventHandler audioEventHandler) {
		this.client = client;
		this.audioEventHandler = audioEventHandler;
		this.audioProvider = audioProvider;
		this.surveys = surveys;
	}

	/**
	 * Accepts a MessageCreateEvent and saves the string content of the message in
	 * msgContent and the whole message object in msgObject
	 * 
	 * @param messageEvent
	 */
	public final boolean acceptEvent(final MessageCreateEvent messageEvent) {

		boolean fetchSuccess = true;
		boolean ret = true;

		try {
			// Save message and other useful variables for easy access
			this.msgEvent = messageEvent;
			this.msgObject = this.msgEvent.getMessage();
			this.msgContent = this.msgObject.getContent();
			this.msgAuthorObject = this.msgObject.getAuthor().orElse(null);
			this.msgAuthorName = this.msgAuthorObject != null? this.msgAuthorObject.getUsername(): "";
			this.argumentSection = "";
			this.commandSection = "";
		} catch (Exception e) {
			System.out.println("Something went wrong, while accepting event!");
			e.printStackTrace();
			fetchSuccess = false;
		}

		// Start redirecting
		if (fetchSuccess) {
			try {
				ret = this.handle();
				// ERROR HANDLING
			} catch (Exception e) {
				e.printStackTrace();
					try {
						this.sendAnswer("seltsam...das hat bei mir einen Fehler ausgelöst!");
					} catch (Exception e2) {
						System.out.println("Cannot send messages at all!");
					}
				}
			}
		return ret;
	}

    protected abstract boolean handle();

    /**
	 * Returns the message string content
	 * 
	 * @return Received message content as string representation
	 */
	protected final String getMessageContent() {
		return this.msgContent;
	}

	/**
	 * Returns the message as Message object
	 * 
	 * @return Received message as Message object
	 */
	protected final Message getMessageObject() {
		return this.msgObject;
	}

	/**
	 * Returns the user name string of the author of the received message
	 * 
	 * @return Author user name of received message as String
	 */
	protected final String getMessageAuthorName() {
		return this.msgAuthorName;
	}

	/**
	 * Returns the author of the message as User object
	 * 
	 * @return Author of received message as User
	 */
	protected final User getMessageAuthorObject() {
		return this.msgAuthorObject;
	}

	/**
	 * Returns the author of this message as Member instance.
	 * 
	 * @return The author of the message
	 */
	protected final Member getMessageAuthorMember() {
		if (!this.isPrivate()) {
			return this.msgEvent.getMember().orElse(null);
		}
		return null;
	}

	/**
	 * Returns the channel, where the received message was sent to
	 * 
	 * @return Channel associated to the received message as MessageChannel
	 */
	protected final MessageChannel getMessageChannel() {
		return this.msgObject.getChannel().block();
	}

	/**
	 * Returns the guild, where the received message was sent to
	 * 
	 * @return Guild associated to the received message as Guild
	 */
	public final Guild getMessageGuild() {
		return this.msgObject.getGuild().block();
	}

	/**
	 * Returns the order section of a Meg[ORDER] message. Returns an empty string,
	 * if there is no order section.
	 * 
	 * @return The order section of the message
	 */
	protected final String getCommandSection() {
		return this.commandSection;
	}

	/**
	 * Returns the argument section of a Meg[ORDER] [ARGUMENTS] message. Returns an
	 * empty string, if there are no arguments.
	 * 
	 * @return The argument section
	 */
	protected final String getArgumentSection() {
		return this.argumentSection;
	}

	/**
	 * Returns the corresponding MessageCreateEvent
	 * 
	 * @return the event
	 */
	protected final MessageCreateEvent getMessageEvent() {
		return this.msgEvent;
	}

	/**
	 * Returns the audio provider, linked to LavaPlayer
	 * 
	 * @return the audio provider
	 */
	protected final AudioProvider getAudioProvider() {
		return this.audioProvider;
	}

	/**
	 * Returns the current bot's client object
	 * 
	 * @return This bot as client
	 */
	public final GatewayDiscordClient getClient() {
		return this.client;
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
	 * Returns whether or not the author is connected to voice
	 * 
	 * @return Author connected to voice (true) or not (false)
	 */
	protected final boolean isAuthorVoiceConnected() {
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
		if (this.isVoiceConnected()) {
			return this.client.getSelf().block().asMember(this.getMessageGuild().getId()).block().getVoiceState()//TODO handle case voicestate == null
					.block().getChannel().block();
		} else {
			return null;
		}
	}

	protected final VoiceConnection getMyVoiceConnection() {
		return this.voiceConnection;
	}

	/**
	 * Indicated whether or not this bot is connected to voice
	 * 
	 * @return
	 */
	protected final boolean isVoiceConnected() {
		return this.voiceConnection != null;
	}

	/**
	 * Get private channel object of author
	 * 
	 * @return Private channel object of author
	 */
	protected final PrivateChannel getMessageAuthorPrivateChannel() {
		return this.msgAuthorObject.getPrivateChannel().block();
	}

	/**
	 * Returns whether or not the received message comes from a private channel
	 * 
	 * @return True if message comes from private channel, false otherwise
	 */
	protected final boolean isPrivate() {
		return this.getMessageChannel().getId().equals(this.getMessageAuthorPrivateChannel().getId());
	}

	protected final long getResponseTime() {
		return 0l;//this.getClient().getResponseTime();
	}

	protected final ApplicationInfo getAppInfo() {
		return this.getClient().getApplicationInfo().block();
	}

	protected final GuildEmoji getEmoji(Snowflake guildID, Snowflake emojiID) {
		List<GuildEmoji> emojiList = this.getEmojiList(guildID);
		for (GuildEmoji emoji : emojiList) {
			if (emoji.getId().equals(emojiID)) {
				return emoji;
			}
		}
		return null;
	}

	protected final String getEmojiFormat(Snowflake guildID, Snowflake emojiID) {
		GuildEmoji emoji = this.getEmoji(guildID, emojiID);
		if (emoji != null) {
			return emoji.asFormat();
		}
		return "";
	}

	protected final List<GuildEmoji> getEmojiList(Snowflake guildID) {
		return this.getClient().getGuildById(guildID).block().getEmojis().buffer().blockFirst();
	}

	protected final Guild getGuildByID(Snowflake guildID) {
		return this.client.getGuildById(guildID).block();
	}

	protected final GuildMessageChannel getChannelByID(Snowflake channelID, Snowflake guildID) {
		return (GuildMessageChannel) this.getGuildByID(guildID).getChannelById(channelID).block();
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

	// ########## INTERACTIVE METHODS ##########

	/**
	 * Sends a response to the same channel where the received message came from and
	 * response the message instance
	 * 
	 * @param message The message to be sent
	 * @return The instance of the message that was sent
	 */
	protected final Message sendInSameChannel(String message) {
		return this.getMessageChannel().createMessage(message).block();
	}

	/**
	 * Logs the bot out
	 */
	protected final void logOut() {
		System.out.println("LOGGING OUT");
		this.client.logout().block();
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
	 * Deletes the message that was received
	 */
	protected final void deleteReceivedMessage() {
		this.getMessageObject().delete().block();
	}

	/**
	 * Sends a direct answer to the message author with mention "[MENTION], message"
	 * 
	 * @param message The message to be applied after mention
	 * @return The instance of the message that was sent
	 */
	protected final Message sendAnswer(String message) {
		return this.sendInSameChannel(this.msgAuthorObject.getMention() + ", " + message);
	}

	/**
	 * Sends a message to a dedicated channel in a guild
	 * 
	 * @param message
	 * @param channelID
	 * @param guildID
	 * @return
	 */
	public final Message sendInChannel(String message, Snowflake channelID, Snowflake guildID) {
		MessageChannel channel = this.getChannelByID(channelID, guildID);
		return channel.createMessage(message).block();
	}

	/**
	 * Sends a private answer to the message author
	 * 
	 * @param message The message to be sent privately
	 * @return The instance of the message that was sent
	 */
	protected final Message sendPrivateAnswer(String message) {
		return this.getMessageAuthorPrivateChannel().createMessage(message).block();
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
			this.voiceConnection = channel.join(spec -> spec.setProvider(audioProvider)).log().block(Duration.ofSeconds(30l));
			System.out.println("Connected to voice channel '" + CHANNEL_NAME + "'");
		}
	}

	/**
	 * Leaves voice channel, if bot is connected. Catches and prints any exceptions
	 * that may occur.
	 */
	public final void leaveVoiceChannel() {
		try {
			this.getClient().getVoiceConnectionRegistry().disconnect(this.voiceConnection.getGuildId()).doOnTerminate(() -> {
				System.out.println("Left voice channel");
				this.voiceConnection = null;
			})
			.subscribe();
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
	protected final int deleteAllMessages(Snowflake channelID, Snowflake guildID) {
		int ret = 0;
		MessageChannel megChannel = this.getChannelByID(channelID, guildID);
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
	protected final int deleteMessages(Snowflake channelID, Snowflake guildID, int amount) {
		int ret = 0;
		if (amount > 0) {
			MessageChannel channel = this.getChannelByID(channelID, guildID);
			for (int i = 0; i < amount; i++) {
				Message lastMessage = channel.getLastMessage().block();
				this.deleteMessage(lastMessage);
				ret++;
			}
		}
		return ret;
	}

	// TECHNICAL METHODS

	protected final boolean surveyExists(String keyWord) {
		return this.getSurveyForKey(keyWord) != null;
	}

	protected final Survey getSurveyForKey(String keyWord) {
		for (Survey survey : this.surveys) {
			if (survey.key.equals(keyWord)) {
				return survey;
			}
		}
		return null;
	}
}
