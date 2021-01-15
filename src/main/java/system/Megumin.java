package system;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Color;
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
import musicBot.MusicVariables;
import musicBot.MusicVariables.TrackLink;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import security.SecurityLevel;
import security.SecurityProvider;
import util.Emoji;
import util.HTTPRequests;
import util.Help;
import util.Markdown;
import util.TimePrint;
import snowflakes.ChannelID;
import snowflakes.GuildID;
import start.RuntimeVariables;
import survey.Survey;
import weather.Weather;
import wiki.Wikipedia;
import wiki.Wikipedia.WikiPage;

public class Megumin extends ResponseType {

	protected int chicken = 0;

	public Megumin(final Snowflake guildId, GatewayDiscordClient client, AudioProvider audioProvider,
			ArrayList<Survey> surveys, AudioEventHandler audioEventHandler, TaskManager<RefinedTimerTask> systemTasks) {
		super(guildId, client, audioProvider, surveys, audioEventHandler, systemTasks);
	}

	@Override
	protected void onGreeting() {
		this.sendInSameChannel("Hey " + this.getMessageAuthorObject().getMention() + "!");
	}

	@Override
	protected void onLogout() {
		if (this.hasPermission(SecurityLevel.ADM)) {
			final String logoutText = "Logout wird ausgeführt...";

			// ########## CLEAN MUSIC SESSION ##########
			System.out.println("Cleaning up mussic session...");
			Message logOutMsg = this.sendInSameChannel(logoutText + "\n" + "Musik Session wird beendet...");
			this.audioEventHandler.clearList();
			if (this.audioEventHandler.isPlaying()) {
				this.audioEventHandler.stop();
			}
			if (this.isVoiceConnected()) {
				this.leaveVoiceChannel();
			}

			// ########## CLEAN INFO CHANNEL ##########
			System.out.println("Deleting messages in info channel...");
			logOutMsg = logOutMsg.edit(edit -> edit.setContent(logoutText + "\n" + "Lösche Botnachrichten...")).block();

			try {
				this.deleteAllMessages(ChannelID.MEGUMIN, GuildID.UNSER_SERVER);
				System.out.println("Messages deleted!");
			} catch (Exception e) {
				e.printStackTrace();
				this.sendInSameChannel("Beim Löschen von Botnachrichten ist ein Fehler aufgetreten!");
			}

			// ########## STOP SURVEYS ##########
			System.out.println("Stopping surveys...");
			logOutMsg = logOutMsg.edit(edit -> edit.setContent(logoutText + "\n" + "Beende existierende Umfragen..."))
					.block();

			this.surveys.forEach(survey -> {
				survey.stop();
			});

			// ########## STOP TASKS ##########
			System.out.println("Stopping tasks...");
			logOutMsg = logOutMsg.edit(edit -> edit.setContent(logoutText + "\n" + "Beende System-Tasks..."))
					.block();
			this.cleanSystemTasks();

			// ########## LOGOUT ##########
			System.out.println("Cleanup finished, logging out!");
			logOutMsg = logOutMsg.edit(edit -> edit.setContent("Bis bald!")).block();

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
						String resp = this.getSurveyForKey(surveyKey).receiveVote(getMessageAuthorObject(),
								surveyOption);
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
								this.sendPrivateAnswer(
										"Mehrfache Antworten sind ausgeschaltet.\nDeine Stimme aus der Umfrage '"
												+ Markdown
														.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
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

			this.sendAnswer("Keine Testfunktion angegeben!");

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
	protected void onReceiveMusicRequest(boolean isPrio) {
		if (this.isAuthorVoiceConnected()) {
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("du musst mir schon sagen, was ich abspielen soll! Gib mir einen YouTube Link!\n"
						+ "Schreib " + Markdown.toCodeBlock("MegMusikIdee") + " für Anregungen!");
			} else {
				// Join authors voice channel, if bot is not connected to voice or not to the
				// same channel (only true if player was not active before)
				if (!this.isVoiceConnected()
						|| !this.getAuthorVoiceChannel().getId().equals(this.getMyVoiceChannel().getId())) {
					this.joinVoiceChannel(this.getAuthorVoiceChannel(), this.getAudioProvider());
				}
				// MUSIC PLAYBACK
				try {
					MusicTrackInfo musicTrack = new MusicTrackInfo(this.getArgumentSection(),
							this.getMessageAuthorObject(), this.audioEventHandler, this.getMessageObject(), isPrio);
					this.audioEventHandler.schedule(musicTrack, this);
					this.sendAnswer("dein Track wurde hinzugefügt!"
							+ (AudioEventHandler.MUSIC_WARN.length() > 0 ? "\n" + AudioEventHandler.MUSIC_WARN : ""));
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
		if (this.handleMusicCheck(true)) {
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
		// Interpret as music add request, if there are arguments
		if(!this.getArgumentSection().equals("")){
			this.onReceiveMusicRequest(false);
		}
		// Else interpret as music pause request
		else if (this.handleMusicCheck(true)) {
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
		if (this.handleMusicCheck(true)) {
			this.audioEventHandler.clearList();
			this.audioEventHandler.stop();
			this.sendAnswer("Musikwiedergabe wurde komplett gestoppt! :stop_button:");
		}
	}

	@Override
	protected void onNextMusic() {
		if (this.handleMusicCheck(true)) {
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
					this.sendAnswer("überspringe Musik...");
				}
				this.audioEventHandler.next(count);// Count = 1, unless a different number was parsed
			}
		}

	}

	/**
	 * 
	 * @param requireSameChannel
	 * @return true if author is connected to voice and not in private, sends an error message otherwise
	 */
	protected boolean handleMusicCheck(boolean requireSameChannel) {
		// Private chat
		if (this.isPrivate()) {
			this.notInPrivate();
			return false;
		}
		// Bot not connected to voice
		else if (!this.isVoiceConnected()) {
			this.sendAnswer("Musik ist nicht an, schreib 'MegMusik URL'!");
			return false;
			// Author not connected to voice
		} else if (!this.isAuthorVoiceConnected()) {
			this.sendAnswer("du musst dafür in einem Voice Channel sein!");
			return false;
		}
		// Author connected to different voice channel
		else if (requireSameChannel && !this.getAuthorVoiceChannel().getId().equals(this.getMyVoiceChannel().getId())) {
			this.sendAnswer("du musst dafür im selben VoiceChannel wie ich sein!");
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
		} else if (this.hasPermission(SecurityLevel.ADM)) {
			try {
				int vol = Integer.parseInt(this.getArgumentSection());
				if (vol > 200) {
					vol = 200;
				} else if (vol < 0) {
					vol = 0;
				}
				this.audioEventHandler.setVolume(vol);
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
		this.sendInSameChannel(
				Markdown.toBold("STATUSINFORMATIONEN:") + "\n" + Markdown.toBold("Name: ") + this.getAppInfo().getName()
						+ "\n" + Markdown.toBold("Beschreibung: ") + this.getAppInfo().getDescription() + "\n"
						/* + Markdown.toBold("Ping: ") + this.getResponseTime() + "ms\n" */
						+ Markdown.toBold("Online seit: ")
						+ TimePrint.DD_MMMM_YYYY_HH_MM_SS(RuntimeVariables.START_TIME) + "\n"
						+ Markdown.toBold("Mein Entwickler: ")
						+ this.getOwner().asMember(GuildID.UNSER_SERVER).block().getDisplayName() + "\n"
						+ Markdown.toBold("Version: ") + RuntimeVariables.VERSION + " "
						+ (RuntimeVariables.IS_DEBUG ? Markdown.toBold("EXPERIMENTELL") : "") + "\n"
						+ Markdown.toBold("GitHub: ") + RuntimeVariables.GIT_URL);
	}

	@Override
	protected void onDeleteMessages() {
		if (this.hasPermission(SecurityLevel.ADM)) {
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("du musst mir sagen, wie viele Nachrichten ich löschen soll!");
			} else if (this.isPrivate()) {
				this.notInPrivate();
			} else {
				this.deleteReceivedMessage();
				try {
					int amount = Integer.parseInt(this.getArgumentSection());

					// Calculate response
					final int deleted = this.deleteMessages(this.getMessageChannel().getId(),
							this.getMessageGuild().getId(), amount);
					if (deleted == 1) {
						this.sendAnswer("eine Nachricht gelöscht!");
					} else {
						this.sendAnswer(deleted + " Nachrichten gelöscht!");
					}
				} catch (NumberFormatException e) {
					this.sendAnswer("konnte deine Zahl nicht auslesen!");
				}
			}
		} else {
			this.noPermission();
		}
	}

	@Override
	protected void onMusicIdea() {
		if (MusicVariables.trackLinks.length == 0) {
			this.sendAnswer("seltsam, es gibt keine Musiktipps...\n" +
			this.getOwner().getMention()
			+ ", kannst du das bitte prüfen?");
		}
		else {
			String msg = "Hier sind einige Anregungen. Du kannst auch gerne"
			+ "Anregungen für diese Liste empfehlen!\n\n";
			for (TrackLink element : MusicVariables.trackLinks) {
				msg += element.getLink() + " " + Markdown.toBold(element.getTitle())+"\n";
			}
			this.sendAnswer(msg);
		}
	}

	@Override
	protected void onMusicQueue() {
		if (this.isVoiceConnected()) {
			AudioTrack curTrack = this.audioEventHandler.getCurrentAudioTrack();
			// Leave if no track is playing (this should not happen)
			if (curTrack == null) {
				throw new NullPointerException("There is no current track!");
			}
			String curTrackUser = AudioEventHandler.getSubmittedByUserName(curTrack, this.getMessageGuild().getId());
			// Build String

			String out = "aktuell wird abgespielt:\n" + Markdown.toBold(curTrack.getInfo().title) + " von "
					+ Markdown.toBold(curTrack.getInfo().author)
					+ (curTrackUser != null ? ", vorgeschlagen von " + Markdown.toBold(curTrackUser) : "") + "\n\n"
					+ this.audioEventHandler.getQueueInfoString();

			out += this.audioEventHandler.getListSize() > 0 ? "\n" : "";

			// Build String for each track in queue

			int parsed = -1;
			if(!this.argumentSection.equals("") && !this.argumentSection.equals("all")){
				try{
					parsed = Integer.parseInt(this.argumentSection);
				}
				catch(Exception e){
				}
			}
			final String ALL = "all";
			final LinkedList<AudioTrack> list = this.audioEventHandler.getDeepListCopy();
			// Use parsed value if valid, else use all if contains all, else use default value
			final int MAX_OUT = parsed > 0 ? parsed : (this.argumentSection.toLowerCase().equals(ALL) || this.commandSection.endsWith(ALL)) ? Integer.MAX_VALUE : 5;
			for (int i = 0; i < list.size() && i < MAX_OUT; i++) {
				final AudioTrack track = list.get(i);
				String trackUser = AudioEventHandler.getSubmittedByUserName(track, this.getMessageGuild().getId());

				out += Markdown.toBold(i + 1 + ".") + " " + Markdown.toBold(track.getInfo().title) + " von "
						+ Markdown.toBold(track.getInfo().author)
						+ (trackUser != null ? " vorgeschlagen von " + Markdown.toBold(trackUser) : "") + "\n\n";
			}
			if (list.size() > MAX_OUT) {
				final int diff = list.size() - MAX_OUT;

				out += "\nEs gibt noch " + (diff == 1 ? Markdown.toBold("einen") + " weiteren Track!"
						: Markdown.toBold("" + diff) + " weitere Tracks!");
			}
			this.sendAnswer(out);
		} else {
			this.sendAnswer("es läuft aktuell keine Musik!");
		}
	}

	@Override
	protected void onPSA() {
		if (this.hasPermission(SecurityLevel.DEV)) {
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("keine Nachricht angegeben!");
			} else {
				this.sendInChannel(this.getArgumentSection(), ChannelID.ANKUENDIGUNGEN, GuildID.UNSER_SERVER);
			}
		} else {
			this.noPermission();
		}

	}

	@Override
	protected void onClearMusicQueue() {
		if (this.handleMusicCheck(true)) {
			// List already empty
			if (this.audioEventHandler.getListSize() == 0) {
				final String[] responses = { "Warteschlange wird gele- Moment, die Liste ist schon leer!",
						"Leerer als leer kann die Liste glaub ich nicht werden!",
						"Your call was absorbed by the darkness.", "Die Warteschlange ist bereits leer!" };
				Random rand = new Random();
				this.sendAnswer(responses[rand.nextInt(responses.length)]);
			}
			// Clear list
			else {
				this.audioEventHandler.clearList();
				this.sendAnswer("Die Musikqueue ist nun leer!");
			}
		}
	}

	@Override
	protected void notInPrivate() {
		this.sendAnswer("diese Funktion ist im Privatchat nicht nutzbar!");
	}

	@Override
	protected void onChangeName() {
		if (this.hasPermission(SecurityLevel.DEV)) {
			if (this.argumentSection.equals("")) {
				this.sendAnswer("du hast keinen Namen angegeben!");
			}
			else if(this.isPrivate()){
				this.notInPrivate();
			}
			else {
				this.getMessageGuild().changeSelfNickname(this.argumentSection).block();
				this.sendAnswer("Name erfolgreich geändert!");
			}
		} else {
			this.noPermission();
		}
	}

	@Override
	protected void onUpdatePSA() {
		if (this.hasPermission(SecurityLevel.DEV)) {
			final String INTRO = "Ein neues Update ist verfügbar! :christmas_tree: Was ist neu?\n";
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("keine Nachricht angegeben!");
			} else if(!RuntimeVariables.IS_DEBUG){
				this.sendInChannel(
						INTRO + Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()),
						ChannelID.ANKUENDIGUNGEN, GuildID.UNSER_SERVER);
			}
			else{
				this.sendMessageToOwner(INTRO + Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()));
			}
		} else {
			this.noPermission();
		}
	}

	@Override
	protected void onWeather() {
		String city = this.getArgumentSection().equals("") ? RuntimeVariables.HOME_TOWN : this.getArgumentSection();

		String resp = Weather.buildMessage(Weather.getWeatherResponse(city));
		this.sendAnswer(resp);
	}

	@Override
	protected void onYesNo() {
		if(this.getArgumentSection().equals("")){
			this.sendAnswer("du musst mir eine Frage stellen!");
			return;
		}

		final String[] posResp = { "ja.", "tatsache.", "das ist sehr wahrscheinlich.", "völlig korrekt.", "in der Tat.",
				"absolut.", "aber natürlich!", "auf jeden Fall!", "definitiv.", "daran besteht kein Zweifel.",
				"davon würde ich definitiv ausgehen.", "zweifellos!", "offensichtlich ja.", "yes.", "jawohl!", "klar!",
				"rein objektiv gesehen: Ja.", "jup.", "jep.", "jo.", "selbstverständlich!", "klingt nach einer hervorragenden Idee!", ":thumbsup:" };
		final String[] negResp = { "nein.", "überhaupt nicht.", "auf garkeinen Fall.", "völlig unmöglich.",
				"definitiv nicht.", "no!", "offensichtlich nicht.", "das ist nicht möglich.",
				"davon würde ich sowas von abraten!", "bloß nicht!", "rein objektiv gesehen: Nein.",
				"nein, wieso auch?", "eher nicht.", "ne.", "nope.", "nö", "nichts da!", "klingt wenn du mich fragst nach einer scheiß Idee.", ":thumbsdown:" };
		final Random rand = new Random();

		this.sendAnswer(rand.nextBoolean() ? posResp[rand.nextInt(posResp.length)] : negResp[rand.nextInt(negResp.length)]);
	}

	@Override
	protected void onWiki() {
		if(this.argumentSection.equals("")){
			this.sendAnswer("du musst mir einen Begriff nennen!");
			return;
		}

		final Message loadingMessage = this.sendAnswer("einen Moment bitte, ich versuche auf Wikipedia etwas geeignetes zu finden...");

		WikiPage page = Wikipedia.getWikiPage(this.argumentSection);

		if(page == null){
            this.sendAnswer("konnte unter diesem Begriff nichts finden!");
        }
        if(page.CUSTOM_PROP_LANGUAGE == null || page.CUSTOM_PROP_LANGUAGE.equals("")){
            throw new IllegalMagicException("CUSTOM_PROP_LANGUAGE must not be null or empty");
        }

		final String humanUrl = "https://"+page.CUSTOM_PROP_LANGUAGE+"."+Wikipedia.WIKI_NORMAL_BASE_URL + HTTPRequests.urlEncode(page.title.replace(" ", "_"));
		
		this.getMessageChannel().createEmbed(spec -> {
				spec.setColor(Color.CYAN)
				.setTitle(page.title)
				.setUrl(humanUrl)
				.setDescription(page.extract);

				if(page.original != null){
					spec.setImage(page.original.source);
				}
			}).block();

			loadingMessage.delete("Deleting info concerning async tasks");
	}

	@Override
	protected void onFeedback() {
		if(this.getArgumentSection().equals("")){
			this.sendAnswer("ein leeres Feedback ist kein Feedback!");
			return;
		}
		String response = "Wir haben Feedback von "+Markdown.toBold(this.getMessageAuthorName())+" erhalten:\n"
							+ Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection());
		this.sendMessageToOwner(response);
		this.sendPrivateAnswer("Vielen Dank! Ich habe dein Feedback an "+this.getOwner().getUsername()+" weitergeleitet! "
								+"Er wird bei Rückfragen auf dich zukommen. Falls du zunächst keine Rückmeldung bekommst heißt das, dass dein Feedback ohne Rückfragen akzeptiert wurde :smile:\n"
								+Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()));
		this.deleteReceivedMessage();
	}

	@Override
	protected void onFastForwardMusic() {
		if(!this.handleMusicCheck(true)){			
			return;
		}

		if(this.getArgumentSection().equals("")){
			this.sendAnswer("mir ist nicht klar, wie viel ich skippen soll!");
			return;
		}

		int skipSeconds = 0;
		try{
			skipSeconds = Integer.parseInt(this.getArgumentSection());
			this.audioEventHandler.fastForward(skipSeconds * 1000);
		}
		catch(NumberFormatException e){
			this.sendAnswer("ungültige Eingabe. Gib in Sekunden an, wie viel ich skippen soll. Negative Zahlen gehen auch!");
		}
	}

	@Override
	protected void onMusicRandom() {
		if(this.handleMusicCheck(true)){
			if(this.audioEventHandler.getListSize() >= 1){
				this.sendAnswer("es müssen mindestens zwei Tracks in der Warteschlange sein, sonst macht das ganze keinen Sinn!");
			}
			else{
				this.audioEventHandler.randomize();
				this.sendAnswer("*ratter* *ratter* *schüttel* *schüttel* Die Warteschlange wurde einmal kräftig durchgemischt!");
			}
		}
	}

	@Override
	protected void onChicken() {
		if(this.chicken == Integer.MAX_VALUE){
			this.sendAnswer("Mehr geht nicht!");
			return;
		}

		final String appendString = ":chicken: ";

		this.chicken++;
		StringBuilder response = new StringBuilder(this.chicken * appendString.length());
		for(int i = 0; i < this.chicken; i++){
			response.append(appendString);
		}
		this.sendAnswer(response.toString());
	}

	@Override
	protected void onNuggets() {
		String response = "";
		
		if(this.chicken == 0){
			response = "keine Zutaten für Nuggets verfügbar!";
		}
		else if(this.chicken == 1){
			response = "hier hast du eine Portion Chicken Nuggets!";
		}
		else if(this.chicken == 2){
			response = "hier sind Chicken Nuggets für zwei Personen!";
		}
		else if(this.chicken >= 3){
			response = "hier sind "+this.chicken+" Portionen Chicken Nuggets!";
		}
		else if(this.chicken >= 10){
			response = "hier sind "+this.chicken+" Portionen Chicken Nuggets, genug für eine Großfamilie!";
		}
		else if(this.chicken >= 50){
			response = "hier ist deine McDonalds Tageslieferung an Chicken Nuggets ("+this.chicken+" Portionen)";
		}
		else if(this.chicken == 420){
			response = "diese "+this.chicken+" Chicken Nuggets riechen seltsam...";
		}
		else if(this.chicken >= 200){
			response = this.chicken+" Hühner wurden gewaltsam umgebra- Hier sind deine Chicken Nuggets, Mahlzeit!";
		}
		else if(this.chicken == Integer.MAX_VALUE){
			response = "du hast das Maximum an Chicken Nuggets erreicht ("+this.chicken+")! Das ist ein Achievement Wert aber sowas gibt es bei mir nicht!";
		}

		this.sendAnswer(response);
		this.chicken = 0;
	}

	@Override
	protected void onPommes() {
		this.sendAnswer("Pommes gibt es bei mir nicht!");
	}
}
