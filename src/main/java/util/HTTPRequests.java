package util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HTTPRequests {

	public static String getSimple(final String URL) {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(URL)).GET().timeout(Duration.ofSeconds(10l)).build();
		HttpResponse<String> resp = HTTPRequests.executeHttp(client, req);

		return resp != null ? resp.body() : null;	
	}

	

	/**
	 * Execute http request with given client and request. Log request
	 */
	public static HttpResponse<String> executeHttp(HttpClient client, HttpRequest request){
		System.out.println("[HTTPRequests] Sending HTTP request to: " + request.uri());
		HttpResponse<String> resp = null;
		try {
			resp = client.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return resp;
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
