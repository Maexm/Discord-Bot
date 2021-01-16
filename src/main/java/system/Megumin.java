package system;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Color;
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
import musicBot.MusicTrackInfo.ScheduleType;
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
import survey.VoteChange;
import weather.Weather;
import wiki.Wikipedia;
import wiki.Wikipedia.WikiPage;

public class Megumin extends ResponseType {

	protected int chicken = 0;

	public Megumin(MiddlewareConfig config, TaskManager<RefinedTimerTask> localTasks) {
		super(config, localTasks);
	}

	@Override
	protected void onGreeting() {
		this.sendInSameChannel("Hey " + this.getMessage().getUser().getMention() + "!");
	}

	@Override
	protected void onLogout() {
		if (this.hasPermission(SecurityLevel.ADM)) {
			Message logOutMsg = this.sendInSameChannel("Logout wird ausgeführt. Das kann kurz etwas dauern...");

			this.getGlobalProxy().purgeAllGuilds();

			// ########## LOGOUT ##########
			System.out.println("Logging out!");
			logOutMsg = logOutMsg.edit(edit -> edit.setContent("Bis bald!")).block();

			this.getGlobalProxy().logout();
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
				Snowflake userId = this.getMessage().getUser().getId();
				if (this.surveyExistsUserScoped(surveyKey, userId)) {
					Survey survey = this.getSurveyForKeyUserScoped(surveyKey, userId);
					String surveyOption = survey.getOptionTextFromIndex(arguments[1]);
					if (survey.optionExists(surveyOption)) {
						boolean inPrivate = this.isPrivate();
						if (!inPrivate) {
							this.deleteReceivedMessage();
							this.sendPrivateAnswer(
									"Du hast öffentlich für eine Umfrage abgestimmt. Ich habe diese Nachricht gelöscht,\n"
											+ "eventuell hat aber jemand schon deinen geheimen Vote gesehen! :cold_sweat:");
						}
						VoteChange resp = this.getSurveyForKeyUserScoped(surveyKey, userId).receiveVote(this.getMessage().getUser(),
								surveyOption);
						switch (resp) {
							case ADDED:
								this.sendPrivateAnswer("Du hast für die Umfrage '"
										+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
										+ " abgestimmt!\nDu hast abgestimmt für: '" + Markdown.toBold(surveyOption)
										+ "'\nDanke für die Teilnahme!");
								break;
							case DELETED:
								this.sendPrivateAnswer("Deine Stimme aus der Umfrage '"
										+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
										+ " wurde entfernt!\nDu hattest vorher '" + Markdown.toBold(surveyOption)
										+ "' gewählt. ");
								break;
							case REJECTED:
								// Not currently used
								this.sendPrivateAnswer(
										"Mehrfache Antworten sind ausgeschaltet.\nDeine Stimme aus der Umfrage '"
												+ Markdown
														.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
												+ " ist also nicht gültig.\nWenn du deine Antwort ändern möchtest, musst du sie zunächst löschen (für die zu löschende Auswahl nochmal abstimmen!)");
								break;
							case CHANGED:
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
					this.sendAnswer("diese Umfrage existiert nicht oder ist für dich nicht verfügbar!");
				}
				break;
			// Create new survey
			case 3:

				final String[] options = arguments[1].split(";");
				final String description = arguments[0];
				final String duration = arguments[2];

				try {
					new Survey(description, options, Integer.parseInt(duration), this.getMessageChannel(),
					this.getMessage().getUser(), this.getSurveyListVerbose(), isMulti);
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
		Snowflake userId = this.getMessage().getUser().getId();
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("du musst eine Umfrage (ID) angeben! Die ID findest du in einer Umfrage immer unten!");
		} else if (this.surveyExistsUserScoped(this.getArgumentSection(), userId)) {
			Survey survey = this.getSurveyForKeyUserScoped(this.getArgumentSection(), userId);
			if (survey.userHasParticipated(this.getMessage().getUser())) {
				survey.removeAllVotesForUser(this.getMessage().getUser());
				this.deleteReceivedMessage();
				this.sendPrivateAnswer("Alle deine Stimmen für die Umfrage '"
						+ Markdown.toBold(survey.description + "' (" + survey.getIDPrint() + ")")
						+ " wurden entfernt!");
			} else {
				this.sendAnswer("du hast für diese Umfrage nicht abgestimmt!");
			}
		} else {
			this.sendAnswer("diese Umfrage existiert nicht oder ist für dich nicht verfügbar!");
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
			SecurityProvider.checkPermission(this.getMessage().getUser(), SecurityLevel.DEV, this.getOwner().getId());

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
	protected void onReceiveMusicRequest(ScheduleType scheduleType) {
		if (this.isAuthorVoiceConnected()) {
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("du musst mir schon sagen, was ich abspielen soll! Gib mir einen YouTube Link oder einen Suchbegriff!");
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
					this.getMessage().getUser(), this.getMusicWrapper().getMusicBotHandler(), this.getMessage().getMessageObject(), scheduleType);
					this.getMusicWrapper().getMusicBotHandler().schedule(musicTrack, this);
					this.sendAnswer("dein Track wurde hinzugefügt!"
							+ (AudioEventHandler.MUSIC_WARN.length() > 0 ? "\n" + AudioEventHandler.MUSIC_WARN : ""));
					this.deleteReceivedMessage();

				} catch (Exception e) {
					// Should not occur
					this.sendAnswer("das ist kein gültiger YouTube-/SoundCloud-/ Bandcamp-Link!");
					if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
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
			if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.sendAnswer("es wird momentan keine Musik abgespielt!");
			} else if (!this.getMusicWrapper().getMusicBotHandler().isPaused()) {
				this.getMusicWrapper().getMusicBotHandler().pause();
				this.sendAnswer("Musik wurde pausiert! :pause_button:");
			} else {
				this.sendAnswer("Musik ist bereits pausiert! Schreib 'MegPlay', um die Wiedergabe fortzuführen!");
			}
		}
	}

	@Override
	protected void onResumeMusic() {
		// Interpret as music add request, if there are arguments
		if(!this.getArgumentSection().equals("")){
			this.onReceiveMusicRequest(ScheduleType.NORMAL);
		}
		// Else interpret as music pause request
		else if (this.handleMusicCheck(true)) {
			if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.sendAnswer("es wird momentan keine Musik abgespielt!");
			} else if (this.getMusicWrapper().getMusicBotHandler().isPaused()) {
				this.getMusicWrapper().getMusicBotHandler().pause();
				this.sendAnswer("Musik wird wieder abgespielt! :arrow_forward:");
			} else {
				this.sendAnswer("Musik spielt bereits! Schreib 'MegPause', um die Wiedergabe zu pausieren!");
			}
		}
	}

	@Override
	protected void onStopMusic() {
		if (this.handleMusicCheck(true)) {
			this.getMusicWrapper().getMusicBotHandler().clearList();
			this.getMusicWrapper().getMusicBotHandler().stop();
			this.sendAnswer("Musikwiedergabe wurde komplett gestoppt! :stop_button:");
		}
	}

	@Override
	protected void onNextMusic() {
		if (this.handleMusicCheck(true)) {
			if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.sendAnswer("es wird nichts abgespielt!");
			} else {
				int count = 1;
				// Stop music, if queue is empty
				if (this.getMusicWrapper().getMusicBotHandler().getListSize() == 0) {
					this.sendAnswer("keine Musik in der Warteschlange. Musik wird gestoppt.");
					this.getMusicWrapper().getMusicBotHandler().stop();
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
				this.getMusicWrapper().getMusicBotHandler().next(count);// Count = 1, unless a different number was parsed
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
			int vol = this.getMusicWrapper().getMusicBotHandler().getVolume();
			this.sendAnswer("die aktuelle Lautstärke ist " + vol + " " + Emoji.getVol(vol));
		} else if (this.hasPermission(SecurityLevel.KREIS) || this.authorIsGuildOwner()) {
			try {
				int vol = Integer.parseInt(this.getArgumentSection());
				vol = Math.max(0, vol);
				vol = Math.min(200, vol);
				this.getMusicWrapper().getMusicBotHandler().setVolume(vol);
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
						+ this.getOwner().getUsername() + "\n"
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
							this.config.guildId, amount);
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
		if (this.handleMusicCheck(false)) {
			AudioTrack curTrack = this.getMusicWrapper().getMusicBotHandler().getCurrentAudioTrack();
			// Leave if no track is playing (this should not happen)
			if (curTrack == null) {
				throw new NullPointerException("There is no current track!");
			}
			String curTrackUser = AudioEventHandler.getSubmittedByUserName(curTrack, this.config.guildId);
			// Build String

			String out = "aktuell wird abgespielt:\n" + Markdown.toBold(curTrack.getInfo().title) + " von "
					+ Markdown.toBold(curTrack.getInfo().author)
					+ (curTrackUser != null ? ", vorgeschlagen von " + Markdown.toBold(curTrackUser) : "") + "\n\n"
					+ this.getMusicWrapper().getMusicBotHandler().getQueueInfoString();

			out += this.getMusicWrapper().getMusicBotHandler().getListSize() > 0 ? "\n" : "";

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
			final LinkedList<AudioTrack> list = this.getMusicWrapper().getMusicBotHandler().getDeepListCopy();
			// Use parsed value if valid, else use all if contains all, else use default value
			final int MAX_OUT = parsed > 0 ? parsed : (this.argumentSection.toLowerCase().equals(ALL) || this.commandSection.endsWith(ALL)) ? Integer.MAX_VALUE : 5;
			for (int i = 0; i < list.size() && i < MAX_OUT; i++) {
				final AudioTrack track = list.get(i);
				String trackUser = AudioEventHandler.getSubmittedByUserName(track, this.config.guildId);

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
				this.sendInChannel(this.getArgumentSection(), this.getSystemChannel().getId());
			}
		} else {
			this.noPermission();
		}

	}

	@Override
	protected void onClearMusicQueue() {
		if (this.handleMusicCheck(true)) {
			// List already empty
			if (this.getMusicWrapper().getMusicBotHandler().getListSize() == 0) {
				final String[] responses = { "Warteschlange wird gele- Moment, die Liste ist schon leer!",
						"Leerer als leer kann die Liste glaub ich nicht werden!",
						"Your call was absorbed by the darkness.", "Die Warteschlange ist bereits leer!" };
				Random rand = new Random();
				this.sendAnswer(responses[rand.nextInt(responses.length)]);
			}
			// Clear list
			else {
				this.getMusicWrapper().getMusicBotHandler().clearList();
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
				this.getGuildByID(this.config.guildId).changeSelfNickname(this.argumentSection).block();
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
						this.getSystemChannel().getId());
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
		String response = "Wir haben Feedback von "+Markdown.toBold(this.getMessage().getAuthorName())+" erhalten:\n"
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
			this.getMusicWrapper().getMusicBotHandler().jump(skipSeconds * 1000);
		}
		catch(NumberFormatException e){
			this.sendAnswer("ungültige Eingabe. Gib in Sekunden an, wie viel ich skippen soll. Negative Zahlen gehen auch!");
		}
	}

	@Override
	protected void onMusicRandom() {
		if(this.handleMusicCheck(true)){
			if(this.getMusicWrapper().getMusicBotHandler().getListSize() >= 1){
				this.sendAnswer("es müssen mindestens zwei Tracks in der Warteschlange sein, sonst macht das ganze keinen Sinn!");
			}
			else{
				this.getMusicWrapper().getMusicBotHandler().randomize();
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

		final String appendString = ":chicken:";

		this.chicken++;

		if(this.chicken <= 200){
			StringBuilder response = new StringBuilder(this.chicken * appendString.length());
			for(int i = 0; i < this.chicken; i++){
				response.append(appendString);
			}
			this.sendAnswer(response.toString());
		}
		else{
			this.sendAnswer(this.chicken+" "+appendString);
		}
	}

	@Override
	protected void onNuggets() {
		String response = "";

		switch(this.chicken){
			case 0:
				response = "keine Zutaten für Nuggets verfügbar!";
				break;
			case 1:
				response = "hier hast du eine Portion Chicken Nuggets!";
				break;
			case 2:
				response = "hier sind Chicken Nuggets für zwei Personen!";
				break;
			case 3:
				response = "hier sind 3 Chicken Nuggets für dich und deine Freunde!";
				break;
			case 420:
				response = "diese 420 Chicken Nuggets riechen seltsam...";
				break;
			default:
				if(this.chicken >= 4){
					response = "hier sind "+this.chicken+" Portionen Chicken Nuggets!";
				}
				else if(this.chicken >= 10){
					response = "hier sind "+this.chicken+" Portionen Chicken Nuggets, genug für eine Großfamilie!";
				}
				else if(this.chicken >= 50){
					response = "hier ist deine McDonalds Tageslieferung an Chicken Nuggets ("+this.chicken+" Portionen)";
				}
				else if(this.chicken >= 200){
					response = this.chicken+" Hühner wurden gewaltsam umgebra- Hier sind deine Chicken Nuggets, Mahlzeit!";
				}
				else if(this.chicken == Integer.MAX_VALUE){
					response = "du hast das Maximum an Chicken Nuggets erreicht ("+this.chicken+")! Das ist ein Achievement Wert aber sowas gibt es bei mir nicht!";
				}
		}
		
		this.sendAnswer(response);
		this.chicken = 0;
	}

	@Override
	protected void onPommes() {
		this.sendAnswer("Pommes gibt es bei mir nicht!");
	}
}
