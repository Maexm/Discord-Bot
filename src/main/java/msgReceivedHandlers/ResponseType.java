package msgReceivedHandlers;

import java.util.ArrayList;

import discord4j.core.GatewayDiscordClient;
import discord4j.voice.AudioProvider;
import musicBot.AudioEventHandler;
import start.RuntimeVariables;
import survey.Survey;

public abstract class ResponseType extends Middleware {

	public ResponseType(GatewayDiscordClient client, AudioProvider audioProvider, ArrayList<Survey> surveys,
			AudioEventHandler audioEventHandler) {
		super(client, audioProvider, surveys, audioEventHandler);
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
				this.commandSection = this.msgContent.split(" ")[0];
				this.argumentSection = this.msgContent.replaceFirst(this.commandSection + " ", "");
				this.commandSection = this.commandSection.toLowerCase().replaceFirst(PREFIX, "");
			}
			// No command section exists
			else {
				this.commandSection = this.msgContent.toLowerCase().replaceFirst(PREFIX, "");
			}

			// Redirect
			switch (this.commandSection) {
			case "logout":
				this.onLogout();
				break;
			case "sprechen":
			case "schreiben":
				this.onRepeat();
				break;
			case "help":
				this.onHelp();
				break;
			case "katakana":
				this.onConvertToKatakana();
				break;
			case "musik":
				this.onReceiveMusicRequest();
				break;
			case "musikpause":
				this.onPauseMusic();
				break;
			case "musikplay":
				this.onResumeMusic();
				break;
			case "musikstop":
				this.onStopMusic();
				break;
			case "musiknext":
				this.onNextMusic();
				break;
			case "musikhelp":
			case "helpmusik":
				this.onMusicHelp();
				break;
			case "musikvol":
				this.onMusicVol();
				break;
			case "musikliste":
			case "musiklist":
			case "musikidee":
			case "musikideen":
				this.onMusicIdea();
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
				this.onStatus();
				break;
			case "test":
				this.onTest();
				break;
			case "delete":
			case "löschen":
			case "vernebeln":
				this.onDeleteMessages();
				break;
			default:
				// Nothing, user typed in a command that does not exist
			}
		}
		return true;
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

	/**
	 * Sends a list of existing commands
	 */
	protected abstract void onHelp();

	/**
	 * Converts the argument to katakana
	 */
	protected abstract void onConvertToKatakana();

	/**
	 * Receives a link to a music source and adds it to the music queue
	 */
	protected abstract void onReceiveMusicRequest();

	protected abstract void onPauseMusic();

	protected abstract void onResumeMusic();

	protected abstract void onStopMusic();

	protected abstract void onNextMusic();

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

}
