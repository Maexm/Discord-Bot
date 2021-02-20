package start;

import java.util.Calendar;
import java.util.TimeZone;

import config.MainConfig;

public class RuntimeVariables {

	private static RuntimeVariables runtimeVariables = null;
	private static Calendar startDate = Calendar.getInstance();
	static boolean isDebug = false;
	static boolean firstLogin = true;

	private TimeZone homeTimezone = TimeZone.getTimeZone("CET");
	private String version = "1.1.0.1";
	private String commandPrefix = "MEG";
	private String gitUrl = "https://github.com/Maexm/Discord-Bot";
	private String weatherApiKey = null;
	private String homeTown = "Aachen";
	private String ansPrefix = "";
	private String ansSuffix = " " + ansPrefix;
	private String spotifyClientId = "";
	private String spotifyClientSecret = "";

	private RuntimeVariables(MainConfig config){
		if(config != null){
			if(config.homeTimezone != null){
				this.homeTimezone = TimeZone.getTimeZone(config.homeTimezone);
			}
			
			this.version = config.version;
			this.commandPrefix = config.commandPrefix;
			this.gitUrl = config.gitUrl;
			this.weatherApiKey = config.weatherApiKey;
			this.homeTown = config.homeTown;
			this.ansSuffix = config.ansSuffix;
			this.ansPrefix = config.ansPrefix;
			this.spotifyClientId = config.spotifyClientId;
			this.spotifyClientSecret = config.spotifyClientSecret;
		}
	}

	public TimeZone getTimezone(){
		return (TimeZone) this.homeTimezone.clone();
	}

	public String getVersion(){
		return this.version;
	}

	public String getCommandPrefix(){
		return this.commandPrefix;
	}

	public String getGitUrl(){
		return this.gitUrl;
	}

	public String getWeatherApiKey(){
		return this.weatherApiKey;
	}

	public String getHometown(){
		return this.homeTown;
	}

	public String getAnsSuffix(){
		return this.ansSuffix;
	}

	public String getAnsPrefix(){
		return this.ansPrefix;
	}

	String getSpotifyClientSecret(){
		return this.spotifyClientSecret;
	}

	String getSpotifyClientId(){
		return this.spotifyClientId;
	}

	// ########## STATIC ##########

	public static Calendar getStartDate(){
		return (Calendar) RuntimeVariables.startDate.clone();
	}

	public static boolean isDebug(){
		return RuntimeVariables.isDebug;
	}

	public static boolean isFirstLogin(){
		return RuntimeVariables.firstLogin;
	}
	
	public static String getStatus(){
		return RuntimeVariables.isDebug ? "TEST" : "Schreib 'MegHelp'!";
	}

	public static RuntimeVariables getInstance(){
		return runtimeVariables;
	}

	public static RuntimeVariables createInstance(MainConfig config){
		runtimeVariables = new RuntimeVariables(config);
		return runtimeVariables;
	}
}
