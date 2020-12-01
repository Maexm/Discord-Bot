package system;

import java.util.ArrayList;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.voice.AudioProvider;
import exceptions.IllegalMagicException;
import musicBot.AudioEventHandler;
import security.SecurityLevel;
import start.RuntimeVariables;
import survey.Survey;

public abstract class ResponseType extends Middleware {

	public ResponseType(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
			AudioEventHandler audioEventHandler) {
		super(guildId, client, audioProvider, surveys, audioEventHandler);
	}

	/**
	 * Redirects message to appropriate method, where the actual response will be
	 * created.
	 */
	@Override
	protected final boolean handle() {
		
		final String PREFIX = RuntimeVariables.MESSAGE_PREFIX.toLowerCase();
		
		// Expressions
		switch (this.msgContent.toLowerCase()) {
		case "hey megumin":
			this.onGreeting();
		}

		// Expressions starting with prefix
		if (this.msgContent.toLowerCase().startsWith(PREFIX)) {

			// Extract order and arguments
			if (this.msgContent.contains(" ")) {
				final String[] splitted = this.msgContent.split(" ");

				int commandPos = 0;
				if(splitted[0].toLowerCase().equals(PREFIX)){
					commandPos++; // Use command in splitted[1] instead of splitted[0] - Message might be "Meg Command Args" instead of "MegCommand Args"
				}

				this.commandSection = splitted[commandPos];
				// Prefix right next to command
				if(commandPos == 0){
					this.argumentSection = this.msgContent.replaceFirst(this.commandSection + " ", "");// Do not use splitted[1], since args can have spaces as well
					this.commandSection = this.commandSection.toLowerCase().replaceFirst(PREFIX, "");
				}
				// Prefix seperated with " "
				else if(commandPos == 1){
					this.argumentSection = this.msgContent.replaceFirst(PREFIX + " " + this.commandSection + " ", "");
					this.commandSection = this.commandSection.toLowerCase();
				}
				else{
					throw new IllegalMagicException("commandPos must be 0 or 1");
				}
				
			}
			// No command section exists
			else {
				this.commandSection = this.msgContent.toLowerCase().replaceFirst(PREFIX, "");
			}

			// Redirect // TODO: Outsource
			switch (this.commandSection) {
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
			case "musik":
				this.onReceiveMusicRequest(false);
				break;
			case "prio":
			case "musicprio":
			case "musikprio":
			case "priomusic":
			case "priomusik":
			case "push":
			case "musicpush":
			case "musikpush":
			case "pushmusic":
			case "pushmusik":
			case "front":
			case "musicfront":
			case "musikfront":
				this.onReceiveMusicRequest(true);
				break;
			case "clear":
			case "musicclear":
			case "musikclear":
			case "clearmusic":
			case "clearmusik":
				this.onClearMusicQueue();
				break;
			case "pause":
			case "musicpause":
			case "musikpause":
				this.onPauseMusic();
				break;
			case "play":
			case "musikplay":
			case "musicplay":
				this.onResumeMusic();
				break;
			case "stop":
			case "musikstop":
			case "musicstop":
				this.onStopMusic();
				break;
			case "musiknxt":
			case "musicnxt":
			case "musikskip":
			case "musicskip":
			case "next":
			case "nxt":
			case "skip":
			case "skp":
			case "musiknext":
			case "musicnext":
				this.onNextMusic();
				break;
			case "musikhelp":
			case "musichelp":
			case "helpmusik":
			case "helpmusic":
				this.onMusicHelp();
				break;
			case "vol":
			case "musikvol":
			case "musicvol":
				this.onMusicVol();
				break;
			case "musikidee":
			case "musikideen":
			case "musicidee":
			case "musicideen":
			case "musicideas":
			case "musicidea":
			case "musikidea":
			case "musikideas":
				this.onMusicIdea();
				break;
			case "musikliste":
			case "musiklist":
			case "musiclist":
			case "musicliste":
			case "list":
			case "lst":
			case "ls":
			case "queue":
			case "musicqueue":
			case "musikqueue":
			case "warteschlange":
			case "schlange":
			case "musikschlange":
			case "musicschlange":
			
			case "musiklisteall":
			case "musiklistall":
			case "musiclistall":
			case "musiclisteall":
			case "listall":
			case "queueall":
			case "musicqueueall":
			case "musikqueueall":
			case "warteschlangeall":
			case "schlangeall":
			case "musikschlangeall":
			case "musicschlangeall":

				this.onMusicQueue();
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
			case "löschen":
			case "loeschen":
			case "vernebeln":
			case "del":
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
			case "rand":
			case "random":
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
				this.onFeedback();
				break;
			default:
				// Nothing, user typed in a command that does not exist
			}
		}
		return true;
	}

	private void kill(){
		if(this.hasPermission(SecurityLevel.ADM)){
			System.exit(1);
		}
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
	protected abstract void onReceiveMusicRequest(boolean isPrio);

	protected abstract void onPauseMusic();

	protected abstract void onResumeMusic();

	protected abstract void onStopMusic();

	protected abstract void onNextMusic();

	protected abstract void onMusicQueue();

	protected abstract void onClearMusicQueue();

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

}
