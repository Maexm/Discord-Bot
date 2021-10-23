package system;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.TimeZone;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import exceptions.IllegalMagicException;
import exceptions.NoPermissionException;
import exceptions.SurveyCreateIllegalDurationException;
import japanese.Jisho;
import japanese.RomajiPreparer;
import japanese.ToKatakanaConverter;
import japanese.ToRomajiConverter;
import logging.QuickLogger;
import musicBot.AudioEventHandler;
import musicBot.MusicTrackInfo;
import musicBot.MusicVariables;
import musicBot.MusicTrackInfo.ScheduleType;
import musicBot.MusicVariables.TrackLink;
import reactor.core.publisher.Mono;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import security.SecurityLevel;
import util.Emoji;
import util.HTTPRequests;
import util.Help;
import util.Markdown;
import util.Pair;
import util.StringUtils;
import util.Time;
import util.TimePrint;
import start.RuntimeVariables;
import start.StartUp;
import survey.Survey;
import survey.VoteChange;
import translator.TranslatorResponse;
import translator.LanguageResponse.Language;
import weather.Weather;
import wiki.Wikipedia;
import wiki.Wikipedia.WikiPage;

public class Megumin extends ResponseType {

	protected int chicken = 0;

	public Megumin(MiddlewareConfig config, TaskManager<RefinedTimerTask> localTasks) {
		super(config, localTasks);
		if (!RuntimeVariables.isDebug() && !this.isPrivate()){
			try {
				// MessageChannel channel = this.getPsaChannel(false);
				// Message message = channel
				// 		.createMessage(
				// 				"Ich bin Online und einsatzbereit! Schreib " + Markdown.toCodeBlock("MegHelp") + "!")
				// 		.block();
				// this.config.helloMessage = message;
			} catch (Exception e) {
				QuickLogger.logMinErr("Failed to post hello message in guild " + this.getGuildSecureName());
			}
		}
	}

	@Override
	protected void onGreeting() {
		this.sendInSameChannel("Hey " + this.getMessage().getUser().getMention() + "!");
	}

	@Override
	protected void onLogout() {
		if (this.hasPermission(SecurityLevel.ADM)) {
			Message logOutMsg = this.sendInSameChannel("Logout wird ausgeführt. Das kann kurz etwas dauern...");

			this.getClient().updatePresence(Presence.doNotDisturb(Activity.playing("Herunterfahren..."))).block();
			this.getGlobalProxy().purgeAllGuilds();

			// ########## LOGOUT ##########
			QuickLogger.logInfo("Logging out!");
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
				.replace(" ;", ";").split(";");

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
						if (!inPrivate && this.isTextCommand()) {
							this.deleteReceivedMessage();
							this.sendPrivateAnswer(
									"Du hast öffentlich für eine Umfrage abgestimmt. Ich habe diese Nachricht gelöscht,\n"
											+ "eventuell hat aber jemand schon deinen geheimen Vote gesehen! :cold_sweat:");
						}
						VoteChange resp = this.getSurveyForKeyUserScoped(surveyKey, userId)
								.receiveVote(this.getMessage().getUser(), surveyOption);
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
						if(!this.isTextCommand()){
							this.sendAnswer("Siehe Privatchat :)");
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
				if(this.isPrivate()){
					this.notInPrivate();
					return;
				}

				final String[] options = arguments[1].split("-");
				final String description = arguments[0];
				final String duration = arguments[2];

				try {
					new Survey(description, options, Integer.parseInt(duration), this.getMessageChannel(),
							this.getMessage().getUser(), this.getSurveyListVerbose(), isMulti, this.getGuildId());
					if(!this.isTextCommand()){
						this.sendAnswer("Umfrage erstellt!");
					}
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
		this.sendPrivateAnswer(Help.SURVEYHELPTEXT());
	}

	protected void onPostTime() {
		Calendar currentTime = Calendar.getInstance(RuntimeVariables.getInstance().getTimezone());
		String dayOfWeek = currentTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG_STANDALONE, Locale.GERMAN);
		this.sendAnswer("es ist " + Markdown.toBold(dayOfWeek) + " der "
				+ Markdown.toBold(TimePrint.DD_MMMM_YYYY_HH_MM_SS(currentTime)));
	}

	@Override
	protected void onTest() {
		try {
			this.config.getSecurityProvider().checkPermission(this.getMessage().getUser(), SecurityLevel.DEV);

			//this.getGuild().getadmin

			this.sendAnswer("Keine Testfunktion angegeben!");
			this.sendAnswer("TEST");

		} catch (NoPermissionException e) {
			this.noPermission();
		}
	}

	@Override
	protected void onJisho() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("du musst ein Wort nennen!");
		} else {
			Optional<Message> msg = this.sendAnswer("Jisho wird durchsucht, einen Moment...");
			String result = Jisho.buildMessage(Jisho.lookUpKeyWord(this.getArgumentSection()),
					this.getArgumentSection(), 3, 3);
			if(msg.isEmpty()){
				this.sendAnswer(result);
			}
			else{
				msg.get().edit(spec -> spec.setContent(result)).block();
			}
		}
	}

	@Override
	protected void onReceiveMusicRequest(ScheduleType scheduleType, String query) {
		if (this.handleMusicCheck(true, false)) {
			if (query == null || query.equals("")) {
				this.sendAnswer(
						"du musst mir schon sagen, was ich abspielen soll! Gib mir einen YouTube Link oder einen Suchbegriff!");
			} else {
				// Join authors voice channel, if bot is not connected to voice or not to the
				// same channel (only true if player was not active before)
				if (!this.isVoiceConnected()
						|| !this.getAuthorVoiceChannel().getId().equals(this.getMyVoiceChannel().getId())) {
							try{
								this.joinVoiceChannel(this.getAuthorVoiceChannel(), this.getAudioProvider());
							} catch(Exception e){
								this.sendAnswer("kann deinem VoiceChannel nicht beitreten :anger: Möglicherweise fehlen mir dafür Rechte! Frag bitte deinen lokalen Administrator/ Moderator.");
								return;
							}
				}
				// MUSIC PLAYBACK
				Optional<Message> infoMsg = this.sendAnswer("dein Trackvorschlag wird verarbeitet :mag:", true);
				Optional<InteractionCreateEvent> interaction = this.getMessage().getInteraction();
				try {
					MusicTrackInfo musicTrack = new MusicTrackInfo(query,
							this.getMessage().getUser(), this.getMusicWrapper().getMusicBotHandler(),
							this.getMessage(),
							(String msg) -> {
								if(infoMsg.isPresent()){
									return infoMsg.get().edit(spec -> spec.setContent(msg)).onErrorResume(err -> null).then();
								}
								else{
									if(interaction.isPresent()){
										return interaction.get().getInteractionResponse().createFollowupMessage(msg).onErrorResume(err -> null).then();
									}
									return Mono.empty();
								}
							},
							scheduleType, this.getMusicWrapper().getSpotifyResolver());
					
					this.getMusicWrapper().getMusicBotHandler().schedule(musicTrack, this);
				} catch (Exception e) {
					this.sendAnswer("das ist kein gültiger YouTube-/SoundCloud-/Bandcamp- oder Spotify-Link! :x:");
					if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
						this.leaveVoiceChannel();
					}
					if(infoMsg.isPresent()){
						infoMsg.get().delete().block();
					}
					e.printStackTrace();
				}
				this.deleteReceivedMessage();
			}
		}
	}

	@Override
	protected void onPauseMusic() {
		if (this.handleMusicCheck(true, true)) {
			if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.sendAnswer("es wird momentan keine Musik abgespielt!");
			} else if (!this.getMusicWrapper().getMusicBotHandler().isPaused()) {
				this.getMusicWrapper().getMusicBotHandler().pause();
				this.sendAnswer("Musik wurde pausiert! :pause_button:");
				this.deleteReceivedMessage();
			} else if(this.getArgumentSection().equals("")){
				this.onResumeMusic();
			}
		}
	}

	@Override
	protected void onResumeMusic() {
		// Interpret as music add request, if there are arguments
		if (!this.getArgumentSection().equals("")) {
			this.onReceiveMusicRequest(ScheduleType.NORMAL, this.getArgumentSection());
		}
		// Else interpret as music resume request
		else if (this.handleMusicCheck(true, true)) {
			if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.sendAnswer("es wird momentan keine Musik abgespielt!");
			} else if (this.getMusicWrapper().getMusicBotHandler().isPaused()) {
				this.getMusicWrapper().getMusicBotHandler().pause();
				this.sendAnswer("Musik wird wieder abgespielt! :arrow_forward:");
				this.deleteReceivedMessage();
			} else {
				this.sendAnswer("Musik spielt bereits! Schreib 'MegPause', um die Wiedergabe zu pausieren!");
			}
		}
	}

	@Override
	protected void onStopMusic() {
		if (this.handleMusicCheck(true, true)) {
			this.getMusicWrapper().getMusicBotHandler().clearList();
			this.getMusicWrapper().getMusicBotHandler().stop();
			this.sendAnswer("Musikwiedergabe wurde komplett gestoppt!");
			this.deleteReceivedMessage();
		}
	}

	@Override
	protected void onNextMusic() {
		if (this.handleMusicCheck(true, true)) {
			if (!this.getMusicWrapper().getMusicBotHandler().isPlaying()) {
				this.sendAnswer("es wird nichts abgespielt!");
				this.deleteReceivedMessage();
			} else {
				int count = 1;
				// Stop music, if queue is empty
				if (this.getMusicWrapper().getMusicBotHandler().getListSize() == 0) {
					this.sendAnswer("keine Musik in der Warteschlange. Musik wird gestoppt.");
					this.deleteReceivedMessage();
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
					this.deleteReceivedMessage();
				}
				this.getMusicWrapper().getMusicBotHandler().next(count);// Count = 1, unless a different number was
																		// parsed
			}
		}

	}

	/**
	 * 
	 * @param requireSameChannel
	 * @return true if author is connected to voice and not in private, sends an
	 *         error message otherwise
	 */
	protected boolean handleMusicCheck(boolean requireSameChannel, boolean requireBotConnected) {
		// Private chat
		if (this.isPrivate()) {
			this.notInPrivate();
			return false;
		}
		// Author not connected to voice
	    else if (!this.isAuthorVoiceConnectedGuildScoped()) {
			this.sendAnswer("du musst dafür in einem Voice Channel sein!");
			return false;
		}
		// Bot not connected to voice
		else if (!this.isVoiceConnected() && requireBotConnected) {
			this.sendAnswer("Musik ist nicht an, schreib 'MegMusik URL'!");
			return false;
		}
		// Author connected to different voice channel
		else if (requireBotConnected && requireSameChannel && !this.getAuthorVoiceChannel().getId().equals(this.getMyVoiceChannel().getId())) {
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
		this.sendPrivateAnswer(Help.MUSICHELPTEXT());
	}

	@Override
	protected void onMusicVol() {
		if (this.getArgumentSection().equals("")) {
			int vol = this.getMusicWrapper().getMusicBotHandler().getVolume();
			this.sendAnswer("die aktuelle Lautstärke ist " + vol + " " + Emoji.getVol(vol));
		} else if (this.hasPermission(SecurityLevel.GUILD_SPECIAL)) {
			try {
				int vol = Integer.parseInt(this.getArgumentSection());
				vol = Math.max(0, vol);
				vol = Math.min(200, vol);
				this.getMusicWrapper().getMusicBotHandler().setVolume(vol);
				this.sendAnswer("Lautstärke wurde auf " + vol + " gestellt! Das kann kurz ein paar Sekunden dauern..." + Emoji.getVol(vol));
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
						+ Markdown.toBold("Online seit: ") + TimePrint.DD_MMMM_YYYY_HH_MM_SS(RuntimeVariables.getStartDate()) + "\n"
						+ Markdown.toBold("Anzahl Server: ") + this.getGlobalProxy().getClient().getGuilds().buffer().next().map(guildList -> guildList.size()).block()+"\n"
						+ Markdown.toBold("Mein Entwickler: ") + this.getOwner().getUsername() + "\n"
						+ Markdown.toBold("Version: ") + RuntimeVariables.getInstance().getVersion()+ " " + (RuntimeVariables.isDebug() ? Markdown.toBold("EXPERIMENTELL") : "") + "\n"
						+ Markdown.toBold("GitHub: ") + RuntimeVariables.getInstance().getGitUrl());
	}

	@Override
	protected void onDeleteMessages() {
		if(!this.hasPermission(SecurityLevel.GUILD_ADM)){
			this.noPermission();
			return;
		}

		if(this.getArgumentSection().equals("")){
			this.sendAnswer("du musst mir sagen, wie viele Nachrichten ich löschen soll!");
			return;
		}

		try {
			int amount = Integer.parseInt(this.getArgumentSection());
			boolean reduced = false;
			int maxAllowed = 20;

			if(amount > maxAllowed){
				reduced = true;
				amount = maxAllowed;
			}

			final int deleted = this.deleteMessages(this.getMessageChannel().getId(), amount);
			this.sendAnswer(deleted == 1 ? Markdown.toBold("eine")+" Nachricht gelöscht!" : (Markdown.toBold(""+deleted) + " Nachrichten gelöscht!" + (reduced ? " Aus Sicherheits Gründen kannst du nicht mehr als "+maxAllowed+" gleichzeitig Nachrichten löschen!" : "")));
		} catch (NumberFormatException e) {
			this.sendAnswer("konnte deine Zahl nicht auslesen!");
		}
	}

	@Override
	protected void onMusicIdea() {
		if (MusicVariables.trackLinks.length == 0) {
			this.sendAnswer("seltsam, es gibt keine Musiktipps...\n" + this.getOwner().getMention()
					+ ", kannst du das bitte prüfen?");
		} else {
			String msg = "Hier sind einige Anregungen. Du kannst auch gerne"
					+ "Anregungen für diese Liste empfehlen!\n\n";
			for (TrackLink element : MusicVariables.trackLinks) {
				msg += element.getLink() + " " + Markdown.toBold(element.getTitle()) + "\n";
			}
			this.sendAnswer(msg);
		}
	}

	@Override
	protected void onMusicQueue() {
		if (this.handleMusicCheck(false, true)) {
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

			out += this.getMusicWrapper().getMusicBotHandler().getListSize() > 0 ? "\n\n" : "";

			// Build String for each track in queue

			int parsed = -1;
			if (!this.argumentSection.equals("") && !this.argumentSection.equals("all")) {
				try {
					parsed = Integer.parseInt(this.argumentSection);
				} catch (Exception e) {
				}
			}
			final String ALL = "all";
			final LinkedList<AudioTrack> list = this.getMusicWrapper().getMusicBotHandler().getDeepListCopy();
			// Use parsed value if valid, else use all if contains all, else use default
			// value
			final int MAX_OUT = parsed > 0 ? parsed
					: (this.argumentSection.toLowerCase().equals(ALL) || this.commandSection.endsWith(ALL))
							? Integer.MAX_VALUE
							: 5;
			for (int i = 0; i < list.size() && i < MAX_OUT; i++) {
				if(i == 0){
					out += Markdown.toUnsafeMultilineBlockQuotes("");
				}
				final AudioTrack track = list.get(i);
				String trackUser = AudioEventHandler.getSubmittedByUserName(track, this.config.guildId);

				out += Markdown.toBold(i + 1 + ".") + " " + Markdown.toBold(track.getInfo().title) + " von "
						+ Markdown.toBold(track.getInfo().author)
						+ (trackUser != null ? " vorgeschlagen von " + Markdown.toBold(trackUser) : "") + "\n\n";
			}
			if (list.size() > MAX_OUT) {
				final int diff = list.size() - MAX_OUT;

				out += "Es gibt noch " + (diff == 1 ? Markdown.toBold("einen") + " weiteren Track!"
						: Markdown.toBold("" + diff) + " weitere Tracks!");
			}
			this.sendAnswer(out);
			this.deleteReceivedMessage();
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
	protected void onGlobalPSA() {
		if (this.hasPermission(SecurityLevel.DEV)) {
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("keine Nachricht angegeben!");
			} else {
				this.globalAnnounce(this.getArgumentSection(), false);
			}
		} else {
			this.noPermission();
		}

	}

	@Override
	protected void onClearMusicQueue() {
		if (this.handleMusicCheck(true, true)) {
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
			} else if (this.isPrivate()) {
				this.notInPrivate();
			} else {
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
			final String INTRO = "Ein neues Update ist verfügbar! :partying_face: Was ist neu?\n";
			if (this.getArgumentSection().equals("")) {
				this.sendAnswer("keine Nachricht angegeben!");
			} else if (!RuntimeVariables.isDebug()) {
				this.globalAnnounce(INTRO + Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()), true);
			} else {
				this.sendMessageToOwner(INTRO + Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()));
			}
		} else {
			this.noPermission();
		}
	}

	@Override
	protected void onWeather() {
		String city = this.getArgumentSection().equals("") ? this.getLocalHomeTown() : this.getArgumentSection();

		Optional<Message> message = this.sendAnswer("suche nach Wetterdaten, gib mir einen Moment...");

		String resp = Weather.buildMessage(this.getGlobalProxy().getWeatherService().getWeatherResponse(city));
		this.sendAnswer(resp);
		if(message.isPresent()){
			message.get().delete().block();
		}
	}

	@Override
	protected void onYesNo() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("du musst mir eine Frage stellen!");
			return;
		}

		final String[] posResp = { "ja.", "tatsache.", "das ist sehr wahrscheinlich.", "völlig korrekt.", "in der Tat.",
				"absolut.", "aber natürlich!", "auf jeden Fall!", "definitiv.", "daran besteht kein Zweifel.",
				"davon würde ich definitiv ausgehen.", "zweifellos!", "offensichtlich ja.", "yes.", "jawohl!", "klar!",
				"rein objektiv gesehen: Ja.", "jup.", "jep.", "jo.", "selbstverständlich!",
				"klingt nach einer hervorragenden Idee!", ":thumbsup:" };
		final String[] negResp = { "nein.", "überhaupt nicht.", "auf garkeinen Fall.", "völlig unmöglich.",
				"definitiv nicht.", "no!", "offensichtlich nicht.", "das ist nicht möglich.",
				"davon würde ich sowas von abraten!", "bloß nicht!", "rein objektiv gesehen: Nein.",
				"nein, wieso auch?", "eher nicht.", "ne.", "nope.", "nö", "nichts da!",
				"klingt wenn du mich fragst nach einer scheiß Idee.", ":thumbsdown:" };
		final Random rand = new Random();

		this.sendAnswer(
				rand.nextBoolean() ? posResp[rand.nextInt(posResp.length)] : negResp[rand.nextInt(negResp.length)]);
	}

	@Override
	protected void onWiki() {
		if (this.argumentSection.equals("")) {
			this.sendAnswer("du musst mir einen Begriff nennen!");
			return;
		}

		final Optional<Message> loadingMessage = this.sendAnswer("einen Moment bitte, ich versuche auf Wikipedia etwas geeignetes zu finden...");

		WikiPage page = Wikipedia.getWikiPage(this.argumentSection);

		if (page == null) {
			this.sendAnswer("konnte unter diesem Begriff nichts finden!");
		}
		else{
			if (page.CUSTOM_PROP_LANGUAGE == null || page.CUSTOM_PROP_LANGUAGE.equals("")) {
				throw new IllegalMagicException("CUSTOM_PROP_LANGUAGE must not be null or empty");
			}
	
			final String humanUrl = "https://" + page.CUSTOM_PROP_LANGUAGE + "." + Wikipedia.WIKI_NORMAL_BASE_URL
					+ HTTPRequests.urlEncode(page.title.replace(" ", "_"));
	
			this.getMessageChannel().createEmbed(spec -> {
				spec.setColor(Color.CYAN)
				.setTitle(page.title)
				.setUrl(humanUrl)
				.setDescription(page.extract);
	
				if (page.original != null) {
					spec.setImage(page.original.source);
				}
			}).block();
		}

		if(loadingMessage.isPresent()){
			loadingMessage.get().delete("Deleting info concerning async tasks").block();
		}
	}

	@Override
	protected void onFeedback() {
		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("ein leeres Feedback ist kein Feedback!");
			return;
		}
		this.sendMessageToOwner("Wir haben Feedback von " + Markdown.toBold(this.getMessage().getAuthorName())
				+ " erhalten! BenutzerId: "+ Markdown.toCodeBlock(this.getMessage().getUser().getId().asString()));
		this.sendMessageToOwner(Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()));
		QuickLogger.logfeedback("From: "+this.getMessage().getAuthorName()+" "+this.getMessage().getUser().getId().asString()+" - "+this.getArgumentSection());
		this.sendPrivateAnswer("Vielen Dank! Ich habe dein Feedback an " + this.getOwner().getUsername()
				+ " weitergeleitet! "
				+ "Er wird bei Rückfragen auf dich zukommen. Falls du zunächst keine Rückmeldung bekommst heißt das, dass dein Feedback ohne Rückfragen akzeptiert wurde :smile:");
		this.sendPrivateAnswer(Markdown.toSafeMultilineBlockQuotes(this.getArgumentSection()));
		this.deleteReceivedMessage();
		if(!this.isTextCommand()){
			this.sendAnswer("*siehe Privatchat*");
		}
	}

	@Override
	protected void onFastForwardMusic() {
		if (!this.handleMusicCheck(true, true)) {
			return;
		}

		if(this.getMusicWrapper().getMusicBotHandler().currentIsStream()){
			this.sendAnswer("es läuft ein Livestream, hier kannst du nicht springen!");
			return;
		}

		if (this.getArgumentSection().equals("")) {
			this.sendAnswer("mir ist nicht klar, wie viel ich skippen soll!");
			return;
		}

		final String possibleTimestamp = this.getArgumentSection().replaceAll("[^0-9]+", ":");
		if(!this.getArgumentSection().endsWith("%") && !this.getArgumentSection().startsWith("-") && possibleTimestamp.contains(":")){
			try{
				long ms = Time.revertMsToPretty(possibleTimestamp, ":");
				long pos = this.getMusicWrapper().getMusicBotHandler().setPosition(ms);
				this.sendAnswer("neue Position ist "+Markdown.toBold(TimePrint.msToPretty(pos)));
				this.deleteReceivedMessage();
			}
			catch(Exception e){
				this.sendAnswer("ungültiger Timestamp. Timestamps haben das Format mm:ss oder hh:mm:ss");
				return;
			}
		}
		else if(this.argumentSection.endsWith("%")){
			String percentage = this.argumentSection.substring(0, this.getArgumentSection().length()-1);
			try{
				int perc = Integer.parseInt(percentage);
				double dec = perc/100.0;
				long pos = this.getMusicWrapper().getMusicBotHandler().setPosition((long) (dec * this.getMusicWrapper().getMusicBotHandler().getCurrentAudioTrack().getDuration()));
				this.sendAnswer("springe auf "+Markdown.toBold(this.getArgumentSection())+", neue Position ist "+Markdown.toBold(TimePrint.msToPretty(pos)));
				this.deleteReceivedMessage();
			}
			catch(NumberFormatException e){
				this.sendAnswer("ungültige Prozentangabe!");
				return;
			}
		}
		else{
			try {
				int skipSeconds = Integer.parseInt(this.getArgumentSection());
				if(skipSeconds == 0){
					this.getMusicWrapper().getMusicBotHandler().setPosition(0l);
					this.sendAnswer("alles auf Anfang! :repeat:");
					this.deleteReceivedMessage();
					return;
				}
				long pos = this.getMusicWrapper().getMusicBotHandler().jump(skipSeconds * 1000);
				this.sendAnswer("springe "+Markdown.toBold(skipSeconds+" "+(Math.abs(skipSeconds) > 1 ? "Sekunden" : "Sekunde"))+"! Neue Position ist "+Markdown.toBold(TimePrint.msToPretty(pos)));
				this.deleteReceivedMessage();
			} catch (NumberFormatException e) {
				this.sendAnswer(
						"ungültige Eingabe. Gib in Sekunden an, wie viel ich skippen soll. Negative Zahlen gehen auch!");
			}
		}
	}

	@Override
	protected void onMusicRandom() {
		if (this.handleMusicCheck(true, true)) {
			if (this.getMusicWrapper().getMusicBotHandler().getListSize() <= 1) {
				this.sendAnswer(
						"es müssen mindestens zwei Tracks in der Warteschlange sein, sonst macht das ganze keinen Sinn!");
					this.deleteReceivedMessage();
			} else {
				this.getMusicWrapper().getMusicBotHandler().randomize();
				this.sendAnswer(
						"*ratter* *ratter* *schüttel* *schüttel*  Die Warteschlange wurde einmal kräftig durchgemischt!");
				this.deleteReceivedMessage();
			}
		}
	}

	@Override
	protected void onChicken() {
		if(this.isPrivate()){
			this.notInPrivate();
			return;
		}

		if (this.chicken == Integer.MAX_VALUE) {
			this.sendAnswer("Mehr geht nicht!");
			return;
		}

		final String appendString = ":chicken:";

		this.chicken++;

		if (this.chicken <= 200) {
			StringBuilder response = new StringBuilder(this.chicken * appendString.length());
			for (int i = 0; i < this.chicken; i++) {
				response.append(appendString);
			}
			this.sendAnswer(response.toString());
		} else {
			this.sendAnswer(this.chicken + " " + appendString);
		}
	}

	@Override
	protected void onNuggets() {
		if(this.isPrivate()){
			this.sendAnswer("Chicken Nuggets isst man am besten mit Freunden und nicht alleine im Privatchat!");
			return;
		}

		String response = "";

		switch (this.chicken) {
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
			case Integer.MAX_VALUE:
				response = "du hast das Maximum an Chicken Nuggets erreicht (" + this.chicken
				+ ")! Das ist ein Achievement Wert aber sowas gibt es bei mir nicht!";
				break;
			default:
				if (this.chicken >= 200) {
					response = this.chicken
							+ " Hühner wurden gewaltsam umgebra- Hier sind deine Chicken Nuggets, Mahlzeit!";
				} else if (this.chicken >= 50) {
					response = "hier ist deine McDonalds Tageslieferung an Chicken Nuggets (" + this.chicken
							+ " Portionen)";
				} else if (this.chicken >= 10) {
					response = "hier sind " + this.chicken + " Portionen Chicken Nuggets, genug für eine Großfamilie!";
				} else if (this.chicken >= 4) {
					response = "hier sind " + this.chicken + " Portionen Chicken Nuggets!";
				}
		}

		this.sendAnswer(response);
		this.chicken = 0;
	}

	@Override
	protected void onPommes() {
		this.sendAnswer("Pommes gibt es bei mir nicht!");
	}

	@Override
	protected void onVoiceStateEvent(VoiceStateUpdateEvent event) {
		if(event.isJoinEvent() || event.isMoveEvent()){
			final Guild eventGuild = event.getCurrent().getGuild().block();
			final VoiceChannel voiceChannel = event.getCurrent().getChannel().block();
			final Member joinedUser = event.getCurrent().getUser().flatMap(user -> user.asMember(eventGuild.getId())).block();
			final int connectedAmount = voiceChannel.getVoiceStates().collectList().map(stateList -> stateList.size()).block();
			
			// ignore if this bot joined or if there are two or more users
			if(connectedAmount >= 2 || joinedUser.getId().equals(this.getClient().getSelfId())){
				return;
			}

			// Notify users if voice channel is in subscriber hashmap
			if(this.config.voiceSubscriberMap.containsKey(voiceChannel.getId())){
				// ########## SPAM PROTECTION ##########
				Calendar notBefore = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				if(this.config.voiceSubscriberMap.get(voiceChannel.getId()).key != null && this.config.voiceSubscriberMap.get(voiceChannel.getId()).key.compareTo(notBefore) > 0){
					return;
				}
				notBefore.add(Calendar.MINUTE, 10); // Create new anti spam time stamp
				this.config.voiceSubscriberMap.get(voiceChannel.getId()).key = notBefore;
				// ########## NOTIFY SUBSCRIBED USERS ##########
				for(Snowflake userId : this.config.voiceSubscriberMap.get(voiceChannel.getId()).value){
					// Get user voice state for this perticular user
					VoiceState listUserVoiceState = null;
					try{
						listUserVoiceState= eventGuild.getMemberById(userId).flatMap(member -> member.getVoiceState()).block();
					}catch(Exception e){}

					// Ignore if user in list is already connected to voice in same guild
					if(listUserVoiceState != null && listUserVoiceState.getGuildId().equals(eventGuild.getId())){
						continue;
					}

					// Else notify user
					this.getClient().getUserById(userId)
					.flatMap(user -> user.getPrivateChannel())
					.flatMap(channel -> channel.createMessage(spec ->{
						spec.setContent(Markdown.toSafeMultilineBlockQuotes("In "+Markdown.toBold(this.getGuildSecureName())+" im "+Markdown.toBold(voiceChannel.getName())+" VoiceChannel ist etwas los. Komm und sag Hallo!\n\n"
							+"Schreib auf dem entsprechenden Server "+Markdown.toCodeBlock("MegUnfollow "+voiceChannel.getId().asString())+" um die Benachrichtigung auszuschalten!"));
					})).block();
				}
			}
		}
	}

	@Override
	protected void onSubscribeVoice() {
		if(this.isPrivate()){
			this.notInPrivate();
			return;
		}

		String channelIdentifier = this.argumentSection;
		if(channelIdentifier.equals("")){
			// Use connected voice channel, if author is connected
			if(this.isAuthorVoiceConnectedGuildScoped()){
				channelIdentifier = this.getAuthorVoiceChannel().getId().asString();
			}
			else{
				this.sendAnswer("du musst mir einen gültigen Namen oder eine ID nennen! Alternativ kannst du einem VoiceChannel beitreten und es erneut versuchen!");
				return;
			}
		}

		for(GuildChannel channel : this.getVoiceChannels()){
			// Find corresponding voice channel
			if(channel.getId().asString().equals(channelIdentifier) || channel.getName().equals(channelIdentifier)){
				final Snowflake userId = this.getMessage().getUser().getId();
				final String okayMessage = "Du hast "+channel.getName()+" abonniert! Schreib auf diesem Server "+Markdown.toCodeBlock("MegUnfollow "+channel.getId().asString())+", um diesen wieder zu deabonnieren!";
				// Found channel in HashMap
				if(this.config.voiceSubscriberMap.containsKey(channel.getId())){
					final HashSet<Snowflake> subscriberSet = this.config.voiceSubscriberMap.get(channel.getId()).value;
					if(subscriberSet.contains(userId)){
						this.sendPrivateAnswer("Du hast diesen Kanal schon abonniert! Schreib auf diesem Server "+Markdown.toCodeBlock("MegUnfollow "+channel.getId().asString())+", um diesen zu deabonnieren!");
					}
					else{
						subscriberSet.add(userId);
						this.getGlobalProxy().saveAllGuilds();
						this.sendPrivateAnswer(okayMessage);
					}
				}
				// Create new entry in HashMap, if channel not found in HashMap
				else{
					HashSet<Snowflake> set = new HashSet<>();
					set.add(userId);
					this.config.voiceSubscriberMap.put(channel.getId(), new Pair<>(null, set));
					this.getGlobalProxy().saveAllGuilds();
					this.sendPrivateAnswer(okayMessage);
				}
				this.deleteReceivedMessage();
				if(!this.isTextCommand()){
					this.sendAnswer("eine Nachricht wurde dir privat geschickt");
				}
				return;
			}
		}

		this.sendAnswer("konnte diesen Kanal nicht finden, oder du hast keinen Voice Kanal ausgewählt!");
	}

	@Override
	protected void onUnsubscribeVoice() {
		if(this.isPrivate()){
			this.notInPrivate();
			return;
		}

		String channelIdentifier = this.argumentSection;
		if(channelIdentifier.equals("")){
			// Use connected voice channel, if author is connected
			if(this.isAuthorVoiceConnectedGuildScoped()){
				channelIdentifier = this.getAuthorVoiceChannel().getId().asString();
			}
			else{
				this.sendAnswer("du musst mir einen gültigen Namen oder eine ID nennen! Alternativ kannst du einem VoiceChannel beitreten und die selbe Eingabe erneut versuchen!");
				return;
			}
		}

		for(GuildChannel channel : this.getVoiceChannels()){
			// Find corresponding voice channel
			if(channel.getId().asString().equals(channelIdentifier) || channel.getName().equals(channelIdentifier)){
				final Snowflake userId = this.getMessage().getUser().getId();
				final String errorMessage = "Du hast diesen Kanal nicht abonniert! Schreib auf diesem Server "+Markdown.toCodeBlock("MegFollow "+channel.getId().asString())+", um diesen zu abonnieren!";
				// Found channel in HashMap
				if(this.config.voiceSubscriberMap.containsKey(channel.getId())){
					final HashSet<Snowflake> subscriberSet = this.config.voiceSubscriberMap.get(channel.getId()).value;
					if(subscriberSet.remove(userId)){
						this.getGlobalProxy().saveAllGuilds();
						this.sendPrivateAnswer("Du hast "+channel.getName()+" deabonniert!");
					}
					else{
						this.sendPrivateAnswer(errorMessage);
					}
				this.deleteReceivedMessage();
				}
				// Channel not found in HashMap -> user was not subscribed!
				else{
					this.sendAnswer(errorMessage);
				}
				if(!this.isTextCommand()){
					this.sendAnswer("eine Nachricht wurde dir privat geschickt");
				}
				return;
			}
		}

		this.sendAnswer("konnte diesen Kanal nicht finden, oder du hast keinen Voice Kanal ausgewählt!");
	}

	@Override
	protected void onGetVoiceSubscriptions() {
		HashSet<Pair<Guild, VoiceChannel>> set = this.getGlobalProxy().getSubscribedGuildChannelPairs(this.getMessage().getUser().getId());
		if(!this.isTextCommand()){
			this.sendAnswer("eine Antwort wurde dir privat gesendet!");
		}
		if(set.isEmpty()){
			this.sendPrivateAnswer("Du hast noch keinen VoiceChannel abonniert. Schreib auf einem entsprechenden Server "+Markdown.toCodeBlock("MegFollow ChannelName/Snowflake")+", um Benachrichtigungen für einen VoiceChannel zu erhalten!");
			return;
		}

		String response = "Du hast "+set.size()+" VoiceChannel"+" abonniert:\n";
		response += Markdown.toUnsafeMultilineBlockQuotes("");
		for(Pair<Guild, VoiceChannel> pair : set){
			response += "Server: "+Markdown.toBold(pair.key.getName())+" - VoiceChannel: "+Markdown.toBold(pair.value.getName())+"\n";
		}

		response += "\n\nSchreib auf dem entsprechenden Server(!) "+Markdown.toCodeBlock("MegUnfollow ChannelName/Snowflake")+" um einen Kanal zu deabonnieren!";

		this.sendPrivateAnswer(response);
		this.deleteReceivedMessage();
	}

	@Override
	protected void onPrivate() {
		if(!this.hasPermission(SecurityLevel.DEV)){
			this.noPermission();
			return;
		}

		if(this.getArgumentSection().equals("")){
			this.sendAnswer("Empfänger und Nachricht sind nötig!");
			return;
		}

		String id = this.getArgumentSection().split(" ")[0];
		if(this.getArgumentSection().length() <= id.length()+1){
			this.sendAnswer("Nachricht fehlt!");
			return;
		}

		String content = this.getArgumentSection().substring(id.length()+1);
		try{
			Snowflake userSnowflake = Snowflake.of(id);
			this.getClient().getUserById(userSnowflake)
			.flatMap(user -> user.getPrivateChannel())
			.flatMap(channel -> channel.createMessage("Nachricht vom Botowner:\n\n"
			+ Markdown.toUnsafeMultilineBlockQuotes(content))).block();

			this.sendPrivateAnswer("Deine Nachricht ist bei "+Markdown.toBold(this.getClient().getUserById(userSnowflake).map(user -> user.getUsername()).block())+" angekommen!");
		}
		catch(ClientException e){
			switch(e.getStatus().code()){
				case 404:
					this.sendPrivateAnswer("konnte Benutzer nicht finden!");
					break;
				case 401:
				case 403:
					this.sendAnswer("Benutzer hat mich geblockt :(");
					break;
				default:
					this.sendAnswer("konnte Privatnachricht nicht versenden (Fehler "+e.getStatus().code()+")!");
			}
		}
		catch(NumberFormatException e){
			this.sendPrivateAnswer("fehlerhafte BenutzerId!");
		}
		this.deleteReceivedMessage();
	}

	@Override
	protected void onLoadMainConfig() {
		if(!this.hasPermission(SecurityLevel.DEV)){
			this.noPermission();
			return;
		}

		boolean success = StartUp.loadMainConfig();

		this.sendPrivateAnswer(success ? "MainConfig wurde erfolgreich aktualisiert" : "Beim Aktualisieren der MainConfig ist ein Fehler aufgetreten");
		this.deleteReceivedMessage();
	}

	@Override
	protected void onLog() {
		if(!this.hasPermission(SecurityLevel.DEV)){
			this.noPermission();
			return;
		}

		try{
			File logFile = new File("./nohup.out");

			if(!logFile.exists()){
				this.sendPrivateAnswer("Log Datei konnte nicht gefunden werden!");
			}
			else if(!logFile.canRead()){
				this.sendPrivateAnswer("konnte diese Datei finden, kann diese aber nicht lesen!");
			}
			else{
				try(InputStream fileStream = new FileInputStream(logFile)){
					this.getMessageAuthorPrivateChannel()
					.createMessage(spec -> 
								spec.addFile("nohup.txt", fileStream)
								.setContent("Hier deine Logdatei!")
								)
								.block();
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
			this.sendPrivateAnswer("etwas ist schief gelaufen!");
		}
		finally{
			this.deleteReceivedMessage();
		}
	}

	@Override
	protected void onReload() {
		if(!this.hasPermission(SecurityLevel.DEV)){
			this.noPermission();
			return;
		}

		StartUp.loadMainConfig();
		this.getGlobalProxy().reloadAllGuilds();

		this.sendPrivateAnswer("Erfolgreich neu geladen!");

		this.deleteReceivedMessage();

	}

	@Override
	protected void onConfig() {
		Guild targetGuild = this.getGuild();
		Snowflake authorId = this.getMessageAuthor().getId();
		final String delimiter = ";";
		LinkedList<String> args = new LinkedList<>(Arrays.asList(this.getArgumentSection().split(delimiter)));
		final Permission requiredPermission = Permission.ADMINISTRATOR;

		// ########## Determine target guild & check user permissions ##########

		List<Guild> parsedGuilds = this.parseGuild(args.getFirst());

		// Filter out guilds, where user is not member or not admin
		parsedGuilds.removeIf(guild ->{
			GuildHandler handler = this.getGlobalProxy().getGuildHandler(guild.getId());

			// Got no handler for id -> something is broken -> ignore
			if(handler == null){
				return true;
			} 
			return !handler.hasPermission(authorId, requiredPermission);
		});

		// First argument not a valid guild or user not admin in parsed guilds
		if(parsedGuilds.size() == 0){
			// Quit if no further target guild available or user has no permission on given target guild
			if(targetGuild == null /* aka private chat*/ || !this.getHandler().hasPermission(authorId, requiredPermission)){
				this.sendAnswer("du musst als erstes Argument den betreffenden Server angeben, oder diesen Befehl auf einem Server ausführen!\nVielleicht existiert der genannte Server aber auch nicht, oder du bist kein Serveradmin!");
				return;
			}
			else{
				if(args.size() == 1 && args.getFirst().equals("")){
					args.clear();
				}
				args.addFirst(targetGuild.getId().asString()); // User did not provide a server in args but used command in guild and is guild adm
			}
		}
		// Else use first arg as target guild - Parsed guilds have been filterd by permission
		else{
			targetGuild = parsedGuilds.get(0); // User provided valid guild in args
		}

		// ########## Build answer ##########
		String answer = "Botkonfiguration für den Server " +Markdown.toBold(targetGuild.getName())+" mit der ID "+Markdown.toBold(targetGuild.getId().asString())+"\n\n"+Markdown.toSafeMultilineBlockQuotes("");
		String configArg = "";
		ResponseType target = this.getResponseType(targetGuild.getId());
		switch(args.size()){
			// Current config
			case 1:
				answer += "Es gelten folgende Einstellungen:\n"
				       + "Hier werden Infos zur Musiksession geposted: " + Markdown.toBold(target.getMusicWrapper().getMusicChannelId().isPresent() ? target.getChannelById(target.getMusicWrapper().getMusicChannelId().get()).getName() : "Kanal wird automatisch ermittelt")+"\nSchreib zum Konfigurieren "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";musik;channelId oder Name")+"\n\n"
					   + "Rolle mit mehr Berechtigungen: "+ Markdown.toBold(target.getConfig().getSecurityProvider().specialRoleId != null ? target.getRoleById(target.getConfig().getSecurityProvider().specialRoleId).getName() : "Keiner außer Serveradmins hat Extraberechtigungen")+"\nSchreib zum Konfigurieren "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";spezial;rollenId oder Name")+"\n\n"
					   + "Systembenachrichtigungen zum Bot (keine Update Infos): "+Markdown.toBold(target.getConfig().psaNote ? "Ja, dein Server bleibt immer informiert!" : "Nein, dein Server lebt hinterm Mond!")+"\nSchreib zum Konfigurieren "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";psa")+"\n\n"
					   + "Benachrichtigungen bei neuen Updates: "+ Markdown.toBold(target.getConfig().updateNote ? "Ja, dein Server wird bei neuen Updates informiert!" : "Nein, deine Servermitglieder erfahren nicht, wenn es neue Funktionen zum Ausprobieren gibt!")+"\nSchreib zum Konfigurieren "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";update")+"\n\n"
					   + "Heimatstadt: "+Markdown.toBold(target.getConfig().homeTown != null && !target.getConfig().homeTown.equals("") ? target.getConfig().homeTown : "Nichts angegeben, verwende Standard: "+RuntimeVariables.getInstance().getHometown())+"\nSchreib zum Konfigurieren "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";stadt;Deine Stadt")+"\n\n"
					   + "Befehlsprefix (NUR auf deinem Server): "+Markdown.toBold(!StringUtils.isNullOrWhiteSpace(target.getConfig().customPrefix) ? target.getConfig().customPrefix : "Nichts angegeben.")+"\nSchreib zum Konfigurieren "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";prefix;neuer Prefix")+"\n\n";
				this.sendPrivateAnswer(answer);
				return;
			// Apply config (case 3 if config requires args)
			case 3:
				configArg = args.get(2);
			case 2:
				switch(args.get(1).toLowerCase()){
					case "music":
					case "musik":
						if(configArg.equals("")){
							answer += "Infos zu Musik Sessions werden automatisch in passende Kanäle versendet!";
							target.getMusicWrapper().setMusicChannelId(null);
							break;
						}
						List<MessageChannel> channels = target.parseMsgChannel(configArg);
						if(channels.size() == 0){
							this.sendPrivateAnswer("Diesen Textkanal gibt es nicht!");
							this.deleteReceivedMessage();
							return;
						}
						else{
							answer += "Informationen zu Musiksessions werden jetzt im Kanal "+Markdown.toBold(channels.get(0).getRestChannel().getData().map(data -> data.name().get()).block())+ " angezeigt!";
							target.getMusicWrapper().setMusicChannelId(channels.get(0).getId());
						}
						break;
					case "mod":
					case "moderator":
					case "spezial":
					case "special":
						if(configArg.equals("")){
							answer += "Es gibt keine Rolle mit speziellen Berechtigungen mehr!";
							target.getConfig().getSecurityProvider().specialRoleId = null;
							break;
						}
						List<Role> roles = target.parseRole(configArg);
						if(roles.size() == 0){
							this.sendPrivateAnswer("Diese Rolle gibt es nicht!");
							this.deleteReceivedMessage();
							return;
						}
						else{
							answer += "Die Rolle "+Markdown.toBold(roles.get(0).getName()+" hat jetzt zusätzliche Zugriffsrechte für den Bot (Musikbot Lautstärke ändern)!");
							target.getConfig().getSecurityProvider().specialRoleId = roles.get(0).getId();
						}
					break;
					case "update":
						if(configArg.toLowerCase().equals("true")){
							target.getConfig().updateNote = true;
						}
						else if(configArg.toLowerCase().equals("false")){
							target.getConfig().updateNote = false;
						}
						else{
							target.getConfig().updateNote = !target.getConfig().updateNote;
						}
						answer += !target.getConfig().updateNote ? "Der Server erhält keine Infos bzgl. Updates mehr (betrifft nicht Systemnachrichten)" : "Der Server erhält Infos zu frischen Updates, falls es auf dem Server einen Systemkanal gibt (betrifft nicht Systemnachrichten)";
						break;
					case "psa":
					case "info":
						if(configArg.toLowerCase().equals("true")){
							target.getConfig().psaNote = true;
						}
						else if(configArg.toLowerCase().equals("false")){
							target.getConfig().psaNote = false;
						}
						else{
							target.getConfig().psaNote = !target.getConfig().psaNote;
						}
						answer += !target.getConfig().psaNote ? "Der Server erhält keine Systembenachrichtigungen mehr und du verpasst wichtige Meldungen (betrifft nicht Update Meldungen)" : "Der Server erhält Systembenachrichtigungen und du bleibst bei allen Angelegenheiten stets informiert, falls es auf dem Server einen Systemkanal gibt (betrifft nicht Update Meldungen)";
						break;
					case "home":
					case "town":
					case "city":
					case "stadt":
						target.getConfig().homeTown = configArg;
						answer += configArg.equals("") ? "Verwende Standard "+Markdown.toBold(RuntimeVariables.getInstance().getHometown()) : "Die Heimatstadt des Servers ist nun "+Markdown.toBold(configArg);
						break;
					case "prefix":
					case "command":
					case "cmd":
					case "befehl":
						target.getConfig().customPrefix = configArg;
						answer += StringUtils.isNullOrWhiteSpace(configArg) ? "Prefix gelöscht. Alle Befehle fangen mit "+Markdown.toBold(RuntimeVariables.getInstance().getCommandPrefix())+ " an!" : "Befehle fangen auf deinem Server jetzt mit "+Markdown.toBold(configArg)+" an. Der alte Prefix ("+Markdown.toBold(RuntimeVariables.getInstance().getCommandPrefix())+") funktioniert aber weiterhin!";
						break;
					default:
					this.sendPrivateAnswer("Ungültiger Parameter. Argumente müssen mit Semikolon getrennt werden. Versuchs mit "+Markdown.toCodeBlock("MegConfig "+targetGuild.getId().asString()+";option;argumente"));
					this.deleteReceivedMessage();
					return;
				}
				break;
			default:
				this.sendPrivateAnswer("Ungültige Anzahl an Argumenten. Das Befehlsschema lautet: MegConfig ServerName/Id;option;Argument(opt.)");
				this.deleteReceivedMessage();
				return;
		}

		this.sendPrivateAnswer(answer);
		this.deleteReceivedMessage();
		try{
			this.getGlobalProxy().saveAllGuilds();
			this.sendPrivateAnswer("Einstellungen erfolgreich gespeichert :white_check_mark:");
		}
		catch(Exception e){
			this.sendPrivateAnswer("Beim Speichern ist ein Problem aufgetreten. Bitte prüfe die aktuellen Einstellungen und versuche es ggf. nochmal! :x:");
		}
	}

	@Override
	protected void onWrongInteraction() {
		this.sendAnswer("dieser Befehl existiert noch nicht! Wenn du diese Nachricht siehst heißt das, dass mein Besitzer "+Markdown.toBold(this.getOwner().getUsername())+" Mist gebaut hat :smile:");
		QuickLogger.logWarn("Received an unimplemented command '"+this.getMessage().getContent()+"'");
	}

	@Override
	protected void onLoopMusic() {
		if(this.handleMusicCheck(true, true)){
			boolean isLoop = this.getMusicWrapper().getMusicBotHandler().toggleLoop();

			this.sendAnswer(isLoop ? "Loop "+Markdown.toBold("aktiviert")+" :repeat_one:" : "Loop "+Markdown.toBold("deaktiviert"));
		}
	}

	@Override
	protected void onHelp() {
		String interactionAns = "";
		switch(this.getArgumentSection()){
			case "musik":
				this.onMusicHelp();
				interactionAns = "eine Liste an Musikbefehlen wurde dir zugesendet!";
				break;
			case "umfrage":
				this.onSurveyHelp();
				interactionAns = "eine Anleitung für Umfragen wurde dir privat zugesendet!";
				break;
			case "config":
				this.onConfig();
				interactionAns = "du hast privat eine Liste an Konfigurationen erhalten, sofern du Rechte dazu hast";
				break;
			case "":
				interactionAns = "eine Befehlsliste wurde dir privat zugesendet!";
				break;
			default:
				this.sendAnswer("dafür gibt es keine Befehlsliste!");
				return;
		}

		if(!this.isPrivate()){
			this.sendAnswer(interactionAns);
		}
	}

	@Override
	protected void onMexico() {
		try{
			this.sendAnswer(":flag_mx:");
	
			this.onReceiveMusicRequest(ScheduleType.INTRUSIVE, "The Mexican Hat Dance");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected void onTranslate(String query) {
		if(StringUtils.isNullOrWhiteSpace(query)){
			this.sendAnswer("Text fehlt! Was soll ich übersetzen?");
			return;
		}
		
		Optional<Message> msg = this.sendAnswer("wird übersetzt... :mag:");

		Optional<TranslatorResponse[]> resps = this.getGlobalProxy().getTranslatorService().translate(new String[] {query}, "de");
		if(resps.isEmpty() || resps.get().length == 0 || resps.get()[0].translations == null || resps.get()[0].translations.length == 0){
			String errMsg = this.getMessageAuthor().getMention()+", konnte "+Markdown.toBold(query)+" nicht übersetzen!";
			if(msg.isPresent()){
				msg.get().edit(spec -> spec.setContent(errMsg)).block();
			}
			else{
				this.sendAnswer(errMsg);
			}
			return;
		}

		

		TranslatorResponse firstResp = resps.get()[0];
		Optional<Language> detectedLang = firstResp.detectedLanguage != null ? this.getGlobalProxy().getTranslatorService().getLanguage(firstResp.detectedLanguage.language, "de") : Optional.empty();
		String successMsg = Markdown.toSafeMultilineBlockQuotes(this.getMessageAuthor().getMention()+"\n"+(detectedLang.isPresent() ? "Erkannte Sprache: "+Markdown.toBold(detectedLang.get().name)+"\n" : "")
		+ "\nÜbersetzung: "+Markdown.toBold(firstResp.translations[0].text));

		if(msg.isPresent()){
			msg.get().edit(spec -> spec.setContent(successMsg)).block();
		}
		else{
			this.sendAnswer(successMsg);
		}
	}

	@Override
	protected void onPrevMusic() {
		if(this.handleMusicCheck(true, true)){
			Optional<AudioTrack> prevTrack = this.config.musicWrapper.getMusicBotHandler().playPrevious();
			if(prevTrack.isPresent()){
				this.sendAnswer("spiele vorherigen Song "+Markdown.toBold(prevTrack.get().getInfo().title) + " von "+Markdown.toBold(prevTrack.get().getInfo().author));
			}
			else{
				this.sendAnswer("kein vorheriger Song vorhanden!");
			}
		}
	}
}

