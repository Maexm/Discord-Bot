package system;

import java.util.ArrayList;
import java.util.LinkedList;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import musicBot.MusicTrackInfo;
import musicBot.TrackLoader;
import security.SecurityLevel;
import start.RuntimeVariables;
import survey.Survey;

public final class MessageResponsePicker {
	
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
	
	public MessageResponsePicker(final GatewayDiscordClient client, final AudioProvider audioProvider, final AudioPlayer player, final AudioPlayerManager playerManager) {
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
		this.middlewareBefore.add(new Logger(client, this.audioProvider, this.surveys, this.playerEventHandler));
		this.middlewareBefore.add(new RoleFilter(client, this.audioProvider, this.surveys, this.playerEventHandler,
							() -> !RuntimeVariables.IS_DEBUG, SecurityLevel.DEV, "Meine Dienste sind im Preview Modus nicht verfügbar!"));
		this.middlewareBefore.add(new VoiceGuard(client, this.audioProvider, this.surveys, this.playerEventHandler));
		this.middlewareBefore.add(new MusicRecommendation(client, this.audioProvider, this.surveys, this.playerEventHandler));
		this.responseSet = new Megumin(client, this.audioProvider, this.surveys, this.playerEventHandler);
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
