package start;

import config.SecretsConfig;
import logging.QuickLogger;
import util.StringUtils;

public class Secrets {
    private final SecretsConfig config;

    Secrets(final SecretsConfig config){
        this.config = config;
        
        if(StringUtils.isNullOrWhiteSpace(config.spotifyClientId) || StringUtils.isNullOrWhiteSpace(config.spotifyClientSecret)){
            QuickLogger.logWarn("ClientId and/or secret for Spotify missing. Services using Spotify will not work.");
        }
        if(StringUtils.isNullOrWhiteSpace(config.weatherApiKey)){
            QuickLogger.logWarn("WeatherApiKey missing. Services using weather will not work.");
        }
        if(StringUtils.isNullOrWhiteSpace(config.translatorKey) || StringUtils.isNullOrWhiteSpace(config.translatorRegion)){
            QuickLogger.logWarn("translatorKey and/or translatorRegion missing. Services using translator will not work.");
        }
        
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

    String getTranslatorKey(){
        return this.config.translatorKey;
    }

    String getTranslatorRegion(){
        return this.config.translatorRegion;
    }
}
