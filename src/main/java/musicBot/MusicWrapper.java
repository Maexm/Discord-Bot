package musicBot;

import java.util.LinkedList;
import java.util.Optional;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.common.util.Snowflake;
import discord4j.voice.AudioProvider;
import spotify.SpotifyResolver;

public class MusicWrapper {
    
    private final TrackLoader trackScheduler;
	private final AudioProvider audioProvider;
	private final AudioPlayer player;
	private final LinkedList<AudioTrack> trackList;
	private final LinkedList<MusicTrackInfo> addInfo;
	private final AudioEventHandler playerEventHandler;
	private final SpotifyResolver spotifyResolver;
	private Optional<Snowflake> musicChannelId = Optional.empty(); 
    
    public MusicWrapper(final AudioPlayerManager playerManager, final SpotifyResolver spotifyResolver){
		this.spotifyResolver = spotifyResolver;
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

	public final SpotifyResolver getSpotifyResolver(){
		return this.spotifyResolver;
	}

	public final Optional<Snowflake> getMusicChannelId(){
		return this.musicChannelId;
	}

	public final void setMusicChannelId(Snowflake id){
		this.musicChannelId = Optional.ofNullable(id);
	}

}
