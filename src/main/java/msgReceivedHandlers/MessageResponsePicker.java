package msgReceivedHandlers;

import java.util.ArrayList;
import java.util.LinkedList;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import musicBot.MusicTrackInfo;
import musicBot.TrackLoader;
import survey.Survey;

public final class MessageResponsePicker {
	
	private ResponseType responseSet;
	private final DiscordClient client;
	private final TrackLoader trackScheduler;
	private final AudioProvider audioProvider;
	private final AudioPlayer player;
	private final ArrayList<Survey> surveys = new ArrayList<Survey>();
	private final LinkedList<AudioTrack> trackList;
	private final LinkedList<MusicTrackInfo> addInfo;
	private final AudioEventHandler playerEventHandler;
	
	public MessageResponsePicker(final DiscordClient client, final AudioProvider audioProvider, final AudioPlayer player, final AudioPlayerManager playerManager) {
		this.trackList = new LinkedList<AudioTrack>();
		this.addInfo = new LinkedList<MusicTrackInfo>();
		this.client = client;
		this.player = player;
		this.player.setVolume(20);
		this.trackScheduler = new TrackLoader(player, trackList, addInfo, playerManager);
		this.audioProvider = audioProvider;
		this.playerEventHandler = new AudioEventHandler(this.player, playerManager, this.trackScheduler, trackList, addInfo);
		this.player.addListener(playerEventHandler);
		this.responseSet = new Megumin(true, client, this.audioProvider, this.surveys, this.playerEventHandler);
	}
	
	/**
	 * Allow this message to be further analyzed, if its author is NOT this bot.
	 * @param msgEvent The message event
	 * @param client This client
	 */
	public void onMessageReceived(MessageCreateEvent msgEvent) {
		if(!msgEvent.getMessage().getAuthor().get().getId().equals(client.getSelfId().get())) {
			this.responseSet.acceptEvent(msgEvent);
		}
	}

}
