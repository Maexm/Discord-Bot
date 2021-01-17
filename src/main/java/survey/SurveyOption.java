package survey;

import java.util.ArrayList;

import discord4j.core.object.entity.User;
import util.Markdown;

public class SurveyOption {
	public final String option;
	private int votes;
	private ArrayList<User> voters = new ArrayList<User>();
	
	public SurveyOption(final String option) {
		this.option = option;
		this.votes = 0;
	}
	
	public String toString() {
		if(this.votes == 1) {
			return this.option+": "+Markdown.toBold("1 Stimme");
		}
		else {
			return this.option+": "+Markdown.toBold(this.votes+" Stimmen");
		}
		
	}
	/**
	 * Two SurveyOptions are considered to be equal, if their option texts are equal.
	 */
	@Override
	public boolean equals(Object obj) {
		SurveyOption compare = (SurveyOption) obj;
		return compare.option.equals(this.option);
	}
	
	public boolean hasVoted(User user) {
		return this.voters.contains(user);
	}
	
	public int getVotes() {
		return this.votes;
	}
	
	public void addVote(User user) {
		if(!this.hasVoted(user)) {
			this.voters.add(user);
		}
		this.votes++;
	}
	
	public void removeVote(User user) {
		this.voters.remove(user);
		if(this.votes != 0) {
			this.votes--;
		}
	}
	
	

}
