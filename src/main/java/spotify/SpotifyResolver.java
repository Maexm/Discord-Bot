package spotify;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.Base64;

import com.google.gson.Gson;

import exceptions.IllegalMagicException;
import spotify.SpotifyObjects.ISpotifyObject;
import spotify.SpotifyObjects.TokenResponse;
import util.HTTPRequests;

public class SpotifyResolver {
    
    private String clientId;
    private String clientSecret;
    private String authorizationString;
    private String bearerToken = null;
    private final String tokenUrl = "https://accounts.spotify.com/api/token";
    private final String tracksUrl = "https://api.spotify.com/v1/tracks/";
    private final String albumsUrl = "https://api.spotify.com/v1/albums/";
    private final String artistUrl = "https://api.spotify.com/v1/artists/";

    public SpotifyResolver(final String clientId, final String clientSecret){
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.setAuthDetails(clientId, clientSecret);
    }

    private void refreshBearerToken(){
        final String body = "grant_type=client_credentials";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(this.tokenUrl))
        .POST(BodyPublishers.ofString(body))
        .header("Authorization", "Basic "+this.authorizationString)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .timeout(Duration.ofSeconds(10l)).build();

        HttpResponse<String> response = HTTPRequests.executeHttp(client, req);

        // Evaluate response
        if(response == null || response.statusCode() != 200){
           this.bearerToken = null;
        }
        else{
            Gson gson = new Gson();
            TokenResponse tokenResponse = gson.fromJson(response.body(), TokenResponse.class);
            
            this.bearerToken = tokenResponse.access_token;
        }
    }

    public <T extends ISpotifyObject> T getSpotifyObject(String objectId, Class<T> type, boolean tryCachedToken){
        
        if(!tryCachedToken || this.bearerToken == null){
            this.refreshBearerToken();
        }

        objectId = HTTPRequests.urlEncode(objectId);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(this.getSpotifyObjectUrl(type)+objectId))
        .header("Authorization", "Bearer "+this.bearerToken)
        .GET()
        .timeout(Duration.ofSeconds(10l))
        .build();

        HttpResponse<String> response = HTTPRequests.executeHttp(client, req);

        if(response.statusCode() == 401){
            // Refresh token and retry
            if(tryCachedToken){
                return this.getSpotifyObject(objectId, type, false);
            }
            // Did not try cached token => credentials are broken
            System.out.println("WARNING: Spotify credentials are broken!");
            return null;
        }
        else if(response.statusCode() == 404){
            return null;
        }
        else{
            Gson gson = new Gson();
            return gson.fromJson(response.body(), type);
        }
    }

    private final String getSpotifyObjectUrl(Class<? extends ISpotifyObject> type){
        switch(type.getSimpleName()){
            case "SpotifyTrackResponse":
                return this.tracksUrl;
            case "SpotifyAlbumResponse":
                return this.albumsUrl;
            case "SpotifyArtistResponse":
                return this.artistUrl;
            default:
                throw new IllegalMagicException("Illegal Spotify Object");
        }
    }

    public final void setAuthDetails(String clientId, String clientSecret){
        this.authorizationString = Base64.getEncoder().encodeToString((this.clientId+":"+this.clientSecret).getBytes());
    }
}
