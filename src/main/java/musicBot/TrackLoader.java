package musicBot;

import java.util.LinkedList;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import exceptions.IllegalMagicException;
import util.Markdown;

public class TrackLoader implements AudioLoadResultHandler {

	private final AudioPlayer player;
	private final LinkedList<AudioTrack> trackList;
	private final LinkedList<MusicTrackInfo> loadingQueue;
	private final AudioPlayerManager playerManager;

	public TrackLoader(final AudioPlayer player, final LinkedList<AudioTrack> trackList,
			final LinkedList<MusicTrackInfo> loadingQueue, final AudioPlayerManager playerManager) {
		this.player = player;
		this.trackList = trackList;
		this.loadingQueue = loadingQueue;
		this.playerManager = playerManager;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		System.out.println("Track loaded...");
		this.digestTrack(track, this.getInfoForTrack(track));
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {

		if (playlist.getTracks().size() == 0) {
			throw new IllegalMagicException("got a playlist with size = 0");
		}
		if (playlist.isSearchResult()) {
			// Playing first track from search result
			System.out.println("Loaded a search result!");
			AudioTrack firstTrack = playlist.getTracks().get(0);
			this.digestTrack(firstTrack, this.getInfoForTrack(firstTrack));// Passing loaded track to trackLoaded method
		} else {
			System.out.println("Loaded playlist!");
			this.digestMultipleTracks(playlist);
		}

	}

	@Override
	public void noMatches() {
		System.out.println("No matches!");

		// Send info to user
		MusicTrackInfo failedTrack = this.loadingQueue.pollFirst();
		try{
			failedTrack.editMsg.apply(failedTrack.getSubmittedByUser().getMention()+" , konnte unter dem Begriff nichts finden! :x:").subscribe();
		}catch(Exception e){e.printStackTrace();}
		// Load next or stop if nothing is playing
		if (!this.loadingQueue.isEmpty()) {
			this.loadNext();
		} else if (!failedTrack.audioEventHandler.isPlaying()) {
			failedTrack.audioEventHandler.ended();
		}
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		System.out.println("Load failed! " + exception);
		// Send info to user
		MusicTrackInfo failedTrack = this.loadingQueue.pollFirst();
		try{
			failedTrack.editMsg.apply(failedTrack.getSubmittedByUser().getMention()+" , konnte deinen Track leider nicht laden! :x:").subscribe();
		}catch(Exception e){e.printStackTrace();}
		// Load next or stop if nothing is playing
		if (!this.loadingQueue.isEmpty()) {
			this.loadNext();
		} else if (!failedTrack.audioEventHandler.isPlaying()) {
			failedTrack.audioEventHandler.ended();
		}

	}

	private MusicTrackInfo getInfoForTrack(AudioTrack track) {
		if (this.loadingQueue.size() != 0) {
			return this.loadingQueue.pollFirst();
		}
		return null;
	}

	private void digestTrack(AudioTrack track, MusicTrackInfo info) {
		// Update user info, if possible
		AudioTrackInfo trackInfo = track.getInfo();
		if(info != null){
			String userMention = info.getSubmittedByUser().getMention();
			try{
				switch(info.getTrackType()){
					case URL:
						info.editMsg.apply(userMention+", "+Markdown.toBold(trackInfo.title)+" von "+Markdown.toBold(trackInfo.author)+" wurde hinzugefügt! :white_check_mark:").subscribe();
						break;
					case SPOTIFY:
						info.editMsg.apply(userMention+", für deinen Spotify-Link wurde "+ Markdown.toBold(trackInfo.title)+" von "+Markdown.toBold(trackInfo.author) +" gefunden und hinzugefügt! :white_check_mark:").subscribe();
						break;
					case YOUTUBE_SEARCH:
						info.editMsg.apply(userMention+", ich habe "+Markdown.toBold(trackInfo.title)+" von "+Markdown.toBold(trackInfo.author)+" gefunden und hinzugefügt! :white_check_mark:").subscribe();
						break;
				}
			}
			catch(Exception e){e.printStackTrace();}
		}
		track.setUserData(info);
		// Play track immediately, if nothing is playing
		if (this.player.getPlayingTrack() == null) {
			this.playTrack(track, info);
		}
		// Add track to queue, if something is already playing. Differentiate between ScheduleTypes
		else{
			switch(info.getScheduleType()){
				case NORMAL:
					this.trackList.add(track);
					break;
				case INTRUSIVE:
					this.playTrack(track, info);
					break;
				case PRIO:
					this.trackList.addFirst(track);
					break;
			}
		}
		this.loadNext();
	}

	private void digestMultipleTracks(AudioPlaylist playlist) {
		boolean firstTrackAlreadyPlaying = false;
		// Retrieve first track and its info
		MusicTrackInfo info = null;
		AudioTrack first = null;
		if (playlist.getTracks().size() != 0) {
			first = playlist.getTracks().get(0);
			info = this.getInfoForTrack(first);
		} else {
			throw new IllegalMagicException("Received playlist with a size of 0, how is this even possible?!");
		}

		// Update user info, if possible
		if(info != null){
			String userMention = info.getSubmittedByUser().getMention();
			int trackAmount = playlist.getTracks().size();
			try{
				info.editMsg.apply(userMention+", "+Markdown.toBold(trackAmount+" ")+ (trackAmount > 1 ? "Tracks wurden hinzugefügt! :white_check_mark:" : "Track wurde hinzugefügt! :white_check_mark:")).subscribe();
			}
			catch(Exception e){e.printStackTrace();}
		}
		// If nothing is playing, remove first from playlist and play it immediately
		if (this.player.getPlayingTrack() == null) {
			playlist.getTracks().remove(0);
			first.setUserData(info);
			this.playTrack(first, info);
			firstTrackAlreadyPlaying = true;
		}
		// Add loaded list to queue. First track will be included, if playingTrack was not null
		for (int i = 0; i < playlist.getTracks().size(); i++) {
			AudioTrack track = playlist.getTracks().get(i);
			track.setUserData(info);
			// Differentiate between ScheduleType - If first track is prio then every track in this playlist is prio, same for intrusive, but first tarck will be played immediately
			switch(info.getScheduleType()){
				case INTRUSIVE:
					if(!firstTrackAlreadyPlaying){
						this.playTrack(track, info);
						firstTrackAlreadyPlaying = true;
					}
					else{
						// First track has been added -> playlist index is ahead of trackList index
						this.trackList.add(i-1, track);
					}
					break;
				case PRIO:
					this.trackList.add(i, track);
					break;
				case NORMAL:
					this.trackList.add(track);
					break;
			}
		}
		this.loadNext();
	}
	/**
	 * Load next track from loading queue
	 */
	private void loadNext() {
		if (!this.loadingQueue.isEmpty()) {
			System.out.println("Loading next from queue");
			this.playerManager.loadItem(this.loadingQueue.getFirst().getQuery(), this);
		}
	}
	/**
	 * Play a track
	 * @param track The track to be played
	 * @param info MusicInfo, required in order to access the audioEventHandler
	 */
	void playTrack(AudioTrack track, MusicTrackInfo info) {
		if (info.audioEventHandler.isActive()) {
			long startPos = 0l;
			startPos = Math.max(0, info.getStartTimeStamp());
			startPos = Math.min(info.getStartTimeStamp(), track.getDuration());
			track.setPosition(startPos);
			
			this.player.playTrack(track);
		}
	}

}
