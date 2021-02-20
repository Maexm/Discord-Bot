package survey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import exceptions.SurveyCreateIllegalDurationException;
import util.Markdown;
import util.TimePrint;
import start.RuntimeVariables;

public class Survey {

	public final String key;
	private final int id;
	public final String description;
	/**
	 * Do NOT use a HashMap for options, as the order of the options is very
	 * important!
	 */
	public final ArrayList<SurveyOption> options;
	private final Timer timer;
	private final TimerTask endTask;
	private final Calendar endTime;
	public final User createdBy;
	private final Message publicMessage;
	private final ArrayList<Survey> surveyList;
	private final ArrayList<User> participants;
	public final boolean isMulti;
	public VoteEndReason endReason = VoteEndReason.TIMED_OUT;
	public final Snowflake guildId;

	public Survey(final String description, final String[] options, final int duration, final MessageChannel channel,
			final User createdBy, final ArrayList<Survey> surveyList, final boolean isMulti, final Snowflake guildId)
			throws SurveyCreateIllegalDurationException {

		// Check duration
		if (duration <= 0) {
			throw new SurveyCreateIllegalDurationException(duration + " is not a valid duration");
		}

		// Check options
		ArrayList<SurveyOption> optionsList = Survey.fromArray(options);

		this.surveyList = surveyList;
		this.id = this.createIDNumber();
		this.key = Integer.toString(id);
		this.description = description;
		this.options = optionsList;
		this.timer = new Timer("MegSurvey_" + key, true);
		this.createdBy = createdBy;
		this.isMulti = isMulti;
		this.participants = new ArrayList<User>();
		this.guildId = guildId;

		// Calculate and set end time and also save current time
		Calendar startTime = Calendar.getInstance(RuntimeVariables.getInstance().getTimezone());

		Calendar calendarTime = Calendar.getInstance(RuntimeVariables.getInstance().getTimezone());
		calendarTime.add(Calendar.MINUTE, duration);
		this.endTime = calendarTime;

		// Send message

		Message publicMessage = channel.createMessage(this.createMessageText()).block();
		this.publicMessage = publicMessage;

		Survey timerParent = this;
		this.endTask = new TimerTask() {

			@Override
			public void run() {

				System.out.println("Survey '" + key + "' has finished!");
				try{
				final String results = timerParent.createOptionsText();

				String multiChoice = "";
				if (isMulti) {
					multiChoice = "Mehrfache Antworten waren erlaubt!";
				} else {
					multiChoice = "Mehrfache Antworten waren NICHT erlaubt!";
				}

				String participantsText = "";
				for (User user : participants) {
					participantsText += user.getMention() + " ";
				}

				String reason = "";
				switch (endReason) {
					case TIMED_OUT:
						reason = " ";
						break;
					case BROKEN:
						reason = " aufgrund eines Fehlers ";
						break;
					case CREATOR_STOPPED:
						reason = " vorzeitig vom Ersteller ";
						break;
					case LOGOUT:
						reason = " beim Herunterfahren vorzeitig ";
						break;
				}

				// Write survey-completed message
				channel.createMessage(">>> Die Umfrage " + Markdown.toBold(description) + " ("
						+ Markdown.toBold("NR. " + key) + ") wurde"+reason+"beendet!\n" + "Das Resultat:\n" + results + "\n"
						+ multiChoice + "\n" + "Die Umfrage wurde von " + createdBy.getMention()
						+ " initiiert und begann am " + Markdown.toBold(TimePrint.DD_MMMM_YYYY_HH_MM_SS(startTime))
						+ ".\n\n" + "Es haben teilgenommen: " + participantsText + "\n").block();

				// Delete original survey message
				publicMessage.delete().block();

				surveyList.remove(timerParent);
				}
				catch(Exception e){
					this.cancel();
					timer.purge();
					timer.cancel();
				}
			}

		};
		this.surveyList.add(this);
		this.timer.schedule(endTask, this.endTime.getTime());
	}

	public void stop(VoteEndReason reason) {
		this.endReason = reason;
		this.endTask.run();
	}

	public Calendar getEndTime() {
		return (Calendar) this.endTime.clone();
	}

	public boolean equals(Survey obj) {
		return obj.key.equals(this.key);
	}

	public static ArrayList<SurveyOption> fromArray(String[] options) {
		ArrayList<SurveyOption> ret = new ArrayList<SurveyOption>();
		for (String option : options) {
			SurveyOption optionObject = new SurveyOption(option);
			if (!ret.contains(optionObject) && !(option.equals("") || option.equals(" "))) {// Skip duplicates and empty
				ret.add(optionObject);
			}
		}
		return ret;
	}

	/**
	 * Finds a free id number for a survey
	 * 
	 * @return Next free ID number
	 */
	private int createIDNumber() {
		int count = 1;

		for (int i = 0; i < this.surveyList.size(); i++) {
			Survey survey = this.surveyList.get(i);
			if (survey.id == count) {
				// id already used -> incr count and restart for loop
				count++;
				i = 0;
			}
		}
		return count;
	}

	/**
	 * Returns whether or not the user has participated in this survey.
	 * 
	 * @param user User to be searched for
	 * @return USer has participated (true), otherwise false
	 */
	public boolean userHasParticipated(User user) {
		return this.participants.contains(user);
	}

	/**
	 * Returns an ArrayList of all options, the given user has voted for.
	 * 
	 * @param user The user to be searched for
	 * @return The ArrayList with corresponding SurveyOptions (returns empty list if
	 *         user has not participated)
	 */
	public ArrayList<SurveyOption> getUserVotes(User user) {
		ArrayList<SurveyOption> ret = new ArrayList<SurveyOption>();
		if (!this.userHasParticipated(user)) {
			return ret;
		}
		for (SurveyOption option : this.options) {
			if (option.hasVoted(user)) {
				ret.add(option);
			}
		}
		return ret;
	}

	/**
	 * Returns whether or not an option exists in this survey, based on the option
	 * TEXT
	 * 
	 * @param option The string content of the option
	 * @return True if this options exists, false otherwise
	 */
	public boolean optionExists(String option) {
		SurveyOption temp = new SurveyOption(option);
		return this.options.contains(temp);
	}

	/**
	 * Returns the corresponding SurveyOption object for a given option text
	 * 
	 * @param optionText The text of the option
	 * @return The corresponding SurveyOption instance, null if such option does not
	 *         exist.
	 */
	private SurveyOption getOption(String optionText) {
		for (SurveyOption option : this.options) {
			if (option.option.equals(optionText)) {
				return option;
			}
		}
		return null;
	}

	/**
	 * Parses a text into a number and returns the option on position number-1.
	 * 
	 * @param text
	 * @return The options text, used to internally identify the option
	 * @throws SurveyOptionNotFoundException If text does not represent a text
	 */
	public String getOptionTextFromIndex(String text) {
		int pos = -1;
		try {
			pos = Integer.parseInt(text);
			pos--;// Decr. by 1 since user will probably type in '1' for first option instead of
					// '0'
		} catch (Exception e) {
			// Could not parse text to number
		}
		// pos not in range
		if (pos < 0 || pos >= this.options.size()) {
			return "";// "" is not a possible option
		} else {
			// This is what we want :)
			return this.options.get(pos).option;
		}
	}

	/**
	 * Decides what happens after a user has voted.
	 * 
	 * @param user   The user who voted
	 * @param option His option
	 * @return Survey vote status strings (ADDED, DELETED, REJECTED, CHANGED)
	 */
	public VoteChange receiveVote(User user, String option) {
		if (!this.optionExists(option)) {
			throw new IllegalArgumentException("'" + option + "' does not exist!");
		}

		VoteChange ret;

		SurveyOption optionObject = this.getOption(option);
		// Add vote, if user has not participated yet
		if (!this.userHasParticipated(user)) {
			optionObject.addVote(user);
			this.participants.add(user);
			ret = VoteChange.ADDED;
		}
		// Delete vote, if user has voted for this before
		else if (optionObject.hasVoted(user)) {
			optionObject.removeVote(user);
			// Remove user from participants list, if this is not multi or if he has not
			// voted for anything else
			if (!this.isMulti || (isMulti && !this.hasParticipatedSomewhere(user))) {
				this.participants.remove(user);
				// User does not participate anymore
			}
			ret = VoteChange.DELETED;
		}
		// User has voted for something else (but has participated before!)
		else {
			// Remove the option the user has voted for before
			if (!this.isMulti) {
				this.getUserVotes(user).get(0).removeVote(user);
			}
			// Add vote to option
			optionObject.addVote(user);
			if (!this.isMulti) {
				ret = VoteChange.CHANGED;
			} else {
				ret = VoteChange.ADDED;
			}
		}
		// Update message
		if (ret.equals(VoteChange.ADDED) || ret.equals(VoteChange.DELETED) || ret.equals(VoteChange.CHANGED)) {
			this.updateMessage();
		}
		return ret;
	}

	private String getMultiTextNew() {
		if (this.isMulti) {
			return "Mehrere Antwortmöglichkeiten sind möglich!";
		} else {
			return "Nur eine Antwortmöglichkeit ist möglich!";
		}
	}

	/**
	 * Returns whether or not the user has voted somewhere
	 * 
	 * @param user
	 * @return
	 */
	private boolean hasParticipatedSomewhere(User user) {
		for (SurveyOption option : this.options) {
			if (option.hasVoted(user)) {
				return true;
			}
		}
		return false;
	}

	public int getTotalVotes() {
		int ret = 0;
		for (SurveyOption option : this.options) {
			ret += option.getVotes();
		}
		return ret;
	}

	private String createOptionsText() {
		final StringBuilder optionsText = new StringBuilder("");
		for (int i = 0; i < this.options.size(); i++) {
			SurveyOption option = this.options.get(i);
			int percentage = 0;
			if (this.getTotalVotes() != 0) {
				percentage = (int) 100. * option.getVotes() / this.getTotalVotes();
			}
			optionsText.append((i + 1) + ". " + option + " (" + percentage + "%)\n");
		}
		return optionsText.toString();
	}

	/**
	 * Creates the content for a survey message.
	 * 
	 * @return
	 */
	private String createMessageText() {
		final String optionsText = this.createOptionsText();

		return ">>> " + this.createdBy.getMention() + " hat eine Umfrage gestartet:\n"
				+ Markdown.toBold(this.description) + "\n" + optionsText + "\n" + "Die Umfrage endet am: "
				+ Markdown.toBold(TimePrint.DD_MMMM_YYYY_HH_MM_SS(this.endTime)) + "\n" + this.getMultiTextNew() + "\n"
				+ "Schreib mir (am besten privat):\n"
				+ Markdown.toBold("MegUmfrage " + Markdown.toUnderlined(this.key) + "-OPTION_NUMMER") + "\n"
				+ "Achte auf korrekte Syntax!";
	}

	/**
	 * Removes every vote the user has ever voted for
	 * 
	 * @param user The user whose votes needs to be removed
	 */
	public void removeAllVotesForUser(User user) {
		for (SurveyOption option : this.getUserVotes(user)) {
			option.removeVote(user);
		}
		this.participants.remove(user);
		this.updateMessage();
	}

	private void updateMessage() {
		this.publicMessage.edit(spec -> {
			spec.setContent(this.createMessageText());
		}).block();
	}

	public String getIDPrint() {
		return "NR. " + this.key;
	}
}

/*
 * 
 * MÜLLHAUFEN: for(SurveyOption option: options) { if(option.option.length() >=
 * 1) { try { int pos = Integer.parseInt(option.option); pos--; //A possible
 * reference to an option index if(pos >= 0 && pos < options.size()) { throw new
 * SurveyCreateIllegalOptionException("'"+option.
 * option+"' is not a legal option, since it refers to an option index!"); } //
 * This option is fine :) } catch(NumberFormatException e) { // Not an option
 * index, all fine :) } } }
 * 
 */
