package system;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import musicBot.MusicTrackInfo;
import musicBot.TrackLoader;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import security.SecurityLevel;
import services.Time;
import snowflakes.ChannelID;
import start.RuntimeVariables;
import survey.Survey;

public final class BotHeart {
	
	private ResponseType responseSet;
	private final GatewayDiscordClient client;
	private final TrackLoader trackScheduler;
	private final AudioProvider audioProvider;
	private final AudioPlayer player;
	private final ArrayList<Survey> surveys = new ArrayList<Survey>();
	private final LinkedList<AudioTrack> trackList;
	private final LinkedList<MusicTrackInfo> addInfo;
	private final AudioEventHandler playerEventHandler;
	private final ArrayList<Middleware> middlewareBefore = new ArrayList<Middleware>();
	private final Snowflake guildId;
	private final TaskManager<RefinedTimerTask> systemTasks = new TaskManager<>();
	
	public BotHeart(final Snowflake guildId, final GatewayDiscordClient client, final AudioProvider audioProvider, final AudioPlayer player, final AudioPlayerManager playerManager) {
		this.guildId = guildId;
		this.trackList = new LinkedList<AudioTrack>();
		this.addInfo = new LinkedList<MusicTrackInfo>();
		this.client = client;
		this.player = player;
		this.player.setVolume(20);
		this.trackScheduler = new TrackLoader(player, trackList, addInfo, playerManager);
		this.audioProvider = audioProvider;
		this.playerEventHandler = new AudioEventHandler(this.player, playerManager, this.trackScheduler, trackList, addInfo);
		this.player.addListener(playerEventHandler);

		// ########## RESPONSE SETS ##########
		// TODO: Shorten this hell
		this.middlewareBefore.add(new Logger(this.guildId, client, this.audioProvider, this.surveys, this.playerEventHandler));
		this.middlewareBefore.add(new RoleFilter(this.guildId, client, this.audioProvider, this.surveys, this.playerEventHandler,
							msg -> RuntimeVariables.IS_DEBUG, SecurityLevel.DEV, "meine Dienste sind im Preview Modus nicht verfügbar!"));
		// this.middlewareBefore.add(new AutoReact(this.guildId, client, this.audioProvider, this.surveys, this.playerEventHandler,
		// 						msg -> {
		// 							final String[] expressions = {"explosion", "boom", "pau", "bum", "bam", "bäm", "bähm", "kaboom", "peng", "knall", "bakuhatsu", "bakuretsu", "kabum", "buhm", "bahm", "ばくれつ", "爆裂", "ばくはつ", "爆発"};
		// 							final String evalStr = msg.getContent().toLowerCase();
		// 							for(String expr : expressions){if(evalStr.contains(expr)){return true;}};	return false; // React if evalStr contains something from array, skip otherwise
		// 						}, ReactionEmoji. ));
		this.middlewareBefore.add(new VoiceGuard(this.guildId, client, this.audioProvider, this.surveys, this.playerEventHandler));
		this.middlewareBefore.add(new MusicRecommendation(this.guildId, client, this.audioProvider, this.surveys, this.playerEventHandler));
		this.responseSet = new Megumin(this.guildId, client, this.audioProvider, this.surveys, this.playerEventHandler);

		// ########## TASKS ##########
		// TODO: Move to a dedicated file
		final MessageChannel channelRef = (MessageChannel) this.client.getGuildById(this.guildId)
				.flatMap(guild -> guild.getChannelById(ChannelID.MEGUMIN)).block();
		this.systemTasks.addTask(new RefinedTimerTask(null,Long.valueOf(Time.DAY),Time.getNext(3, 0, 0).getTime() ,this.systemTasks){

			@Override
			public void runTask() {
				System.out.println("Executing CleanUp task!");
				Message lastMessage = channelRef.getLastMessage().block();
				List<Message> messages = channelRef.getMessagesBefore(lastMessage.getId()).collectList().block();
				messages.add(0, lastMessage);

				final String cleanUpFinish = "Täglicher CleanUp beendet!";

				Message infoMessage = channelRef.createMessage("Täglicher CleanUp wird ausgeführt!").block();

				for(Message message: messages){
					final String content = message.getContent();
					switch(content){
						case cleanUpFinish:
						case AudioEventHandler.MUSIC_STOPPED:
							message.delete();
							break;
					}
				}

				infoMessage.edit(edit -> edit.setContent(cleanUpFinish)).block();
				System.out.println("CleanUp task finished!");
			}
			
		});
	}
	
	/**
	 * Allow this message to be further analyzed, if its author is NOT this bot.
	 * @param msgEvent The message event
	 * @param client This client
	 */
	public void onMessageReceived(MessageCreateEvent msgEvent) {
		if(!msgEvent.getMessage().getAuthor().get().getId().equals(client.getSelfId())) {

			boolean shouldContinue = true;

			// ########## MIDDLEWARE BEFORE ##########
			for(Middleware middleware: this.middlewareBefore){
				try{
					shouldContinue = middleware.acceptEvent(msgEvent);
				}
				catch(Exception e){
					System.out.println("Error while using middleware: '"+middleware+"'");
					System.out.println(e);
				}
				if(!shouldContinue){
					return;
				}
			}

			// ########## RESPONSE TYPE ##########
			shouldContinue = this.responseSet.acceptEvent(msgEvent);

			if(!shouldContinue){
				return;
			}
		}
	}

}
