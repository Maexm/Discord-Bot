package musicBot;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import discord4j.core.object.entity.Message;
import msgReceivedHandlers.ResponseType;
import services.Emoji;
import services.Markdown;
import services.TimePrint;
import snowflakes.ChannelID;
import snowflakes.GuildID;

public class AudioEventHandler extends AudioEventAdapter {

	private final AudioPlayer player;
	private final LinkedList<AudioTrack> tracks;
	/**
	 * A list containing MusicTrackInfo (mainly who submitted this track).
	 * This instance is shared with a TrackLoader.
	 */
	private final LinkedList<MusicTrackInfo> loadingQueue;
	private Message radioMessage;
	private final AudioPlayerManager playerManager;
	private final TrackLoader loadScheduler;
	private Timer refreshTimer;
	private boolean active = false;
	/**
	 * Required for voice channel disconnect
	 */
	private ResponseType parent = null;

	public AudioEventHandler(final AudioPlayer player, final AudioPlayerManager playerManager, final TrackLoader loadScheduler, final LinkedList<AudioTrack> tracks, final LinkedList<MusicTrackInfo> loadingQueue) {
		this.tracks = tracks;
		this.loadingQueue = loadingQueue;
		this.player = player;
		this.playerManager = playerManager;
		this.loadScheduler = loadScheduler;
		this.refreshTimer = null;
		
	}

	public void schedule(MusicTrackInfo track, ResponseType parent) {
		boolean loadRightNow = this.loadingQueue.isEmpty();
		this.active = true;
		this.loadingQueue.add(track);
		this.parent = parent;
		// Only load track, if only one track is in the loading queue
		if(loadRightNow) {
			System.out.println("Loading track (AudioEventHandler");
			this.playerManager.loadItemOrdered(track, track.getURL(), this.loadScheduler);
		}
		//	Create a new radioMessage, if one does not already exist.
		if (this.radioMessage == null && loadRightNow) {
				this.radioMessage = parent.sendInChannel(":musical_note: Musikwiedergabe wird gestartet...", ChannelID.MEGUMIN, GuildID.UNSER_SERVER);
			}
		//	Update radioMessage, if one does already exist.
		 else {
			this.updateInfoMsg();
		}
	}
	

	/**
	 * Player is considered as (only) loading, when nothing is playing but loadingQueue is not empty
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
	public void clearList() {
		this.tracks.clear();
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
	 * Removes tracks from the track queue and automatically plays the next tracks from the queue.
	 * Stops the music player, if queue is empty.
	 * @param amount The amount of tracks that should be removed. The player stops, if amount exceeds or matches queue size.
	 * amount == 1 is equal to "play next track". amount lower or equal to zero are ignored (nothing happens).
	 */
	public void next(int amount) {
		amount--;//Decrement amount, since this.player.playtrack will already "skip" one track.
		this.remove(amount);
		if (this.tracks.size() != 0) {
			this.player.playTrack(this.tracks.pollFirst());
		} else {
			this.stop();
		}
	}
	/**
	 * Removes tracks from the track queue.
	 * You should consider using next(int amount) instead, as next(int amount)
	 * automatically starts playing the next track.
	 * 
	 * @param amount The amount of tracks that should be removed from the queue. Removes all elements,
	 * if amount exceeds queue size. Values below 1 are ignored (nothing happens).
	 */
	public void remove(int amount) {
		if(amount > 0) {
			for(int i = 0;  i < amount && i < this.getListSize(); i++) {
				this.tracks.remove(0);
			}
		}
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		System.out.println("Track finished with reason: '" + endReason + "'");
		this.refreshTimer.cancel();
		
		//	LOAD FAILED
		if(endReason == AudioTrackEndReason.LOAD_FAILED){
			MusicTrackInfo failedTrack = track.getUserData(MusicTrackInfo.class);
			if(failedTrack != null) {
				Message failedTrackMsg = failedTrack.userRequestMessage;
				failedTrackMsg.getChannel().block().createMessage(failedTrack.getSubmittedByUser().getMention()+", konnte deinen Track leider nicht laden!").block();
			}
			else if(this.radioMessage != null){
				this.radioMessage.getChannel().block().createMessage("Konnte einen Track nicht abspielen!").block();
			}
		}
		
		// STARTING NEXT
		if (this.tracks.size() != 0 && endReason.mayStartNext) {
			System.out.println("Starting next!");
			this.player.playTrack(this.tracks.pollFirst());
		}
		
		// TRACK HAS BEEN REPALCED
		else if(endReason == AudioTrackEndReason.REPLACED) {
			System.out.println("Track got replaced!");
		}
		
		// NO MORE TRACKS IN QUEUE -> STOPPING
		else
		{
			this.ended();
		}
	}
	void ended() {
		System.out.println("Music ended!");
		this.active = false;
		this.radioMessage.edit(spec -> {
			spec.setContent(":musical_note: Eine Musiksession wurde beendet. Danke fürs Zuhören!");
		}).block();
		this.radioMessage = null;
		this.parent.leaveVoiceChannel();
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		System.out.println("Track has started!");
		if(!this.active) {
			System.out.println("Stopping a track from being played, while player not active");
			player.stopTrack();
			return;
		}
		final AudioEventHandler timerParent = this;
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				if(timerParent.radioMessage != null) {
					timerParent.updateInfoMsg();
				}
			}
			
		};
		this.refreshTimer = new Timer("MegMusikBot",true);//Create new Timer instance, since old timers won't work anymore
		this.refreshTimer = new Timer();
		this.refreshTimer.schedule(task, 0, 1000);
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
			return ":musical_note: Musik wird geladen...";
		}
		
		// Volume
		String volume = "Lautstärke: "+Markdown.toBold(this.getVolume()+"% ") + Emoji.getVol(this.getVolume());
		
		// Queue info
		String queueInfo = "";
		if (this.tracks.size() == 0) {
			queueInfo = "Die Warteschlange ist " + Markdown.toBold("leer") + "!";
		} else if (this.tracks.size() == 1) {
			queueInfo = "Es befindet sich " + Markdown.toBold("ein") + " Lied in der Warteschlange!";
		} else {
			queueInfo = "Es befinden sich " + Markdown.toBold(Integer.toString(this.tracks.size()))
					+ " Lieder in der Warteschlange!";
		}
		
		// ytSearch and userName
		String ytSearch = "";
		String userName = "FEHLER";
		if(track != null && track.getUserData(MusicTrackInfo.class) != null) {
			if(track.getUserData(MusicTrackInfo.class).getURL().startsWith("ytsearch:")) {
				ytSearch = "Das Video wurde auf YouTube unter dem Begriff "+Markdown.toBold(track.getUserData(MusicTrackInfo.class).getURL().replaceFirst("ytsearch:", ""))+" gefunden.\n";
			}
			userName = track.getUserData(MusicTrackInfo.class).getSubmittedByUser().asMember(this.radioMessage.getGuild().block().getId()).block().getDisplayName();
		}
		
		// progress bar
		String progressBar = "";
		final int barLength = 30; 
		double perc = 1.*track.getPosition()/ track.getDuration();
		for(int i = 0; i < barLength; i++) {
			if(i <= barLength*perc) {
				progressBar += "█";
			}
			else {
				progressBar += "░";
			}
		}
		progressBar += "\n"
				+ Markdown.toBold(TimePrint.msToPretty(track.getPosition()))+" von "+Markdown.toBold(TimePrint.msToPretty(track.getDuration()));
		
		
		//	##########	RETURNING	##########
		return "_____________________________\n"
				+ ":musical_note: Es wird abgespielt: " + Markdown.toBold(track.getInfo().title) + " von "
				+ Markdown.toBold(track.getInfo().author) + "\n\n"
				+ status
				+ "\n"
				+ volume
				+ "\n"
				+ queueInfo
				+ "\n"
				+ "\n"
				+ progressBar
				+ "\n"
				+ "Der Track wurde hinzugefügt von: "
				+ Markdown.toBold(userName)
				+ "\n"
				+ ytSearch + "Link: " + track.getInfo().uri;
	}

	private void updateInfoMsg() {
		if (this.radioMessage != null) {
			//System.out.println("updating message");
			try {
				this.radioMessage.edit(spec -> {
					spec.setContent(this.buildInfoText(this.player.getPlayingTrack()));
				}).block();
			}
			catch(Exception e) {
				System.out.println("Failed to update radio message!");
				System.out.println(e);
			}
		}
	}

}
