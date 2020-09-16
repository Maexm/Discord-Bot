package msgReceivedHandlers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import discord4j.core.GatewayDiscordClient;
import discord4j.voice.AudioProvider;
import exceptions.IllegalMagicException;
import exceptions.NoPermissionException;
import exceptions.SurveyCreateIllegalDurationException;
import japanese.Jisho;
import japanese.RomajiPreparer;
import japanese.ToKatakanaConverter;
import japanese.ToRomajiConverter;
import musicBot.AudioEventHandler;
import musicBot.MusicTrackInfo;
import reactor.core.publisher.Mono;
import security.SecurityLevel;
import security.SecurityProvider;
import services.Emoji;
import services.Help;
import services.Markdown;
import services.TimePrint;
import snowflakes.ChannelID;
import snowflakes.GuildID;
import start.RuntimeVariables;
import survey.Survey;

public class Megumin extends ResponseType {

	public Megumin(GatewayDiscordClient client, AudioProvider audioProvider,
			ArrayList<Survey> surveys, AudioEventHandler audioEventHandler) {
		super(client, audioProvider, surveys, audioEventHandler);
	}

	@Override
	protected void onGreeting() {
		this.sendInSameChannel("Hey " + this.getMessageAuthorObject().getMention() + "!");
	}

	@Override
	protected void onLogout() {
		if (SecurityProvider.hasPermission(this.getMessageAuthorObject(), SecurityLevel.ADM, this.getOwner().getId())) {
			this.sendInSameChannel("Bis bald!");
			this.audioEventHandler.clearList();
			if (this.audioEventHandler.isPlaying()) {
				this.audioEventHandler.stop();
			}
			if (this.isVoiceConnected()) {
				this.leaveVoiceChannel();
			}
			try {
				this.deleteAllMessages(ChannelID.MEGUMIN, GuildID.UNSER_SERVER);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			this.logOut();
		} else {
			this.noPermission();
		}
	}

	@Override
	protected void onRepeat() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("wie bitte? Was genau soll ich sagen?");
		} else {
			this.sendInSameChannel(">>> " + this.getArgumentSection());
			this.deleteReceivedMessage();
		}
	}

	@Override
	protected void onHelp() {
		this.sendPrivateAnswer(Help.HELPTEXT);
		if (SecurityProvider.hasPermission(this.getMessageAuthorObject(), SecurityLevel.ADM, this.getOwner().getId())) {
			this.sendPrivateAnswer(Help.ADMHELP);
		}
	}

	@Override
	protected void onConvertToKatakana() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("was genau soll ich konvertieren?");
		} else {
			String result = ToKatakanaConverter
					.testConvert(RomajiPreparer.getPrepared(this.getArgumentSection(), this.getMessageChannel()));
			this.sendInSameChannel(result + "     " + "(" + ToRomajiConverter.toRomaji(result) + ")");
		}
	}

	@Override
	protected void onSurvey() {
		String[] arguments = this.getArgumentSection().replace(" -", "-").replace("- ", "-").replace("; ", ";")
				.replace(" ;", ";").split("-");

		boolean isMulti = this.getCommandSection().equals("multiumfrage");
		switch (arguments.length) {
		// All surveys
		case 0:
			this.sendAnswer("diese Funktion ist noch nicht verfügbar!");
			break;
		// Current results/ delete all votes
		case 1:
			// View stats (TODO)
			this.sendAnswer("diese Funktion ist noch nicht verfügbar!");
			break;
		// Submit result
		case 2:
			String surveyKey = arguments[0];
			if (this.surveyExists(surveyKey)) {
				Survey survey = this.getSurveyForKey(surveyKey);
				String surveyOption = survey.getOptionTextFromIndex(arguments[1]);
				if (survey.optionExists(surveyOption)) {
					boolean inPrivate = this.isPrivate();
					if (!inPrivate) {
						this.deleteReceivedMessage();
						this.sendPrivateAnswer(
								"Du hast öffentlich für eine Umfrage abgestimmt. Ich habe diese Nachricht gelöscht,\n"
										+ "eventuell hat aber jemand schon deine tiefsten Geheimnisse gesehen! :cold_sweat:");
					}
					String resp = this.getSurveyForKey(surveyKey).receiveVote(getMessageAuthorObject(), surveyOption);
					switch (resp) {
					case Survey.VOTE_ADDED:
						this.sendPrivateAnswer("Du hast für die Umfrage '"
								+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
								+ " abgestimmt!\nDu hast abgestimmt für: '" + Markdown.toBold(surveyOption)
								+ "'\nDanke für die Teilnahme!");
						break;
					case Survey.VOTE_DELETED:
						this.sendPrivateAnswer("Deine Stimme aus der Umfrage '"
								+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
								+ " wurde entfernt!\nDu hattest vorher '" + Markdown.toBold(surveyOption)
								+ "' gewählt. ");
						break;
					case Survey.VOTE_REJECTED:
						// Not currently used
						this.sendPrivateAnswer("Mehrfache Antworten sind ausgeschaltet.\nDeine Stimme aus der Umfrage '"
								+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
								+ " ist also nicht gültig.\nWenn du deine Antwort ändern möchtest, musst du sie zunächst löschen (für die zu löschende Auswahl nochmal abstimmen!)");
						break;
					case Survey.VOTE_CHANGED:
						this.sendPrivateAnswer("Du hast für die Umfrage '"
								+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
								+ " bereits abgestimmt, aber mehrfache Antworten sind nicht erlaubt!\n"
								+ "Deine alte Stimme wurde gelöscht. Du hast jetzt für '"
								+ Markdown.toBold(surveyOption) + "' abgestimmt!");
						break;
					default:
						throw new IllegalMagicException("Invalid vote response '" + resp + "' received!");
					}
				} else {
					this.sendAnswer("diese Option existiert nicht!");
				}

			} else {
				this.sendAnswer("diese Umfrage existiert nicht!");
			}
			break;
		// Create new survey
		case 3:

			final String[] options = arguments[1].split(";");
			final String description = arguments[0];
			final String duration = arguments[2];

			try {
				new Survey(description, options, Integer.parseInt(duration), this.getMessageChannel(),
						this.getMessageAuthorObject(), this.surveys, isMulti);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				this.sendAnswer("das letzte Argument (Anzahl Minuten bis Ende) ist falsch!");
			} catch (SurveyCreateIllegalDurationException e) {
				e.printStackTrace();
				this.sendAnswer("eine Umfrage muss mindestens eine Minute dauern!");
			}
			break;
		default:
			this.sendAnswer("falsche Argumente!");
		}
	}

	@Override
	protected void onSurveyLeave() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("du musst eine Umfrage (ID) angeben! Die ID findest du in einer Umfrage immer unten!");
		} else if (this.surveyExists(this.getArgumentSection())) {
			Survey survey = this.getSurveyForKey(this.getArgumentSection());
			if (survey.userHasParticipated(this.getMessageAuthorObject())) {
				survey.removeAllVotesForUser(this.getMessageAuthorObject());
				this.deleteReceivedMessage();
				this.sendPrivateAnswer("Alle deine Stimmen für die Umfrage '"
						+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
						+ " wurden entfernt!");
			} else {
				this.sendAnswer("du hast für diese Umfrage nicht abgestimmt!");
			}
		} else {
			this.sendAnswer("diese Umfrage existiert nicht!");
		}
	}

	@Override
	protected void onSurveyHelp() {
		this.sendPrivateAnswer(Help.SURVEYHELPTEXT);
	}

	protected void onPostTime() {
		Calendar currentTime = Calendar.getInstance(RuntimeVariables.HOME_TIMEZONE);
		String dayOfWeek = currentTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG_STANDALONE, Locale.GERMAN);
		this.sendAnswer("es ist " + Markdown.toBold(dayOfWeek) + " der "
				+ Markdown.toBold(TimePrint.DD_MMMM_YYYY_HH_MM_SS(currentTime)));
	}

	@Override
	protected void onTest() {
		try {
			SecurityProvider.checkPermission(getMessageAuthorObject(), SecurityLevel.DEV, this.getOwner().getId());

			int dur = 20;
			this.sendAnswer("**Async test** - Blockiere für "+dur+" Sekunden");
			
			Mono.just("Finished!").delayElement(Duration.ofSeconds(dur)).subscribe(result -> System.out.println(result));
			

		} catch (NoPermissionException e) {
			this.noPermission();
		}
	}

	@Override
	protected void onJisho() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("du musst ein Wort nennen!");
		} else {
			String result = Jisho.buildMessage(Jisho.lookUpKeyWord(this.getArgumentSection()),
					this.getArgumentSection(), 3, 3);
			this.sendAnswer(result);
		}
	}

	@Override
	protected void onReceiveMusicRequest() {

		if (this.isAuthorVoiceConnected()) {
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("du musst mir schon sagen, was ich abspielen soll! Gib mir einen YouTube Link!");
			} else {
				if (!this.isVoiceConnected()
						|| !this.getAuthorVoiceChannel().getId().equals(this.getMyVoiceChannel().getId())) {
					this.joinVoiceChannel(this.getAuthorVoiceChannel(), this.getAudioProvider());
				}
				// MUSIC PLAYBACK
				try {
					MusicTrackInfo musicTrack = new MusicTrackInfo(this.getArgumentSection(),
							this.getMessageAuthorObject(), this.audioEventHandler, this.getMessageObject());
					this.audioEventHandler.schedule(musicTrack, this);
					this.sendAnswer("dein Track wurde hinzugefügt!");
					this.deleteReceivedMessage();

				} catch (Exception e) {
					// Should not occur
					this.sendAnswer("das ist kein gültiger YouTube-/SoundCloud-/ Bandcamp-Link!");
					if (!this.audioEventHandler.isPlaying()) {
						this.leaveVoiceChannel();
					}
					e.printStackTrace();
				}
			}
		} else {
			this.sendAnswer("du musst dafür in einem Voice Channel sein!");
		}
	}

	@Override
	protected void onPauseMusic() {
		if (this.checkMusicRights()) {
			if (!this.audioEventHandler.isPlaying()) {
				this.sendAnswer("es wird momentan keine Musik abgespielt!");
			} else if (!this.audioEventHandler.isPaused()) {
				this.audioEventHandler.pause();
				this.sendAnswer("Musik wurde pausiert! :pause_button:");
			} else {
				this.sendAnswer("Musik ist bereits pausiert! Schreib 'MegMusikPlay', um die Wiedergabe fortzuführen!");
			}
		}
	}

	@Override
	protected void onResumeMusic() {
		if (this.checkMusicRights()) {
			if (!this.audioEventHandler.isPlaying()) {
				this.sendAnswer("es wird momentan keine Musik abgespielt!");
			} else if (this.audioEventHandler.isPaused()) {
				this.audioEventHandler.pause();
				this.sendAnswer("Musik wird wieder abgespielt! :arrow_forward:");
			} else {
				this.sendAnswer("Musik spielt bereits! Schreib 'MegMusikPause', um die Wiedergabe zu pausieren!");
			}
		}
	}

	@Override
	protected void onStopMusic() {
		if (this.checkMusicRights()) {
			this.audioEventHandler.clearList();
			this.audioEventHandler.stop();
			this.sendAnswer("Musikwiedergabe wurde komplett gestoppt! :stop_button:");
		}
	}

	@Override
	protected void onNextMusic() {
		if (this.checkMusicRights()) {
			if (!this.audioEventHandler.isPlaying()) {
				this.sendAnswer("es wird nichts abgespielt!");
			} else {
				int count = 1;
				// Stop music, if queue is empty
				if (this.audioEventHandler.getListSize() == 0) {
					this.sendAnswer("keine Musik in der Warteschlange. Musik wird gestoppt.");
					this.audioEventHandler.stop();
				}
				// Queue not empty
				else {
					if (this.getArgumentSection() != "") {
						try {
							count = Integer.parseInt(this.getArgumentSection());
						} catch (Exception e) {
							// Failed to parse -> skipping 1 track
						}
					}
					this.sendAnswer("Überspringe Musik...");
				}
				this.audioEventHandler.next(count);// Count = 1, unless a different number was parsed
			}
		}

	}

	protected boolean checkMusicRights() {
		if (!this.isVoiceConnected()) {
			this.sendAnswer("Musik ist nicht an, schreib 'MegMusik URL'!");
			return false;
		} else if (!this.isAuthorVoiceConnected()) {
			this.sendAnswer("du musst dafür in einem Voice Channel sein!");
			return false;
		}
		return true;
	}

	@Override
	protected void onRomaji() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("du musst mir etwas geben, was ich umwandeln kann!");
		} else {
			this.sendAnswer(ToRomajiConverter.toRomaji(this.getArgumentSection()));
		}

	}

	@Override
	protected void onMusicHelp() {
		this.sendPrivateAnswer(Help.MUSICHELPTEXT);
	}

	@Override
	protected void onMusicVol() {
		if (this.getArgumentSection().equals("")) {
			int vol = this.audioEventHandler.getVolume();
			this.sendAnswer("die aktuelle Lautstärke ist " + vol + " " + Emoji.getVol(vol));
		} else if (SecurityProvider.hasPermission(this.getMessageAuthorObject(), SecurityLevel.ADM, this.getOwner().getId())) {
			try {
				int vol = Integer.parseInt(this.getArgumentSection());
				this.audioEventHandler.setVolume(vol);
				if (vol > 200) {
					vol = 200;
				} else if (vol < 0) {
					vol = 0;
				}
				this.sendAnswer("Lautstärke wurde auf " + vol + " gestellt! " + Emoji.getVol(vol));
			} catch (Exception e) {
				this.sendAnswer("konnte die Zahl nicht auslesen!");
			}
		} else {
			this.noPermission();
		}
	}

	@Override
	protected void noPermission() {
		this.sendAnswer("du hast dazu keine Rechte!");
	}

	@Override
	protected void onStatus() {
		this.sendInSameChannel(Markdown.toBold("STATUSINFORMATIONEN:") + "\n" + Markdown.toBold("Name: ")
				+ this.getAppInfo().getName() + "\n" + Markdown.toBold("Beschreibung: ")
				+ this.getAppInfo().getDescription() + "\n" + Markdown.toBold("Ping: ")
				+ this.getResponseTime() + "ms\n" + Markdown.toBold("Online seit: ")
				+ TimePrint.DD_MMMM_YYYY_HH_MM_SS(RuntimeVariables.START_TIME) + "\n"
				+ Markdown.toBold("Mein Entwickler: ")
				+ this.getOwner().asMember(GuildID.UNSER_SERVER).block().getDisplayName() + "\n"
				+ Markdown.toBold("Version: ") + RuntimeVariables.VERSION + " "
				+ (RuntimeVariables.IS_DEBUG ? Markdown.toBold("EXPERIMENTELL") : "")+"\n"
				+ Markdown.toBold("GitHub: ")+ RuntimeVariables.GIT_URL);
	}

	@Override
	protected void onDeleteMessages() {
		if(SecurityProvider.hasPermission(this.getMessageAuthorObject(), SecurityLevel.ADM, this.getOwner().getId())) {
			if(this.getArgumentSection().equals("")){
				this.sendAnswer("du musst mir sagen, wie viele Nachrichten ich löschen soll!");
			}
			else {
				this.deleteReceivedMessage();
				try {
					int amount = Integer.parseInt(this.getArgumentSection());
					
					// Calculate response
					final int deleted = this.deleteMessages(this.getMessageChannel().getId(), this.getMessageGuild().getId(), amount);
					if(deleted == 1) {
						this.sendAnswer("eine Nachricht gelöscht!");
					}
					else {
						this.sendAnswer(deleted+" Nachrichten gelöscht!");
					}
				} catch (NumberFormatException e) {
					this.sendAnswer("konnte deine Zahl nicht auslesen!");
				}
			}
		}
		else {
			this.noPermission();
		}
	}
}
