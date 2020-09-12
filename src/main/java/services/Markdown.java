package services;

/**
 * Provides a hand full of functions for markdown
 * @author MaximOleschenko
 *
 */
public class Markdown {
	
	public final static String toItalics(String message) {
		return "*"+message+"*";
	}
	
	public final static String toUnderlinedItalics(String message) {
		return Markdown.toUnderlined(Markdown.toItalics(message));
	}
	
	public final static String toBold(String message) {
		return "**"+message+"**";
	}
	
	public final static String toUnderlinedBold(String message) {
		return Markdown.toUnderlined(Markdown.toBold(message));
	}
	
	public final static String toBoldItalics(String message) {
		return "***"+message+"***";
	}
	
	public final static String toUnderlinedBoldItalics(String message) {
		return Markdown.toUnderlined(Markdown.toBoldItalics(message));
	}
	
	public final static String toUnderlined(String message) {
			return "__"+message+"__";
	}
	
	public final static String toStriketrough(String message) {
		return "~~"+message+"~~";
	}
	
	public final static String toCodeBlock(String message) {
		return "`"+message+"`";
	}
	
	public final static String toMultilineCodeBlock(String message) {
		return "```"+message+"```";
	}
	
	public final static String toBlockQuotes(String message) {
		return "> "+message;
	}
	
	public final static String toMultilineBlockQuotes(String message) {
		return ">>> "+message;
	}
	
	public final static String toSpoiler(String message) {
		return "||"+message+"||";
	}
}
