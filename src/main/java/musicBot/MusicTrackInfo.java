package musicBot;


import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

public class MusicTrackInfo {

	private final String url;
	private final User submittedByUser;
	private final static String[] MUSIC_URL_PREFIXES = {"youtube.com", "youtu.be", "soundcloud.com"};
	public final AudioEventHandler audioEventHandler;
	public final Message userRequestMessage;
	
	public MusicTrackInfo(String url, final User submittedByUser, final AudioEventHandler audioEventHandler, final Message userRequestMessage) {
		
		this.url = this.adjustURL(url);
		this.submittedByUser = submittedByUser;
		this.audioEventHandler = audioEventHandler;
		this.userRequestMessage = userRequestMessage;
	}
	
	private String adjustURL(String url) {
		if(!this.isValidURL(url)) {
			if(url.startsWith("https://") || url.startsWith("www.")) {
				//throw new InvalidMusicURLException("Invalid url '"+url+"'");
			}
			url = "ytsearch:"+url;
		}
		return url;
	}
	
	private boolean isValidURL(String url) {
		url = url.replaceFirst("https://www.", "").replaceFirst("https://", "");
		String[] urlSplitted = url.split("\\.");
		if(urlSplitted.length >= 3 && urlSplitted[1].equals("bandcamp") && urlSplitted[2].startsWith("com/")) {
			return true;
		}
		for(String prefix : MusicTrackInfo.MUSIC_URL_PREFIXES) {
			if(url.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
	
	public final String getURL() {
		return this.url;
	}
	
	public final User getSubmittedByUser() {
		return this.submittedByUser;
	}
	public String toString() {
		return "'"+this.url+"' - submitted by "+this.getSubmittedByUser().getId();
	}
	
	public MusicTrackInfo clone() {
		return new MusicTrackInfo(this.url, this.submittedByUser, this.audioEventHandler, this.userRequestMessage);
	}
}
