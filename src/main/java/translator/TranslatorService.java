package translator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.Optional;

import com.google.gson.Gson;

import translator.LanguageResponse.Language;
import util.HTTPRequests;

public class TranslatorService {
    
    public final static String BASE_URL = "https://api.cognitive.microsofttranslator.com/";
    public final static String TRANSLATE_ROUTE_PARAMS = "translate?api-version=3.0&";
    public final static String LANGUAGES_URL = "https://api.cognitive.microsofttranslator.com/languages?api-version=3.0";

    private String subscriptionKey;
    private String subscriptionRegion;

    public TranslatorService(final String subscriptionKey, final String subscriptionRegion){
        this.setSubscriptionKey(subscriptionKey);
        this.setSubscriptionRegion(subscriptionRegion);
    }

    public void setSubscriptionKey(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }

    public void setSubscriptionRegion(String subscriptionRegion) {
        this.subscriptionRegion = subscriptionRegion;
    }

    public Optional<TranslatorResponse[]> translate(final String[] contents, final String targetLang){
        final String reqUrl = TranslatorService.BASE_URL+TranslatorService.TRANSLATE_ROUTE_PARAMS+"to="+HTTPRequests.urlEncode(targetLang);
        Gson gson = new Gson();
        final TranslatorRequest[] reqObjects = new TranslatorRequest[contents.length];
        for (int i = 0; i < contents.length; i++) {
            reqObjects[i] = new TranslatorRequest(contents[i]);
        }
        final String body = gson.toJson(reqObjects);


        HttpRequest req = HttpRequest
            .newBuilder(URI.create(reqUrl))
            .POST(BodyPublishers.ofString(body))
            .header("Ocp-Apim-Subscription-Key", this.subscriptionKey)
            .header("Ocp-Apim-Subscription-Region", this.subscriptionRegion)
            .header("Content-type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .build();

        HttpResponse<String> res = HTTPRequests.executeHttp(HttpClient.newHttpClient(), req);
        if(res != null && res.statusCode() == 200){
            return Optional.ofNullable(gson.fromJson(res.body(), TranslatorResponse[].class));
        }
        return Optional.empty();
    }

    public Optional<Language> getLanguage(String code, String acceptLanguage){
        Gson gson = new Gson();

        HttpRequest req  = HttpRequest
            .newBuilder(URI.create(TranslatorService.LANGUAGES_URL))
            .GET()
            .header("Ocp-Apim-Subscription-Key", this.subscriptionKey)
            .header("Ocp-Apim-Subscription-Region", this.subscriptionRegion)
            .header("Accept-Language", acceptLanguage)
            .timeout(Duration.ofSeconds(10))
            .build();
        HttpResponse<String> res = HTTPRequests.executeHttp(HttpClient.newHttpClient(), req);
        
        if(res != null && res.statusCode() == 200){
            return Optional.ofNullable(gson.fromJson(res.body(), LanguageResponse.class).translation.get(code));
        }
        return Optional.empty();
    }
}
