package musicBot;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
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
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.http.client.ClientException;
import logging.QuickLogger;
import system.DecompiledMessage;
import system.ResponseType;
import util.Emoji;
import util.Markdown;
import util.TimePrint;

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
	private Optional<Message> radioMessage;
	private final AudioPlayerManager playerManager;
	private final TrackLoader loadScheduler;
	private Timer refreshTimer;
	private TimerTask refreshTask;
	private boolean active = false;
	private boolean lockMsgUpdate = false;
	/**
	 * Required for voice channel disconnect
	 */
	private ResponseType parent = null;
	/**
	 * Change between two different states for loading text (e.g. switch between / and \)
	 */
	private boolean loadFlag = true;
	private boolean isLoop = false;
	private boolean prevMutex = false;
	/**
	 * Copy of current track. Only present, if playback is looping (setting a finished track to pos 0 and replaying it wont work -> clone is required)
	 */
	private Optional<AudioTrack> trackCopy = Optional.empty();
	private Optional<AudioTrack> prevSong = Optional.empty();

	public AudioEventHandler(final AudioPlayer player, final AudioPlayerManager playerManager,
			final TrackLoader loadScheduler, final LinkedList<AudioTrack> tracks,
			final LinkedList<MusicTrackInfo> loadingQueue) {
		this.tracks = tracks;
		this.loadingQueue = loadingQueue;
		this.player = player;
		this.playerManager = playerManager;
		this.loadScheduler = loadScheduler;
		this.refreshTimer = null;
		this.radioMessage = Optional.empty();
	}

	// TODO: Shorten this file

	public void schedule(MusicTrackInfo track, ResponseType parent) {
		boolean loadRightNow = this.loadingQueue.isEmpty();
		this.active = true;
		this.loadingQueue.add(track);
		this.parent = parent;
		// Only load track, if only one track is in the loading queue
		if (loadRightNow) {
			QuickLogger.logDebug("Loading track");
			// Track will load IMMEDIATELY
			this.playerManager.loadItemOrdered(track, track.getQuery(), this.loadScheduler);
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
		this.setLoop(false);
		if (this.tracks.size() != 0) {
			AudioTrack track = this.tracks.pollFirst();
			this.loadScheduler.playTrack(track, track.getUserData(MusicTrackInfo.class));
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
	public long setPosition(long pos){
		if(!this.currentIsStream()){
			pos = Math.max(0l, pos);
			pos = Math.min(this.getCurrentAudioTrack().getDuration(), pos);
			this.getCurrentAudioTrack().setPosition(pos);
			return pos;
		}
		return this.getCurrentAudioTrack().getPosition();

	}
	/**
	 * Moves forward or backwards in the currently playing track. Starts from zero or moves to the end of the track, if new position is out of range.
	 * @param amount Amount to move in milliseconds
	 * @return New position of track
	 */
	public long jump(long amount){
		// ignore streams
		if(this.currentIsStream()){
			return 0l;
		}
		long newTrackPos = this.getCurrentAudioTrack().getPosition() + amount;

		return this.setPosition(newTrackPos);
	}

	public boolean currentIsStream(){
		return this.getCurrentAudioTrack().getInfo().isStream;
	}

	public void randomize(){
		Collections.shuffle(this.tracks);
	}

	public long getTotalDuration(boolean includeCurrent){
		long ret = 0l;
		
		for(AudioTrack track : this.tracks){
			ret += track.getDuration() - track.getPosition();
		}
		if(includeCurrent){
			AudioTrack currentTrack = this.getCurrentAudioTrack();
			ret += currentTrack.getDuration() - currentTrack.getPosition();
		}
		return ret;
	}

	public boolean isLoop(){
		return this.isLoop;
	}

	public void setLoop(boolean isLoop){
		this.isLoop = isLoop;
		if(isLoop){
			this.trackCopy = Optional.of(this.getCurrentAudioTrack().makeClone());
			this.trackCopy.get().setPosition(0);
		}
		else{
			this.trackCopy = Optional.empty();
		}
	}

	public boolean toggleLoop(){
		this.setLoop(!this.isLoop());
		return this.isLoop();
	}

	public Optional<MessageChannel> getRadioMessageChannel(){
		if(this.radioMessage.isPresent()){
			return Optional.of(this.radioMessage.get().getChannel().block());
		}
		return Optional.empty();
	}

	public void tryDeleteRadioMessage(){
		if(this.radioMessage.isPresent()){
			try{
				this.radioMessage.get().delete().block();
				this.radioMessage = Optional.empty();
			}catch(Exception e){}
		}
	}
	/**
	 * Plays previously played song, if one exists.
	 * @return The song that was stored for replay
	 */
	public Optional<AudioTrack> playPrevious(){
		if(!this.isActive()){
			return Optional.empty();
		}
		if(this.prevSong.isPresent()){
			AudioTrack ret = this.prevSong.get().makeClone();
			this.tracks.addFirst(this.getCurrentAudioTrack().makeClone());
			this.prevMutex = true;
			this.loadScheduler.playTrack(this.prevSong.get(), this.prevSong.get().getUserData(MusicTrackInfo.class));
			this.prevSong = Optional.empty();
			return Optional.ofNullable(ret);
		}
		return this.prevSong;
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		QuickLogger.logDebug("Track finished with reason: '" + endReason + "'");
		this.refreshTask.cancel();
		this.refreshTimer.purge();

		// LOAD FAILED
		switch(endReason){
			case LOAD_FAILED:
				QuickLogger.logErr("Failed to log track with reason "+endReason+ " MayStartNext = "+endReason.mayStartNext);
				MusicTrackInfo failedTrack = track.getUserData(MusicTrackInfo.class);
				if (failedTrack != null) {
					DecompiledMessage failedTrackMsg = failedTrack.userRequestMessage;
					failedTrackMsg.getChannel()
					.createMessage(failedTrack.getSubmittedByUser().getMention() + ", bei der Wiedergabe deines Tracks ist leider ein Fehler aufgetreten!\n\n"
					+"Bitte versuch es erneut. Falls das Problem besteht, melde es bitte kurz mit "+Markdown.toCodeBlock("MegFeedback Deine_Fehlerbeschreibung"))
					.subscribe();
				} else if (this.radioMessage.isPresent()) {
					this.radioMessage.get().getChannel()
					.flatMap(channel -> channel.createMessage("Während der Wiedergabe eines Tracks ist ein Fehler aufgetreten!"))
					.subscribe();
				}
				break;
			case CLEANUP:
				MusicTrackInfo cleanedTrack = track.getUserData(MusicTrackInfo.class);
				if (cleanedTrack != null) {
					DecompiledMessage cleanedTrackMsg = cleanedTrack.userRequestMessage;
						cleanedTrackMsg.getChannel()
						.createMessage(cleanedTrack.getSubmittedByUser().getMention() + ", dein Track war inaktiv und wurde beendet!\nBitte kontaktiere den Botinhaber mit `MegFeedback Deine Bugmeldung`, falls das öfter vorkommen sollte. Du solltest die Musik-Session jetzt nochmal starten können!")
						.subscribe();
				} else if (this.radioMessage.isPresent()) {
						this.radioMessage.get().getChannel().flatMap(channel -> channel.createMessage("Ein Track wurde aufgrund von Inaktivität beendet!\nBitte kontaktiere den Botinhaber mit `MegFeedback Deine Bugmeldung`, falls das öfter vorkommen sollte. Du solltest die Musik-Session jetzt nochmal starten können!"))
						.subscribe();
				}
				break;
			case REPLACED:
				this.setLoop(false);
				break;
			default:
			// Default case resolves warning in switch arg
		}

		try{
			if(!this.isLoop() && !track.getInfo().isStream && !this.prevMutex){
				this.prevSong = Optional.ofNullable(track.makeClone());
			}
		}
		catch(Exception e) {}
		

		// STARTING NEXT
		if(endReason.mayStartNext){
			if(this.isLoop() && this.trackCopy.isPresent()){
				this.loadScheduler.playTrack(this.trackCopy.get().makeClone(), track.getUserData(MusicTrackInfo.class));
			}
			else if(this.tracks.size() != 0){
				QuickLogger.logDebug("Starting next!");
				this.next(1);
			}
		}
		// NO MORE TRACKS IN QUEUE -> STOPPING
		else if(endReason != AudioTrackEndReason.REPLACED){
			this.ended();
		}
		this.prevMutex = false;
	}

	void ended() {
		QuickLogger.logDebug("Music ended!");
		try{
			//this.parent.getClient().updatePresence(Presence.online(Activity.playing(RuntimeVariables.getStatus()))).subscribe();
			this.active = false;
			this.setLoop(false);
			this.prevSong = Optional.empty();
			if(this.refreshTask != null){
				this.refreshTask.cancel();
			}
			if(this.refreshTimer != null){
				this.refreshTimer.purge();
				this.refreshTimer.cancel();
			}
			this.refreshTimer = null;
			this.lockMsgUpdate = false;

			Message oldMessage = this.radioMessage.orElse(null);
			this.radioMessage = Optional.empty();
			try{
				this.parent.leaveVoiceChannel();
				oldMessage.delete().subscribe();
			}catch(Exception e){
				QuickLogger.logMinErr("Could not delete radio message while ending music session");
			}
		} catch(Exception e){
			QuickLogger.logFatalErr("Failed to end music session!");
			e.printStackTrace();
		}	
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		QuickLogger.logDebug("Track has started!");
		if (!this.active) {
			QuickLogger.logDebug("Stopping a track from being played, while player not active");
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
				try{
					timerParent.updateInfoMsg();
				}
				catch(Exception e){
					//e.printStackTrace();
				}
				
			}

		};
		if(this.refreshTimer == null){
			this.refreshTimer = new Timer("MegMusikBot", true);// Create new timer instance, if it doesnt exist
		}
		else{
			this.refreshTimer.purge(); // Remove old canceled tasks
		}
		this.refreshTimer.schedule(this.refreshTask, 0, 1000);
		if (this.radioMessage.isPresent()) {
			this.updateInfoMsg();
		} else {
			QuickLogger.logMinErr("A music info message does not exist for some reason!");
		}
	}

	private String buildInfoText() {
		AudioTrack track = this.getCurrentAudioTrack();
		// Status
		String status = "";
		if (this.isPaused()) {
			status = "Die Wiedergabe ist " + Markdown.toBold("pausiert") + "!";
		} else if (!this.isPlaying()) {
			return AudioEventHandler.MUSIC_LOADING;
		}

		if(this.isLoop()){
			status += "\nDauerschleife :repeat_one:";
		}

		// Volume
		String volume = "Lautstärke: " + Markdown.toBold(this.getVolume() + "% ") + Emoji.getVol(this.getVolume());

		// Query info and userName
		String ytSearch = "";
		String userName = "FEHLER";
		MusicTrackInfo trackInfo = track.getUserData(MusicTrackInfo.class);
		if (track != null && trackInfo != null) {
			switch(trackInfo.getTrackType()){
				case YOUTUBE_SEARCH:
					ytSearch = "YT-Suche: "
					+ Markdown
							.toBold(trackInfo.getQuery().replaceFirst("ytsearch:", ""))
					+ "\n";
					break;
				case SPOTIFY:
					ytSearch = "YT-Suche: "
						+ Markdown
								.toBold(trackInfo.getQuery().replaceFirst("ytsearch:", ""))
						+ "\nSpotify: "+Markdown.noLinkPreview(trackInfo.getOriginalQuery())+"\n";
					break;
				default:
			}

			userName = trackInfo.getSubmittedByUser().asMember( trackInfo.userRequestMessage.getGuild().getId())
			.map(member -> member.getDisplayName()).block();
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
				+ "Hinzugefügt von: " + Markdown.toBold(userName) + "\n" + ytSearch + "Link: "
				+ track.getInfo().uri
				+ (AudioEventHandler.MUSIC_WARN.length() > 0 ? "\n\n"+AudioEventHandler.MUSIC_WARN : "");
	}

	public String getQueueInfoString(){
		String queueInfo = "";
		String totalLengthInfo = "Restdauer: "+Markdown.toBold(TimePrint.msToPretty(this.getTotalDuration(true)));
		if (this.tracks.size() == 0) {
			queueInfo = "Die Warteschlange ist " + Markdown.toBold("leer") + "!";
		} else if (this.tracks.size() == 1) {
			queueInfo = "Es befindet sich " + Markdown.toBold("ein") + " Lied in der Warteschlange!\n"+totalLengthInfo+"\n";
		} else {
			queueInfo = "Es befinden sich " + Markdown.toBold(Integer.toString(this.tracks.size()))
					+ " Lieder in der Warteschlange!\n"+totalLengthInfo+"\n";
		}
		if(this.tracks.size() > 0){
			queueInfo += "Schreib "+Markdown.toCodeBlock("MegClear")+", um die Warteschlange zu löschen!";
		}
		return queueInfo;
	}

	private void updateInfoMsg() {
			try {
				if(this.radioMessage.isEmpty()){
					throw new NullPointerException();
				}
				else{
					this.radioMessage = Optional.of(this.radioMessage.get().edit(spec -> {
						spec.setContent(this.buildInfoText());
					}).block());
				}
			}
			catch(ClientException | NullPointerException clientException){
				if((clientException.getClass().getName().equals(ClientException.class.getName()) && ((ClientException) clientException).getStatus().code() == 404 || clientException.getClass().getName().equals(NullPointerException.class.getName())) && this.active){
					QuickLogger.logDebug("Could not find radio message ("+clientException.getClass().getName()+"), creating new one!");
					this.radioMessage = this.createRadioMessage(this.buildInfoText());
				}
			} 
			catch (Exception e) {
				QuickLogger.logMinErr("Failed to update radio message!");
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

	private Optional<Message> createRadioMessage(String msg, Snowflake channelId){
		if(channelId == null){
			channelId = this.parent.getConfig().getMusicWrapper().getMusicChannelId().isPresent() ? 
			this.parent.getConfig().getMusicWrapper().getMusicChannelId().get() : 
			(this.getCurrentAudioTrack() != null && this.getCurrentAudioTrack().getUserData(MusicTrackInfo.class) != null ? this.getCurrentAudioTrack().getUserData(MusicTrackInfo.class).userRequestMessage.getChannel().getId() : null);
		}
		Optional<Message> ret = Optional.empty();

		if(channelId == null){
			return ret;
		}

		try{
			if(this.lockMsgUpdate){
				return this.radioMessage;
			}
			this.lockMsgUpdate = true;
			ret = Optional.of(parent.sendInChannel(msg, channelId));
			this.radioMessage = ret;
		}
		catch(ClientException e){
			if(e.getStatus().code() != 403){
				QuickLogger.logMinErr("Cannot create radio message (http code "+e.getStatus().code()+")");
			}
		}
		catch(Exception e){
			QuickLogger.logErr("Failed to create a new radio message!");
		}
		finally{
			this.lockMsgUpdate = false;
		}
		
		return ret;
	}

	private Optional<Message> createRadioMessage(String msg){
		return this.createRadioMessage(msg, null);
	}

}
