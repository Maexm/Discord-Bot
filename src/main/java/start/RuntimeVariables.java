package start;

import java.util.Calendar;
import java.util.TimeZone;

import config.MainConfig;

public class RuntimeVariables {

	private static RuntimeVariables runtimeVariables = null;
	private static Calendar startDate = Calendar.getInstance();
	static boolean isDebug = false;
	static boolean firstLogin = true;

	private MainConfig config;
	private TimeZone homeTimezone = TimeZone.getTimeZone("CET");

	private RuntimeVariables(MainConfig config){
		this.config = config;

		if(config != null && config.homeTimezone != null && !config.homeTimezone.equals("")){
			this.homeTimezone = TimeZone.getTimeZone(config.homeTimezone);
		}
	}

	public TimeZone getTimezone(){
		return (TimeZone) this.homeTimezone.clone();
	}

	public String getVersion(){
		return this.config.version;
	}

	public String getCommandPrefix(){
		return this.config.commandPrefix;
	}

	public String getGitUrl(){
		return this.config.gitUrl;
	}

	public String getWeatherApiKey(){
		return this.config.weatherApiKey;
	}

	public String getHometown(){
		return this.config.homeTown;
	}

	public String getAnsSuffix(){
		return this.config.ansSuffix;
	}

	public String getAnsPrefix(){
		return this.config.ansPrefix;
	}

	public String getHelpText(){
		return this.config.helpText;
	}

	public String getMusicHelpText(){
		return this.config.musicHelpText;
	}

	public String getSurveyHelpText(){
		return this.config.surveyHelpText;
	}

	public String getAdmHelpText(){
		return this.config.admHelpText;
	}

	String getSpotifyClientSecret(){
		return this.config.spotifyClientSecret;
	}

	String getSpotifyClientId(){
		return this.config.spotifyClientId;
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
