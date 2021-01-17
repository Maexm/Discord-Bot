package musicBot;

import java.util.LinkedList;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.voice.AudioProvider;

public class MusicWrapper {
    
    private final TrackLoader trackScheduler;
	private final AudioProvider audioProvider;
	private final AudioPlayer player;
	private final LinkedList<AudioTrack> trackList;
	private final LinkedList<MusicTrackInfo> addInfo;
    private final AudioEventHandler playerEventHandler;
    
    public MusicWrapper(final AudioPlayerManager playerManager){
		this.player = playerManager.createPlayer();
		this.player.setVolume(20);
		this.audioProvider = new AudioProviderLavaPlayer(player);
		this.trackList = new LinkedList<AudioTrack>();
		this.addInfo = new LinkedList<MusicTrackInfo>();
		this.trackScheduler = new TrackLoader(player, trackList, addInfo, playerManager);
		this.playerEventHandler = new AudioEventHandler(this.player, playerManager, this.trackScheduler, trackList,
				addInfo);
		this.player.addListener(playerEventHandler);
	}
	
	public final AudioProvider getAudioProvider(){
		return this.audioProvider;
	}

	public final AudioEventHandler getMusicBotHandler(){
		return this.playerEventHandler;
	}

}
