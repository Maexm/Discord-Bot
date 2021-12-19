package musicBot;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import discord4j.core.object.entity.User;
import exceptions.IllegalMagicException;
import reactor.core.publisher.Mono;
import spotify.SpotifyResolver;
import spotify.SpotifyObjects.SpotifyAlbumResponse;
import spotify.SpotifyObjects.SpotifyArtistResponse;
import spotify.SpotifyObjects.SpotifyTrackResponse;
import system.DecompiledMessage;
import util.Time;

public class MusicTrackInfo {

	private String trackQuery;
	private final String originalQuery;
	private final User submittedByUser;
	private final String[] protocols = {"https://", "https://www."}; // ignore http, there is no reason for why you should use that
	private final String[] MUSIC_URL_HOSTS = {"youtube.com", "youtu.be", "soundcloud.com", "music.youtube.com"};
	public final AudioEventHandler audioEventHandler;
	public final DecompiledMessage userRequestMessage;
	public final Function<String, Mono<Void>> editMsg;

	private final ScheduleType scheduleType;
	private final long startTimeStamp;
	private TrackType trackType;

	public MusicTrackInfo(final String url, final User submittedByUser, final AudioEventHandler audioEventHandler,
			final DecompiledMessage userRequestMessage, final Function<String, Mono<Void>> editMsg, final ScheduleType scheduleType, final SpotifyResolver spotifyResolver) {
		final String sanitizedUrl = url.replace("${", "").replace("{", "").replace("}", "");
		this.originalQuery = sanitizedUrl;
		this.trackQuery = this.evalUrl(sanitizedUrl, spotifyResolver); // Determine search term for track loader & determine track type

		// URL tracks can have a timestamp
		if(this.trackType == TrackType.URL){
			this.startTimeStamp = this.extractTimeStamp(this.trackQuery);
			if(this.startTimeStamp != 0l){
				try {
					URL tempUrl = new URL(this.trackQuery);
					// Remove ref
					this.trackQuery = this.trackQuery.replace("#"+tempUrl.getRef(), "");
				} catch (MalformedURLException e) {
					throw new IllegalMagicException("");
				}
			}
		}
		else{
			this.startTimeStamp = 0l;
		}
		
		this.submittedByUser = submittedByUser;
		this.audioEventHandler = audioEventHandler;
		this.userRequestMessage = userRequestMessage;
		this.editMsg = editMsg;
		this.scheduleType = scheduleType;
	}

	private MusicTrackInfo(MusicTrackInfo copySrc){
		this.originalQuery = copySrc.originalQuery;
		this.trackQuery = copySrc.trackQuery;
		this.submittedByUser = copySrc.submittedByUser;
		this.audioEventHandler = copySrc.audioEventHandler;
		this.userRequestMessage = copySrc.userRequestMessage;
		this.scheduleType = copySrc.scheduleType;
		this.startTimeStamp = copySrc.startTimeStamp;
		this.trackType = copySrc.trackType;
		this.editMsg = copySrc.editMsg;
	}

	private long extractTimeStamp(final String url) {
		String srcTimeStamp = "";

		try {
			URL urlObject = new URL(this.addProtocol(url));
			String query = urlObject.getQuery();
			String ref = urlObject.getRef();

			// Query or ref exists -> find timestamp
			if(query != null || ref != null){
				String[] queryPairs = query != null ? query.split("&") : ref.split("&");
				for(String pair : queryPairs){
					String[] splittedPair = pair.split("="); // Returns array with original string as only element, if "=" is not included
					// Something is broken, if pair is not a pair. No Timestamp found
					if(splittedPair.length != 2){
						break;
					}
					// Found timestamp
					if(splittedPair[0].equals("t")){
						srcTimeStamp = splittedPair[1];
					}
				}
			}

		} catch (MalformedURLException e) {
		}

		try{
			if(srcTimeStamp.equals("")){
				return 0l;
			}
			else if(srcTimeStamp.contains(":")){
				return Time.revertMsToPretty(srcTimeStamp);
			}
			else{
				while(!srcTimeStamp.equals("")){
					try{
						return Long.parseLong(srcTimeStamp)*1000;
					}catch(NumberFormatException e){
						srcTimeStamp = srcTimeStamp.substring(0, srcTimeStamp.length()-1); // Timestamp might have units (e.g. 20s for 20 seconds) -> Shorten timestamp until something works :D
					}
				}
				// srcTimeStamp could not be parsed as long, even after removing characters
				throw new NumberFormatException();
			}
		}
		catch(Exception e){
			return 0l;
		}
	}
	
	private String evalUrl(final String url, SpotifyResolver spotifyResolver) {
		final String spotifyBaseUrl = "open.spotify.com";
		final String spotifyBaseCode = "spotify:";

		URL urlObj = null;
		try{
			urlObj = new URL(this.addProtocol(url));
		}
		catch(Exception e){
		}


			// Resolve spotify link and search on youtube
			if(urlObj != null && urlObj.getHost().equals(spotifyBaseUrl) || url.startsWith(spotifyBaseCode)){
				this.trackType = TrackType.SPOTIFY; // May be revoked later

				// Create non url path (in case that we received something like: "spotify:trackOrArtist:Id") and remove "spotify:" prefix
				String nonUrlPath = urlObj == null ? url.replaceFirst(spotifyBaseCode, "") : "";

				// Further determine what type of spotify url we are dealing with
				// ########## TRACK ##########
				if(nonUrlPath.startsWith("track:") || urlObj != null && urlObj.getPath().startsWith("/track/")){
					// Extract id and pass fetch metadata
					final String id = urlObj == null ? nonUrlPath.replaceFirst("track:", "") : urlObj.getPath().replace("/track/", "");
					SpotifyTrackResponse trackResponse = spotifyResolver.getSpotifyObject(id, SpotifyTrackResponse.class, true);

					// Evaluate metadata
					if(trackResponse != null){
						String firstArtistName = trackResponse.artists != null && trackResponse.artists.length > 0 ? trackResponse.artists[0].name+" " : "";
						return "ytsearch:"+firstArtistName+trackResponse.name;
					}
				}
				// ########## ALBUM ##########
				else if(nonUrlPath.startsWith("album:") || urlObj != null && urlObj.getPath().startsWith("/album/")){
					// Same comments as for "track"
					final String id = urlObj == null ? nonUrlPath.replaceFirst("album:", "") : urlObj.getPath().replace("/album/", "");
					SpotifyAlbumResponse albumResponse = spotifyResolver.getSpotifyObject(id, SpotifyAlbumResponse.class, true);

					if(albumResponse != null){
						String firstArtistName = albumResponse.artists != null && albumResponse.artists.length > 0 ? albumResponse.artists[0].name+" " : "";
						return "ytsearch:"+firstArtistName+albumResponse.name;
					}
				}
				// ########## ARTIST ##########
				else if(/*nonUrlPath.startsWith("artist:") || */urlObj != null && urlObj.getPath().startsWith("/artist/")){
					// Same comments as for "track"
					final String id = urlObj == null ? nonUrlPath.replaceFirst("artist:", "") : urlObj.getPath().replace("/artist/", "");
					SpotifyArtistResponse artistResponse = spotifyResolver.getSpotifyObject(id, SpotifyArtistResponse.class, true);

					if(artistResponse != null){
						return "ytsearch:"+artistResponse.name+" album";
					}
				}

			// Invalid spotify object or url => search on youtube (will be "catched" in next if block)
		}
		

		// Not an url or invalid host => yt search
		if(urlObj == null || !this.isValidHost(urlObj.getHost())) {
			this.trackType = TrackType.YOUTUBE_SEARCH;
			return "ytsearch:"+url;
		}

		// Valid url and valid host but not a spotify link => Will load track directly from given url
		this.trackType = TrackType.URL;
		return urlObj.toString();
	}

	private String addProtocol(final String url){
		for(String prefix: this.protocols){
			if(url.startsWith(prefix)){
				return url;
			}
		}
		return this.protocols[0] + url;
	}
	
	private boolean isValidHost(String host) {

		if(host.startsWith("www.")){
			host = host.replaceFirst("www\\.", "");
		}
		
		String[] hostSplitted = host.split("\\.");
		// Pay special attention to bandcamp links, since they can have the form "artist.bandcamp.com/", check if second and third component is "bandcamp" and "com/"
		if(hostSplitted.length == 3 && hostSplitted[1].equals("bandcamp") && hostSplitted[2].equals("com")) {
			return true;
		}
		// Check other fixed host names
		for(String prefix : MUSIC_URL_HOSTS) {
			if(host.equals(prefix)) {
				return true;
			}
		}
		return false;
	}

	public TrackType getTrackType(){
		return this.trackType;
	}
	/**
	 * @return The string query, ready to be loaded by a track loader
	 */
	public final String getQuery() {
		return this.trackQuery;
	}
	
	public final String getOriginalQuery(){
		return this.originalQuery;
	}

	public final User getSubmittedByUser() {
		return this.submittedByUser;
	}
	public ScheduleType getScheduleType(){
		return this.scheduleType;
	}
	public String toString() {
		return "'"+this.trackQuery+"' - submitted by "+this.getSubmittedByUser().getId();
	}
	
	public MusicTrackInfo clone() {
		return new MusicTrackInfo(this);
	}

	public long getStartTimeStamp(){
		return this.startTimeStamp;
	}

	public enum ScheduleType{
		PRIO, INTRUSIVE, NORMAL
	}

	public enum TrackType{
		YOUTUBE_SEARCH, URL, SPOTIFY
	}

}
