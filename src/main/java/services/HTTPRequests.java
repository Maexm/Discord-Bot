package services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class HTTPRequests {

	/**
	 * Sends a simple HTTPRequest to a given url and returns its response as string.
	 * Does NOT support additional parameters or authentication.
	 * 
	 * @param url
	 * @return A HtppResponse, including the response in string form. Null if an
	 *         error occurred.
	 */
	public static String get(final String URL) {

		try {
			URL url = new URL(URL);
			URLConnection urlCon = url.openConnection();
			Object content = urlCon.getContent();
			String ret = "";
			try (Scanner sc = new Scanner((InputStream) content, StandardCharsets.UTF_8.name())) {
				while (sc.hasNextLine()) {
					ret += sc.nextLine();
				}
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String get2(final String URL) {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(URL)).build();

		String body = null;
		try {
			body = client.send(req, BodyHandlers.ofString()).body();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			return body;
		}
	}

	public static String getTest(final String URL){
		// Object resp = HttpClient.create()
		// .baseUrl(URL)
		// .get()
		// .responseSingle((res, content) -> Mono.just(content))
		// .block()
		// .block();
		return null;
	}
}
