package services;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class HTTPRequests {

	/**
	 * Sends a simple HTTPRequest to a given url and returns its response as string.
	 * Does NOT support additional parameters or authentication.
	 * @param url
	 * @return A HtppResponse, including the response in string form. Null if an error occurred.
	 */
	public static String get(final String URL){
		
		try {
			URL url = new URL(URL);
			URLConnection urlCon = url.openConnection();
			Object content = urlCon.getContent();
			String ret = "";
			try(Scanner sc = new Scanner((InputStream) content, StandardCharsets.UTF_8.name())){
				while(sc.hasNextLine()) {
					ret += sc.nextLine();
				}
			}
			return ret;
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		
	}
}
