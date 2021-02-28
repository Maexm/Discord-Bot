package start;

import config.SecretsConfig;

public class Secrets {
    private final SecretsConfig config;

    Secrets(final SecretsConfig config){
        this.config = config;
    }

    String getBotKey() {
        return this.config.botKey;
    }

    String getLastBotKey(){
        final String ret = this.config.botKey;
        this.config.botKey = null;
        return ret;
    }

    String getSpotifyClientId() {
        return this.config.spotifyClientId;
    }

    String getSpotifyClientSecret() {
        return this.config.spotifyClientSecret;
    }

    String getWeatherApiKey(){
        return this.config.weatherApiKey;
    }
}
