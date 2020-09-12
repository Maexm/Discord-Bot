package musicBot;

import java.util.LinkedList;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.core.object.entity.Message;
import exceptions.IllegalMagicException;

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
		Message failedTrackMsg = failedTrack.userRequestMessage;
		failedTrackMsg.getChannel().block()
				.createMessage(
						failedTrack.getSubmittedByUser().getMention() + ", konnte unter dem Begriff nichts finden!")
				.block();
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
		Message failedTrackMsg = failedTrack.userRequestMessage;
		failedTrackMsg.getChannel().block()
				.createMessage(
						failedTrack.getSubmittedByUser().getMention() + ", konnte deinen Track leider nicht laden!")
				.block();
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
		track.setUserData(info);
		if (this.player.getPlayingTrack() == null) {
			this.playTrack(track, info);
		} else {
			this.trackList.add(track);
		}
		this.loadNext();
	}

	private void digestMultipleTracks(AudioPlaylist playlist) {
		MusicTrackInfo info = null;
		AudioTrack first = null;
		if (playlist.getTracks().size() != 0) {
			first = playlist.getTracks().get(0);
			info = this.getInfoForTrack(first);
		} else {
			throw new IllegalMagicException("Received playlist with a size of 0, how is this even possible?!");
		}
		// If nothing is playing, remove first from playlist (first track will be played
		// later, but we need to keep the queue up to date!)
		if (this.player.getPlayingTrack() == null) {
			playlist.getTracks().remove(0);
			first.setUserData(info);
		}
		// Add loaded list to queue
		for (AudioTrack track : playlist.getTracks()) {
			track.setUserData(info);
			this.trackList.add(track);
		}
		if (this.player.getPlayingTrack() == null) {
			this.playTrack(first, info);
		}
		this.loadNext();
	}

	private void loadNext() {
		if (!this.loadingQueue.isEmpty()) {
			System.out.println("Loading next from queue");
			this.playerManager.loadItem(this.loadingQueue.getFirst().getURL(), this);
		}
	}

	private void playTrack(AudioTrack track, MusicTrackInfo info) {
		if (info.audioEventHandler.isActive()) {
			this.player.playTrack(track);
		}
	}

}
