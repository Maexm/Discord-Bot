package start;

public class Secrets {
    private String botKey;
    private String spotifyClientId;
    private String spotifyClientSecret;
    private String weatherApiKey;

    public Secrets(){
    }

    Secrets(final String botKey, final String spotifyClientId, final String spotifyClientSecret, final String weatherApiKey){
        this.botKey = botKey;
        this.spotifyClientId = spotifyClientId;
        this.spotifyClientSecret = spotifyClientSecret;
        this.weatherApiKey = weatherApiKey;
    }

    String getBotKey() {
        final String ret = this.botKey;
        this.botKey = null;
        return ret;
    }

    String getSpotifyClientId() {
        return this.spotifyClientId;
    }

    String getSpotifyClientSecret() {
        return this.spotifyClientSecret;
    }

    String getWeatherApiKey(){
        return this.weatherApiKey;
    }
}
