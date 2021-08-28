package system;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import exceptions.IllegalMagicException;
import logging.QuickLogger;
import musicBot.MusicTrackInfo.ScheduleType;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import security.SecurityLevel;
import start.RuntimeVariables;
import survey.VoteEndReason;

public abstract class ResponseType extends Middleware {

	private final TaskManager<RefinedTimerTask> localTasks;
	protected String commandSection = "";
    protected String argumentSection = "";

	public ResponseType(MiddlewareConfig config, final TaskManager<RefinedTimerTask> localTasks) {
		super(config, false);
		this.localTasks = localTasks;
	}

	/**
	 * Redirects message to appropriate method, where the actual response will be
	 * created.
	 */
	@Override
	protected final boolean handle() {
		
		final String PREFIX = RuntimeVariables.getInstance().getCommandPrefix().toLowerCase();

		// Reset
		this.argumentSection = "";
		this.commandSection = "";
		
		// Basic expressions
		switch (this.getMessage().getContent().toLowerCase()) {
		case "hey megumin":
			this.onGreeting();
		}

		// Expressions starting with prefix
		if (this.getMessage().getContent().toLowerCase().startsWith(PREFIX)) {

			// Extract order and arguments
			if (this.getMessage().getContent().contains(" ")) {
				final String[] splitted = this.getMessage().getContent().split(" ");

				int commandPos = 0;
				if(splitted[0].toLowerCase().equals(PREFIX)){
					commandPos++; // Use command in splitted[1] instead of splitted[0] - Message might be "Meg Command Args" instead of "MegCommand Args"
				}

				this.commandSection = splitted[commandPos];
				// Prefix right next to command
				if(commandPos == 0){
					this.argumentSection = this.getMessage().getContent().replaceFirst(this.commandSection + " ", "");// Do not use splitted[1], since args can have spaces as well
					this.commandSection = this.commandSection.toLowerCase().replaceFirst(PREFIX, "");
				}
				// Prefix seperated with " "
				else if(commandPos == 1){
					this.argumentSection = this.getMessage().getContent().replaceFirst(PREFIX + " " + this.commandSection + " ", "");
					this.commandSection = this.commandSection.toLowerCase();
				}
				else{
					throw new IllegalMagicException("commandPos must be 0 or 1");
				}
				
			}
			// No argument section exists
			else {
				this.commandSection = this.getMessage().getContent().toLowerCase().replaceFirst(PREFIX, "");
			}

			boolean isCommand = true;

			// Redirect // TODO: Outsource
			switch (this.commandSection.replace("musik", "music").replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")) {
			case "logout":
				this.onLogout();
				break;
			case "kill":
			case "terminate":
				this.kill();
				break;
			case "sprechen":
			case "schreiben":
			case "echo":
			case "print":
			case "say":
				this.onRepeat();
				break;
			case "katakana":
				this.onConvertToKatakana();
				break;
			case "music":
				this.onReceiveMusicRequest(ScheduleType.NORMAL, this.getArgumentSection());
				break;
			case "prio":
			case "musicprio":
			case "priomusic":
			case "push":
			case "musicpush":
			case "pushmusic":
			case "front":
			case "musicfront":
				this.onReceiveMusicRequest(ScheduleType.PRIO, this.getArgumentSection());
				break;
			case "clear":
			case "musicclear":
			case "clearmusic":
				this.onClearMusicQueue();
				break;
			case "pause":
			case "musicpause":
				this.onPauseMusic();
				break;
			case "play":
			case "musicplay":
				this.onResumeMusic();
				break;
			case "stop":
			case "musicstop":
				this.onStopMusic();
				break;
			case "musicnxt":
			case "musicskip":
			case "next":
			case "nxt":
			case "skip":
			case "skp":
			case "musicnext":
				this.onNextMusic();
				break;
			case "musichelp":
			case "helpmusic":
				this.onMusicHelp();
				break;
			case "vol":
			case "musicvol":
			case "lautstärke":
				this.onMusicVol();
				break;
			// case "musicidee":
			// case "musicideen":
			// case "musicideas":
			// case "musicidea":
			// 	this.onMusicIdea();
			// 	break;
			case "musiclist":
			case "musicliste":
			case "list":
			case "lst":
			case "ls":
			case "queue":
			case "musicqueue":
			case "warteschlange":
			case "schlange":
			case "musicschlange":
			
			case "musiclistall":
			case "musiclisteall":
			case "listall":
			case "queueall":
			case "musicqueueall":
			case "warteschlangeall":
			case "schlangeall":
			case "musicschlangeall":
			case "q":

				this.onMusicQueue();
				break;
			case "musicforward":
			case "musicjump":
			case "musicueberspringen":
			case "musichuepfen":
			case "jump":
			case "huepfen":
			case "ueberspringen":
			case "jmp":
				this.onFastForwardMusic();
				break;
			case "multiumfrage":
			case "umfrage":
				this.onSurvey();
				break;
			case "leaveumfrage":
				this.onSurveyLeave();
				break;
			case "zeit":
			case "time":
				this.onPostTime();
				break;
			case "helpumfrage":
			case "umfragehelp":
				this.onSurveyHelp();
				break;
			case "jisho":
			case "japanese":
				this.onJisho();
				break;
			case "romaji":
				this.onRomaji();
				break;
			case "status":
			case "stat":
			case "stats":
				this.onStatus();
				break;
			case "test":
				this.onTest();
				break;
			case "delete":
			case "loeschen":
			case "vernebeln":
			case "del":
			case "rm":
				this.onDeleteMessages();
				break;
			case "announce":
			case "psa":
			case "announcement":
			case "public":
				this.onPSA();
				break;
			case "update":
			case "updateinfo":
				this.onUpdatePSA();
				break;
			case "name":
				this.onChangeName();
				break;
			case "wetter":
			case "weather":
			case "wetta":
			case "weter":
			case "weta":
			case "tenki":
				this.onWeather();
				break;
			case "frage":
			case "qna":
			case "entscheidung":
			case "decision":
			case "janein":
			case "neinja":
			case "yesno":
			case "yesorno":
				this.onYesNo();
				break;
			case "wiki":
			case "wikipedia":
			case "wissen":
			case "knowledge":
			case "suche":
			case "such":
				this.onWiki();
				break;
			case "feedback":
			case "vorschlag":
			case "request":
			case "req":
				this.onFeedback();
				break;
			case "randomize":
			case "musicrandom":
			case "shuffle":
			case "musicshuffle":
			case "random":
			case "rand":
			case "musicrand":
				this.onMusicRandom();
				break;
			case "force":
			case "musicforce":
			case "forcemusic":
				this.onReceiveMusicRequest(ScheduleType.INTRUSIVE, this.getArgumentSection());
				break;
			case "chicken":
				this.onChicken();
				break;
			case "nuggets":
			case "chicken nuggets":
			case "chickennuggets":
			case "chickenmegnuggets":
				this.onNuggets();
				break;
			case "pommes":
			case "fritten":
			case "frites":
			case "pommes frites":
				this.onPommes();
				break;
			case "follow":
			case "subscribe":
			case "abonnieren":
			case "abo":
			case "sub":
				this.onSubscribeVoice();
				break;
			case "unfollow":
			case "unsubscribe":
			case "deabonnieren":
				this.onUnsubscribeVoice();
				break;
			case "subscriptions":
			case "abos":
			case "following":
				this.onGetVoiceSubscriptions();
				break;
			case "global":
			case "globalpsa":
			case "psaglobal":
				this.onGlobalPSA();
				break;
			case "privat":
			case "private":
				this.onPrivate();
				break;
			case "loadmainconfig":
			case "mainconfig":
				this.onLoadMainConfig();
				break;
			case "reload":
				this.onReload();
				break;
			case "log":
				this.onLog();
				break;
			case "helpconfig":
			case "config":
				this.onConfig();
				break;
			case "loop":
			case "schleife":
			case "repeat":
			case "wiederholen":
			case "kurikaesu":
			case "lop":
				this.onLoopMusic();
				break;
			case "hilfe":
			case "help":
				this.onHelp();
				break;
			case "xiko":
			case "xico":
			case "xikaner":
			case "xicaner":
			case "xican":
			case "ziko":
			case "zico":
			case "zikaner":
			case "zicaner":
			case "zican":
			case "siko":
			case "sico":
			case "sikaner":
			case "sicaner":
			case "sican":
			case "ciko":
			case "cico":
			case "cikaner":
			case "cicaner":
			case "cican":
			case "iko":
			case "ico":
			case "ikaner":
			case "icaner":
			case "ican":
				this.onMexico();
				break;
			default:
				// Nothing, user typed in a command that does not exist
				isCommand = false;
			}
			if(isCommand){
				QuickLogger.logInfo("Command used: "+this.commandSection+" in guild "+this.getGuildSecureName());
			}
			else if(!this.isTextCommand()){
				this.onWrongInteraction();
			}
		}
		// Reset
		this.commandSection = "";
		this.argumentSection = "";
		return true;
	}

	protected final void purge(){
		try{
			QuickLogger.logInfo("########## Purging session for guild: "+this.getGuild().getName()+" ###########");
		}
		catch(Exception e){
			QuickLogger.logMinErr("Guild name unavailable!");
		}
		
		// ########## CLEAN MUSIC SESSION ##########
			QuickLogger.logInfo("Cleaning up mussic session...");
			this.getMusicWrapper().getMusicBotHandler().clearList();
			if (this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.getMusicWrapper().getMusicBotHandler().stop();
			}
			if (this.isVoiceConnected()) {
				this.leaveVoiceChannel();
			}

			// Remove hello message
			QuickLogger.logInfo("Removing hello message...");
			try{
				this.config.helloMessage.delete().block();
			}
			catch(Exception e){
				QuickLogger.logWarn("Failed to remove message, continuing... ");
			}

			// ########## STOP SURVEYS ##########
			QuickLogger.logInfo("Stopping surveys...");

			this.getSurveyListVerbose().forEach(survey ->{
				if(survey.guildId.equals(this.config.guildId)){
					survey.stop(VoteEndReason.LOGOUT);
				}
			});

			// ########## STOP TASKS ##########
			QuickLogger.logInfo("Stopping tasks...");
			this.cleanLocalTasks();
	}

	/**
	 * Returns the order section of a Meg[ORDER] message. Returns an empty string,
	 * if there is no order section.
	 * 
	 * @return The order section of the message
	 */
	protected final String getCommandSection() {
		return this.commandSection;
	}

	/**
	 * Returns the argument section of a Meg[ORDER] [ARGUMENTS] message. Returns an
	 * empty string, if there are no arguments.
	 * 
	 * @return The argument section
	 */
	protected final String getArgumentSection() {
		return this.argumentSection;
	}

	private void kill(){
		if(this.hasPermission(SecurityLevel.ADM)){
			System.exit(1);
		}
	}

	protected void cleanLocalTasks(){
		this.localTasks.stopAll();
	}

	// ########## ABSTRACT RESPONSE METHODS ##########

	/**
	 * Invoked when this bot is greeted with "Hey Megumin"
	 */
	protected abstract void onGreeting();

	/**
	 * Shut down
	 */
	protected abstract void onLogout();

	/**
	 * Repeats the received message and deletes it, making it look like this bot
	 * wrote this message all on its own.
	 */
	protected abstract void onRepeat();

	protected abstract void noPermission();

	protected abstract void notInPrivate();

	/**
	 * Converts the argument to katakana
	 */
	protected abstract void onConvertToKatakana();

	/**
	 * Receives a link to a music source and adds it to the music queue
	 */
	protected abstract void onReceiveMusicRequest(ScheduleType scheduleType, String text);

	protected abstract void onPauseMusic();

	protected abstract void onResumeMusic();

	protected abstract void onStopMusic();

	protected abstract void onNextMusic();

	protected abstract void onMusicQueue();

	protected abstract void onClearMusicQueue();

	protected abstract void onFastForwardMusic();

	/**
	 * Start new survey
	 */
	protected abstract void onSurvey();

	/**
	 * Leave survey
	 */
	protected abstract void onSurveyLeave();

	/**
	 * Whats the time?
	 */
	protected abstract void onPostTime();

	/**
	 * Survey commands
	 */
	protected abstract void onSurveyHelp();

	/**
	 * TEST
	 */
	protected abstract void onTest();

	/**
	 * Look up word in jisho.org
	 */
	protected abstract void onJisho();

	/**
	 * Converts a text with hiragana and katakana to romaji
	 */
	protected abstract void onRomaji();

	protected abstract void onMusicHelp();

	protected abstract void onMusicVol();

	protected abstract void onMusicIdea();

	protected abstract void onStatus();

	protected abstract void onDeleteMessages();

	protected abstract void onPSA();

	protected abstract void onUpdatePSA();

	protected abstract void onChangeName();

	protected abstract void onWeather();

	protected abstract void onYesNo();

	protected abstract void onWiki();

	protected abstract void onFeedback();

	protected abstract void onMusicRandom();

	protected abstract void onChicken();

	protected abstract void onNuggets();

	protected abstract void onPommes();

	protected abstract void onVoiceStateEvent(VoiceStateUpdateEvent event);

	protected abstract void onSubscribeVoice();

	protected abstract void onUnsubscribeVoice();

	protected abstract void onGetVoiceSubscriptions();

	protected abstract void onGlobalPSA();

	protected abstract void onPrivate();

	protected abstract void onLoadMainConfig();

	protected abstract void onLog();

	protected abstract void onReload();

	protected abstract void onConfig();

	protected abstract void onWrongInteraction();

	protected abstract void onLoopMusic();

	protected abstract void onHelp();

	protected abstract void onMexico();

}
