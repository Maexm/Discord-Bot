package japanese;

import com.google.gson.Gson;
import util.HTTPRequests;
import util.Markdown;

public class Jisho {
	
	public final static String jishoURLLookup = "https://jisho.org/api/v1/search/words?keyword=";
	public final static String jishoURLStandard = "https://jisho.org/search/";

	/**
	 * Lookup a keyword through the jisho API.
	 * Jisho can be used for English and Japanese (Japanese or Roman letters) expressions.
	 * @param keyword
	 * @return The response body of the HTTP request (https://jisho.org/api/v1/search/words?keyword=KEYWORD) as a JishoResponse instance.
	 */
	public static JishoResponse lookUpKeyWord(String keyword) {
		keyword = HTTPRequests.urlEncode(keyword);

		String url = Jisho.jishoURLLookup+keyword;
		
			String response = HTTPRequests.getSimple(url);
			if(response == null) {
				return null;
			}
			
			Gson gson = new Gson();
			JishoResponse jishoResponse = gson.fromJson(response.toString(), JishoResponse.class);
		return jishoResponse;
	}
	/**
	 * Build a message out of a Jisho response that will be sent to the end user.
	 * @param resp JishoResponse Body
	 * @param searchKey The searchKey originally used for this request
	 * @param maxResults Max amount of results (resp.data) that will be printed
	 * @param maxSensesResults Max amount of senses (resp.data[i].senses) that will be printed
	 * @return The message, ready to be used as a discord message
	 */
	public static String buildMessage(JishoResponse resp, String searchKey, int maxResults, int maxSensesResults) {
		if(resp == null || searchKey == null) {
			throw new NullPointerException();
		}
		
		if(resp.data.length == 0) {
			return "Auf jisho.org wurden keine Suchergebnisse für "+Markdown.toBold(searchKey)+" gefunden!";
		}
		
		String ret = "Suchergebnisse für "+Markdown.toBold(searchKey)+":\n\n";
		
		if(!searchKey.startsWith("\"") || !searchKey.startsWith(Character.toString(searchKey.toLowerCase().charAt(0)))) {
			ret += "Nicht das, was du erwartet hast? Versuchs mit Anführungszeichen oder Kleinschreibung!\n\n";
		}
		
		for(int i = 0; i < resp.data.length && i < maxResults; i++) {
			Results data = resp.data[i];
			// PRINT WORD AND READING
				// data.slug BROKEN
			if(data.slug.startsWith("5186") && data.slug.length() == 24) {
				// Use japanese[0].word instead of slug

				// Uff
				if(data.japanese[0].reading == null && data.japanese[0].word != null){
					ret += Markdown.toBold(data.japanese[0].word);
				}
				else if(data.japanese[0].reading != null && data.japanese[0].word == null){
					ret += Markdown.toBold( data.japanese[0].reading) + " (" + data.japanese[0].reading+" - "+ToRomajiConverter.toRomaji(data.japanese[0].reading)+")";
				}
				else if(data.japanese[0].reading != null && data.japanese[0].word != null) {
					ret += Markdown.toBold( data.japanese[0].word) + " (" + data.japanese[0].reading+" - "+ToRomajiConverter.toRomaji(data.japanese[0].reading)+")";
				}
				else{
					continue;
				}
				ret += "\n";
			}
			else {
				// WORD AND READING
				ret += Markdown.toBold(data.slug)+" ("+data.japanese[0].reading+" - "+ToRomajiConverter.toRomaji(data.japanese[0].reading)+")\n";
			}
			
			
			
			// ENGLISH DEFINITIONS
			for(int j = 0; j < data.senses.length && j < maxSensesResults; j++) {
				Senses sense = data.senses[j];
				String definitions = "";
				for(String definition: sense.english_definitions) {
					definitions+= ", "+definition;
				}
				definitions = definitions.substring(2);
				ret += Markdown.toItalics(definitions)+"\n";
			}
			ret+="\n";
		}
		ret += "Zeige ggf. nur einen Teil der Suchergebnisse - Mehr Suchergebnisse unter: "+Jisho.jishoURLStandard+HTTPRequests.urlEncode(searchKey);
		return ret;
	}
	
	// Structure of jisho keyword lookup response, starting in JishoResponse class
	public class JishoResponse{
		public int status;
		public Results[] data;
	}
	
	public class Results{
		public String slug;
		public boolean is_common;
		public String[] tags;
		public String[] jlpt;
		public Japanese[] japanese;
		public Senses[] senses;
	}
	
	public class Japanese{
		public String word;
		public String reading;
	}
	
	public class Senses{
		public String[] english_definitions;
		public String[] parts_of_speech;
		public String[] tags;
	}

}
