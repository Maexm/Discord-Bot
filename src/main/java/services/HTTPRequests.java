package services;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

public class HTTPRequests {

	@Deprecated
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

	public static String getModern(final String URL) {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(URL)).GET().timeout(Duration.ofSeconds(10l)).build();

		System.out.println("[HTTPRequests] Sending HTTP request to: " + URL);
		String body = null;
		try {
			body = client.send(req, BodyHandlers.ofString()).body();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return body;
	}

	public static String neutralize(String s) {
		return s.replace(":", "").replace("/", "").replace("?", "").replace("#", "").replace("[", "").replace("]", "")
				.replace("@", "").replace("!", "").replace("$", "").replace("&", "").replace("'", "").replace("(", "")
				.replace(")", "").replace("*", "").replace("+", "").replace(",", "").replace(";", "").replace("=", "");
	}

	public static String urlEncode(String s, boolean perc20Space) {
		try {
			if (perc20Space) {
				return URLEncoder.encode(s, StandardCharsets.UTF_8.name()).replace("+", "%20");
			}
			return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String urlEncode(String s) {
		return HTTPRequests.urlEncode(s, true);
	}
}
