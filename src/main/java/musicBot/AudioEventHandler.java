package musicBot;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.http.client.ClientException;
import system.ResponseType;
import util.Emoji;
import util.Markdown;
import util.TimePrint;
import snowflakes.ChannelID;
import snowflakes.GuildID;

public class AudioEventHandler extends AudioEventAdapter {

	private final AudioPlayer player;
	private final LinkedList<AudioTrack> tracks;
	public final static String MUSIC_WARN = "";//":warning: Wiedergabe und Suche von YouTube Tracks funktioniert aktuell unzuverlässig!";
	public final static String MUSIC_LOADING = ":musical_note: Musik wird geladen...";
	@Deprecated
	public final static String MUSIC_STOPPED = ":musical_note: Eine Musiksession wurde beendet. Danke fürs Zuhören!";
	public final static String MUSIC_INFO_PREFIX = ":musical_note: Es wird abgespielt:";
	/**
	 * A list containing MusicTrackInfo (mainly who submitted this track). This
	 * instance is shared with a TrackLoader.
	 */
	private final LinkedList<MusicTrackInfo> loadingQueue;
	private Message radioMessage;
	private final AudioPlayerManager playerManager;
	private final TrackLoader loadScheduler;
	private Timer refreshTimer;
	private TimerTask refreshTask;
	private boolean active = false;
	/**
	 * Required for voice channel disconnect
	 */
	private ResponseType parent = null;
	/**
	 * Change between two different states for loading text (e.g. switch between / and \)
	 */
	private boolean loadFlag = true;

	private Snowflake musicChannelId;

	public AudioEventHandler(final AudioPlayer player, final AudioPlayerManager playerManager,
			final TrackLoader loadScheduler, final LinkedList<AudioTrack> tracks,
			final LinkedList<MusicTrackInfo> loadingQueue) {
		this.tracks = tracks;
		this.loadingQueue = loadingQueue;
		this.player = player;
		this.playerManager = playerManager;
		this.loadScheduler = loadScheduler;
		this.refreshTimer = null;
	}

	// TODO: Shorten this file

	public void schedule(MusicTrackInfo track, ResponseType parent) {
		boolean loadRightNow = this.loadingQueue.isEmpty();
		this.active = true;
		this.loadingQueue.add(track);
		this.parent = parent;
		// Only load track, if only one track is in the loading queue
		if (loadRightNow) {
			System.out.println("Loading track");
			// Track will load IMMEDIATELY
			this.playerManager.loadItemOrdered(track, track.getURL(), this.loadScheduler);
		}
		// Create a new radioMessage, if one does not already exist.
		if (this.radioMessage == null && loadRightNow) {
			this.musicChannelId = parent.getGuildId().equals(GuildID.UNSER_SERVER) ? ChannelID.MEGUMIN : track.userRequestMessage.getChannelId();
			this.createRadioMessage(":musical_note: Musikwiedergabe wird gestartet...");
		}
		// Update radioMessage, if one does already exist.
		else {
			this.updateInfoMsg();
		}
	}

	/**
	 * Player is considered as (only) loading, when nothing is playing but
	 * loadingQueue is not empty
	 * 
	 * @return
	 */
	public boolean isLoading() {
		return !this.isPlaying() && this.loadingQueue.size() != 0;
	}

	public boolean isPaused() {
		return this.player.isPaused();
	}

	public int getVolume() {
		return this.player.getVolume();
	}

	public void setVolume(int vol) {
		this.player.setVolume(vol);
	}
	/**
	 * Indicates whether or not a music session is actve. A music session is active when the first song has been added, until the
	 * ended() method has been invoked (which also deletes the music info message on discord)
	 * @return True if player is music session is on going (!= player is actually playing). False otherwise.
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * 
	 * @return The audio track that the AudioPlayer is actually playing right now.
	 */
	public AudioTrack getCurrentAudioTrack() {
		return this.player.getPlayingTrack();
	}

	/**
	 * Stops current track, invoking AudioEventHandler.onTrackEnd
	 */
	public void stop() {
		this.player.stopTrack();
	}

	public int clearList() {
		int ret = this.tracks.size();
		this.tracks.clear();
		return ret;
	}

	public int getListSize() {
		return this.tracks.size();
	}

	/**
	 * 
	 * @return True if audio player has a currently playing track, false otherwise
	 */
	public boolean isPlaying() {
		return this.getCurrentAudioTrack() != null;
	}

	public void pause() {
		this.player.setPaused(!isPaused());
		this.updateInfoMsg();
	}

	/**
	 * Removes tracks from the track queue and automatically plays the next tracks
	 * from the queue. Stops the music player, if queue is empty.
	 * 
	 * @param amount The amount of tracks that should be removed. The player stops,
	 *               if amount exceeds or matches queue size. amount == 1 is equal
	 *               to "play next track". amount lower or equal to zero are ignored
	 *               (nothing happens).
	 */
	public void next(int amount) {
		amount--;// Decrement amount, since this.player.playtrack will already "skip" one track.
		this.remove(amount);
		if (this.tracks.size() != 0) {
			this.player.playTrack(this.tracks.pollFirst());
		} else {
			this.stop();
		}
	}

	/**
	 * Removes tracks from the track queue. You should consider using next(int
	 * amount) instead, as next(int amount) automatically starts playing the next
	 * track.
	 * 
	 * @param amount The amount of tracks that should be removed from the queue.
	 *               Removes all elements, if amount exceeds queue size. Values
	 *               below 1 are ignored (nothing happens).
	 */
	public void remove(int amount) {
		for (int i = 0; i < amount && i < this.getListSize(); i++) {
			this.tracks.remove(0);
		}
	}
	/**
	 * Sets the position for the current track.
	 * Does nothing, if track is a stream
	 * @param pos
	 */
	public void setPosition(long pos){
		if(!this.getCurrentAudioTrack().getInfo().isStream){
			this.getCurrentAudioTrack().setPosition(pos);
		}
	}
	/**
	 * Moves forward or backwards in the currently playing track. Starts from zero or moves to the end of the track, if new position is out of range.
	 * @param amount Amount to move in milliseconds
	 * @return New position of track
	 */
	public long jump(long amount){
		long newTrackPos = this.getCurrentAudioTrack().getPosition() + amount;
		newTrackPos = Math.max(0l, newTrackPos);
		newTrackPos = Math.min(this.getCurrentAudioTrack().getDuration(), newTrackPos);

		this.setPosition(newTrackPos);

		return newTrackPos;
	}

	public void randomize(){
		Collections.shuffle(this.tracks);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		System.out.println("Track finished with reason: '" + endReason + "'");
		this.refreshTask.cancel();
		this.refreshTimer.purge();

		// LOAD FAILED
		if (endReason == AudioTrackEndReason.LOAD_FAILED) {
			MusicTrackInfo failedTrack = track.getUserData(MusicTrackInfo.class);
			if (failedTrack != null) {
				Message failedTrackMsg = failedTrack.userRequestMessage;
				failedTrackMsg.getChannel()
				.flatMap(channel -> channel.createMessage(failedTrack.getSubmittedByUser().getMention() + ", bei der Wiedergabe deines Tracks ist leider ein Fehler aufgetreten!\n\n"
				+"Beachte, bei "+Markdown.toBold("YouTube-Videos")+" können Videos mit Alterbeschränkung leider nicht abgespielt werden!"))
				.subscribe();
			} else if (this.radioMessage != null) {
				this.radioMessage.getChannel()
				.flatMap(channel -> channel.createMessage("Während der Wiedergabe eines Tracks ist ein Fehler aufgetreten!"))
				.subscribe();
			}
		}
		else if(endReason == AudioTrackEndReason.CLEANUP){
			MusicTrackInfo failedTrack = track.getUserData(MusicTrackInfo.class);
			if (failedTrack != null) {
				Message failedTrackMsg = failedTrack.userRequestMessage;
				this.parent.getOwnerMentionAsync().flatMap(ownerMention -> 
					failedTrackMsg.getChannel()
					.flatMap(channel -> channel.createMessage(failedTrack.getSubmittedByUser().getMention() + ", dein Track war inaktiv und wurde beendet!\n"+ownerMention+", das könnte ein Bug sein!"))
				).subscribe();
			} else if (this.radioMessage != null) {
				this.parent.getOwnerMentionAsync().flatMap(ownerMention -> 
					this.radioMessage.getChannel()
					.flatMap(channel -> channel.createMessage("Ein Track wurde aufgrund von Inaktivität beendet!\n"+ownerMention+", das könnte ein Bug sein!"))
				).subscribe();
			}
		}

		// STARTING NEXT
		if (this.tracks.size() != 0 && endReason.mayStartNext) {
			System.out.println("Starting next!");
			this.player.playTrack(this.tracks.pollFirst());
		}

		// TRACK HAS BEEN REPALCED
		else if (endReason == AudioTrackEndReason.REPLACED) {
			System.out.println("Track got replaced!");
		}

		// NO MORE TRACKS IN QUEUE -> STOPPING
		else {
			this.ended();
		}
	}

	void ended() {
		System.out.println("Music ended!");
		//this.parent.getClient().updatePresence(Presence.online(Activity.playing(RuntimeVariables.getStatus()))).subscribe();
		this.active = false;
		this.refreshTask.cancel();
		this.refreshTimer.purge();
		this.refreshTimer.cancel();
		this.refreshTimer = null;

		//Message oldMessage = this.radioMessage;
		try{
			this.parent.leaveVoiceChannel();
			this.radioMessage.delete().block();
		}catch(Exception e){
			System.out.println("Could not delete radio message while ending music session");
		}
		
		this.radioMessage = null;
		this.musicChannelId = null;
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		System.out.println("Track has started!");
		if (!this.active) {
			System.out.println("Stopping a track from being played, while player not active");
			player.stopTrack();
			return;
		}

		// Set discord status
		//this.parent.getClient().updatePresence(Presence.online(Activity.streaming(RuntimeVariables.getStatus(), track.getInfo().uri))).subscribe();

		// Create refresh task
		final AudioEventHandler timerParent = this;
		this.refreshTask = new TimerTask() {

			@Override
			public void run() {
				timerParent.updateInfoMsg();
			}

		};
		if(this.refreshTimer == null){
			this.refreshTimer = new Timer("MegMusikBot", true);// Create new timer instance, if it doesnt exist
		}
		else{
			this.refreshTimer.purge(); // Remove old canceled tasks
		}
		this.refreshTimer.schedule(this.refreshTask, 0, 1000);
		if (this.radioMessage != null) {
			this.updateInfoMsg();
		} else {
			System.out.println("A music info message does not exist for some reason!");
		}
	}

	private String buildInfoText(AudioTrack track) {
		// Status
		String status = "";
		if (this.isPaused()) {
			status = "Die Wiedergabe ist " + Markdown.toBold("pausiert") + "!";
		} else if (!this.isPlaying()) {
			return AudioEventHandler.MUSIC_LOADING;
		}

		// Volume
		String volume = "Lautstärke: " + Markdown.toBold(this.getVolume() + "% ") + Emoji.getVol(this.getVolume());

		// ytSearch and userName
		String ytSearch = "";
		String userName = "FEHLER";
		if (track != null && track.getUserData(MusicTrackInfo.class) != null) {
			if (track.getUserData(MusicTrackInfo.class).getURL().startsWith("ytsearch:")) {
				ytSearch = "Das Video wurde auf YouTube unter dem Begriff "
						+ Markdown
								.toBold(track.getUserData(MusicTrackInfo.class).getURL().replaceFirst("ytsearch:", ""))
						+ " gefunden.\n";
			}
			userName = track.getUserData(MusicTrackInfo.class).getSubmittedByUser()
					.asMember(this.radioMessage.getGuild().block().getId()).block().getDisplayName();
		}

		// progress bar
		String progressBar = "";
		if(track.getInfo().isStream){
			progressBar += ":red_circle: LIVE STREAM " + (this.loadFlag ? "/" : "\\");
			this.loadFlag = !this.loadFlag;
		}
		else{
			final int barLength = 30;
			double perc = 1. * track.getPosition() / track.getDuration();
			for (int i = 0; i < barLength; i++) {
			if (i <= barLength * perc) {
				progressBar += "█";
			} else {
				progressBar += "░";
				}
			}
			progressBar += "\n" + Markdown.toBold(TimePrint.msToPretty(track.getPosition())) + " von "
					+ Markdown.toBold(TimePrint.msToPretty(track.getDuration()));
		}

		// ########## RETURNING ##########
		return 	AudioEventHandler.MUSIC_INFO_PREFIX + " "
				+ Markdown.toBold(track.getInfo().title) + " von " + Markdown.toBold(track.getInfo().author) + "\n\n"
				+ status + "\n" + volume + "\n" + this.getQueueInfoString() + "\n" + "\n" + progressBar + "\n"
				+ "Der Track wurde hinzugefügt von: " + Markdown.toBold(userName) + "\n" + ytSearch + "Link: "
				+ track.getInfo().uri
				+ (AudioEventHandler.MUSIC_WARN.length() > 0 ? "\n\n"+AudioEventHandler.MUSIC_WARN : "");
	}

	public String getQueueInfoString(){
		String queueInfo = "";
		if (this.tracks.size() == 0) {
			queueInfo = "Die Warteschlange ist " + Markdown.toBold("leer") + "!";
		} else if (this.tracks.size() == 1) {
			queueInfo = "Es befindet sich " + Markdown.toBold("ein") + " Lied in der Warteschlange!";
		} else {
			queueInfo = "Es befinden sich " + Markdown.toBold(Integer.toString(this.tracks.size()))
					+ " Lieder in der Warteschlange!";
		}
		if(this.tracks.size() > 0){
			queueInfo += "Schreib "+Markdown.toCodeBlock("MegClear")+", um die Warteschlange zu löschen!";
		}
		return queueInfo;
	}

	private void updateInfoMsg() {
		if (this.radioMessage != null) {
			// System.out.println("updating message");
			try {
				this.radioMessage = this.radioMessage.edit(spec -> {
					spec.setContent(this.buildInfoText(this.player.getPlayingTrack()));
				}).block();
			}
			catch(ClientException clientException){
				if(clientException.getStatus().code() == 404){
					System.out.println("Could not find radio message, creating new one!");
					this.createRadioMessage(this.buildInfoText(this.player.getPlayingTrack()));
				}
			} 
			catch (Exception e) {
				System.out.println("Failed to update radio message!");
				System.out.println(e);
			}
		}
	}

	public LinkedList<AudioTrack> getDeepListCopy(){
		LinkedList<AudioTrack> ret = new LinkedList<>();
		for(AudioTrack track: this.tracks){
			ret.add(track.makeClone());
		}
		return ret;
	}

	public static User getSubmittedByUser(AudioTrack track){
		MusicTrackInfo trackInfo = track.getUserData(MusicTrackInfo.class);
		return trackInfo != null ? trackInfo.getSubmittedByUser() : null;
	}
	public static String getSubmittedByUserName(AudioTrack track, Snowflake guildId){
		MusicTrackInfo trackInfo = track.getUserData(MusicTrackInfo.class);
		return trackInfo != null ? trackInfo.getSubmittedByUser().asMember(guildId).map(mem -> mem.getDisplayName()).block() : null;
	}

	private Message createRadioMessage(String msg){
		Message ret = null;
		try{
			ret = parent.sendInChannel(msg, this.musicChannelId);
			this.radioMessage = ret;
		}
		catch(Exception e){
			System.out.println("Failed to create a new radio message!");
		}
		
		return ret;
	}

}
